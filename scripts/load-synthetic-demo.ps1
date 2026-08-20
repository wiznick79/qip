param(
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$ReindexDocuments
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$machinesPath = Join-Path $repositoryRoot "samples/machines.json"
$documentsPath = Join-Path $repositoryRoot "output/pdf"
$machines = Get-Content -Raw -LiteralPath $machinesPath | ConvertFrom-Json

Add-Type -AssemblyName System.Net.Http

function Send-Document {
    param(
        [System.Net.Http.HttpClient]$Client,
        [string]$Uri,
        [string]$Title,
        [string]$DocumentPath
    )

    $form = [System.Net.Http.MultipartFormDataContent]::new()
    $response = $null
    try {
        $titleContent = [System.Net.Http.StringContent]::new($Title, [System.Text.Encoding]::UTF8)
        $fileContent = [System.Net.Http.ByteArrayContent]::new([System.IO.File]::ReadAllBytes($DocumentPath))
        $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::new("application/pdf")
        $form.Add($titleContent, "title")
        $form.Add($fileContent, "file", [System.IO.Path]::GetFileName($DocumentPath))

        $response = $Client.PostAsync($Uri, $form).GetAwaiter().GetResult()
        $responseBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "Document upload failed with HTTP $([int]$response.StatusCode): $responseBody"
        }
        return $responseBody | ConvertFrom-Json
    }
    finally {
        if ($null -ne $response) {
            $response.Dispose()
        }
        $form.Dispose()
    }
}

Write-Host "Loading synthetic QIP demo data into $BaseUrl"

$existingAssets = (Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/assets?page=0&size=100").items
foreach ($machine in $machines) {
    $existing = $existingAssets | Where-Object { $_.externalReference -eq $machine.externalReference } | Select-Object -First 1
    if ($null -ne $existing) {
        Write-Host "Asset already exists: $($machine.name) [$($existing.id)]"
        continue
    }

    $body = @{
        name = $machine.name
        type = $machine.type
        externalReference = $machine.externalReference
    } | ConvertTo-Json
    $created = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/assets" -ContentType "application/json" -Body $body
    Write-Host "Created asset: $($created.name) [$($created.id)]"
}

$httpClient = [System.Net.Http.HttpClient]::new()
try {
    foreach ($machine in $machines) {
        $documentPath = Join-Path $documentsPath $machine.documentFile
        if (-not (Test-Path -LiteralPath $documentPath)) {
            throw "Missing synthetic document: $documentPath"
        }

        $uploaded = Send-Document -Client $httpClient -Uri "$BaseUrl/api/documents" -Title $machine.documentTitle -DocumentPath $documentPath
        Write-Host "Uploaded document: $($uploaded.title) [$($uploaded.status)]"
        if ($ReindexDocuments) {
            $reindexed = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/documents/$($uploaded.id)/indexing"
            Write-Host "Re-indexed document: $($uploaded.title) [$($reindexed.status)]"
        }
    }
}
finally {
    $httpClient.Dispose()
}

Write-Host "Synthetic demo data is ready. Re-running this script skips existing assets and reuses documents by checksum."
