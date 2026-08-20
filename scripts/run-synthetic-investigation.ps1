param(
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$ReindexDocuments
)

$ErrorActionPreference = "Stop"
$loader = Join-Path $PSScriptRoot "load-synthetic-demo.ps1"

& $loader -BaseUrl $BaseUrl -ReindexDocuments:$ReindexDocuments

$assets = (Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/assets?page=0&size=100").items
$documents = (Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/documents?page=0&size=100").items
$asset = $assets | Where-Object { $_.externalReference -eq "SYN-HP-040" } | Select-Object -First 1
$document = $documents | Where-Object { $_.title -eq "Atlas HP-40 Synthetic Service Manual" } | Select-Object -First 1

if ($null -eq $asset) {
    throw "Synthetic Atlas HP-40 asset was not found after loading demo data."
}
if ($null -eq $document -or $document.status -ne "INDEXED") {
    throw "Synthetic Atlas HP-40 manual is not indexed. Restart QIP with the intended model profile and retry."
}

$incidentBody = @{
    assetId = $asset.id
    title = "Synthetic HP-40 heat and slow retract"
    description = "Oil temperature reached 66 C, ram retract time rose to 5.4 seconds, and return-filter differential was 3.1 bar."
    severity = "HIGH"
    occurredAt = [DateTimeOffset]::UtcNow.ToString("o")
} | ConvertTo-Json
$incident = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/incidents" -ContentType "application/json" -Body $incidentBody
$investigation = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/incidents/$($incident.id)/investigations"

$questionBody = @{
    question = "What evidence supports the first inspection, and what remains uncertain?"
    documentIds = @($document.id)
} | ConvertTo-Json
$answer = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/investigations/$($investigation.id)/questions" -ContentType "application/json" -Body $questionBody

$finding = $null
$reviewedFinding = $null
if ($answer.status -eq "GROUNDED") {
    $findingBody = @{
        sourceQuestionId = $answer.id
        summary = $answer.answer
        proposedBy = "synthetic-demo-investigator"
    } | ConvertTo-Json
    $finding = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/investigations/$($investigation.id)/findings" -ContentType "application/json" -Body $findingBody

    $reviewBody = @{
        decision = "CONFIRMED"
        reviewerReference = "synthetic-demo-reviewer"
        rationale = "The synthetic source citation and recorded incident conditions support retaining this inspection finding."
    } | ConvertTo-Json
    $reviewedFinding = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/investigations/$($investigation.id)/findings/$($finding.id)/reviews" -ContentType "application/json" -Body $reviewBody
}

Write-Host "Synthetic investigation completed."
Write-Host "Incident: $($incident.id)"
Write-Host "Investigation: $($investigation.id)"
Write-Host "Status: $($answer.status)"
Write-Host "Answer: $($answer.answer)"
foreach ($citation in $answer.citations) {
    Write-Host "Source: $($citation.documentTitle), page $($citation.pageNumber), relevance $($citation.relevanceScore)"
}
if ($null -ne $reviewedFinding) {
    Write-Host "Finding: $($reviewedFinding.status) by $($reviewedFinding.reviewedBy) [$($reviewedFinding.id)]"
    Write-Host "Review events: $($reviewedFinding.events.Count)"
}
