param(
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$ReindexDocuments,
    [string]$Username,
    [string]$Password
)

$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($Username)) {
    $Username = if ($env:QIP_ADMIN_USERNAME) { $env:QIP_ADMIN_USERNAME } else { "qip-admin" }
}
if ([string]::IsNullOrWhiteSpace($Password)) {
    $Password = if ($env:QIP_ADMIN_PASSWORD) { $env:QIP_ADMIN_PASSWORD } else { "qip-admin-local-only" }
}

$loader = Join-Path $PSScriptRoot "load-synthetic-demo.ps1"
. (Join-Path $PSScriptRoot "qip-authenticated-client.ps1")
& $loader -BaseUrl $BaseUrl -ReindexDocuments:$ReindexDocuments -Username $Username -Password $Password

$context = New-QipAuthenticatedClient -BaseUrl $BaseUrl -Username $Username -Password $Password
try {
    $assets = (Invoke-QipJson -Context $context -Method "GET" -Path "/api/assets?page=0&size=100").items
    $documents = (Invoke-QipJson -Context $context -Method "GET" -Path "/api/documents?page=0&size=100").items
    $asset = $assets | Where-Object { $_.externalReference -eq "SYN-HP-040" } | Select-Object -First 1
    $document = $documents | Where-Object { $_.title -eq "Atlas HP-40 Synthetic Service Manual" } | Select-Object -First 1

    if ($null -eq $asset) {
        throw "Synthetic Atlas HP-40 asset was not found after loading demo data."
    }
    if ($null -eq $document -or $document.status -ne "INDEXED") {
        throw "Synthetic Atlas HP-40 manual is not indexed. Restart QIP with the intended model profile and retry."
    }

    $eventTime = [DateTimeOffset]::UtcNow.AddSeconds(-1).ToString("o")
    $incident = Invoke-QipJson -Context $context -Method "POST" -Path "/api/incidents" -Body @{
        assetId = $asset.id
        title = "Synthetic HP-40 heat and slow retract"
        description = "Oil temperature reached 66 C, ram retract time rose to 5.4 seconds, and return-filter differential was 3.1 bar."
        severity = "HIGH"
        occurredAt = $eventTime
    }
    $observation = Invoke-QipJson -Context $context -Method "POST" -Path "/api/incidents/$($incident.id)/observations" -Body @{
        text = "Operator observed elevated oil temperature and slower ram retraction before any machine setting was changed."
        observedAt = $eventTime
    }
    $evidence = Invoke-QipJson -Context $context -Method "POST" -Path "/api/incidents/$($incident.id)/evidence" -Body @{
        type = "MEASUREMENT"
        summary = "Return-filter differential pressure measured 3.1 bar while oil temperature was 66 C."
        sourceReference = "Synthetic return-line gauge PT-14"
        eventAt = $eventTime
    }
    $investigation = Invoke-QipJson -Context $context -Method "POST" -Path "/api/incidents/$($incident.id)/investigations"
    $incident = Invoke-QipJson -Context $context -Method "PATCH" -Path "/api/incidents/$($incident.id)/status" -Body @{ status = "INVESTIGATING" }

    $answer = Invoke-QipJson -Context $context -Method "POST" -Path "/api/investigations/$($investigation.id)/questions" -Body @{
        question = "What evidence supports the first inspection, and what remains uncertain?"
        documentIds = @($document.id)
    }

    $finding = $null
    $reviewedFinding = $null
    $closedInvestigation = $null
    if ($answer.status -eq "GROUNDED") {
        $finding = Invoke-QipJson -Context $context -Method "POST" -Path "/api/investigations/$($investigation.id)/findings" -Body @{
            sourceQuestionId = $answer.id
            summary = $answer.answer
        }
        $reviewedFinding = Invoke-QipJson -Context $context -Method "POST" -Path "/api/investigations/$($investigation.id)/findings/$($finding.id)/reviews" -Body @{
            decision = "CONFIRMED"
            rationale = "The synthetic source citation and recorded incident conditions support retaining this inspection finding."
        }
        $closedInvestigation = Invoke-QipJson -Context $context -Method "POST" -Path "/api/investigations/$($investigation.id)/closure" -Body @{
            summary = "The synthetic return-filter inspection finding is confirmed. Other contributing factors remain uncertain and require normal human follow-up."
        }
        $incident = Invoke-QipJson -Context $context -Method "PATCH" -Path "/api/incidents/$($incident.id)/status" -Body @{ status = "RESOLVED" }
    }

    Write-Host "Synthetic investigation completed."
    Write-Host "Incident: $($incident.id)"
    Write-Host "Investigation: $($investigation.id)"
    Write-Host "Incident status: $($incident.status)"
    Write-Host "Observation: $($observation.authorReference) [$($observation.id)]"
    Write-Host "Evidence: $($evidence.type), $($evidence.provenance) [$($evidence.id)]"
    Write-Host "Status: $($answer.status)"
    Write-Host "Answer: $($answer.answer)"
    foreach ($citation in $answer.citations) {
        Write-Host "Source: $($citation.documentTitle), page $($citation.pageNumber), relevance $($citation.relevanceScore)"
    }
    if ($null -ne $reviewedFinding) {
        Write-Host "Finding: $($reviewedFinding.status) by $($reviewedFinding.reviewedBy) [$($reviewedFinding.id)]"
        Write-Host "Review events: $($reviewedFinding.events.Count)"
    }
    if ($null -ne $closedInvestigation) {
        Write-Host "Investigation status: $($closedInvestigation.status) by $($closedInvestigation.closedBy)"
        Write-Host "Closure summary: $($closedInvestigation.closureSummary)"
    }
}
finally {
    $context.Client.Dispose()
}
