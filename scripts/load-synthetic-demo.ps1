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

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$machinesPath = Join-Path $repositoryRoot "samples/machines.json"
$documentsPath = Join-Path $repositoryRoot "output/pdf"
$machines = Get-Content -Raw -LiteralPath $machinesPath | ConvertFrom-Json
. (Join-Path $PSScriptRoot "qip-authenticated-client.ps1")

Write-Host "Loading synthetic QIP demo data into $BaseUrl as $Username"
$context = New-QipAuthenticatedClient -BaseUrl $BaseUrl -Username $Username -Password $Password
try {
    $existingAssets = (Invoke-QipJson -Context $context -Method "GET" -Path "/api/assets?page=0&size=100").items
    foreach ($machine in $machines) {
        $existing = $existingAssets | Where-Object { $_.externalReference -eq $machine.externalReference } | Select-Object -First 1
        if ($null -ne $existing) {
            Write-Host "Asset already exists: $($machine.name) [$($existing.id)]"
            continue
        }

        $created = Invoke-QipJson -Context $context -Method "POST" -Path "/api/assets" -Body @{
            name = $machine.name
            type = $machine.type
            externalReference = $machine.externalReference
        }
        Write-Host "Created asset: $($created.name) [$($created.id)]"
    }

    foreach ($machine in $machines) {
        $documentPath = Join-Path $documentsPath $machine.documentFile
        if (-not (Test-Path -LiteralPath $documentPath)) {
            throw "Missing synthetic document: $documentPath"
        }

        $uploaded = Send-QipDocument -Context $context -Title $machine.documentTitle -DocumentPath $documentPath
        Write-Host "Uploaded document: $($uploaded.title) [$($uploaded.status)]"
        if ($ReindexDocuments) {
            $reindexed = Invoke-QipJson -Context $context -Method "POST" -Path "/api/documents/$($uploaded.id)/indexing"
            Write-Host "Re-indexed document: $($uploaded.title) [$($reindexed.status)]"
        }
    }
}
finally {
    $context.Client.Dispose()
}

Write-Host "Synthetic demo data is ready. Re-running this script skips existing assets and reuses documents by checksum."
