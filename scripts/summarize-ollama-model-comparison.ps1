param(
    [string]$ComparisonDirectory = (Join-Path (Split-Path -Parent $PSScriptRoot) "target/model-comparison")
)

$ErrorActionPreference = "Stop"
$scoresPath = Join-Path $ComparisonDirectory "scores.csv"
$resultsPath = Join-Path $ComparisonDirectory "raw/all-results.json"
$reportPath = Join-Path $ComparisonDirectory "final-report.md"

function Get-Median {
    param([double[]]$Values)
    $ordered = @($Values | Sort-Object)
    $middle = [math]::Floor($ordered.Count / 2)
    if ($ordered.Count % 2 -eq 1) {
        return $ordered[$middle]
    }
    return ($ordered[$middle - 1] + $ordered[$middle]) / 2
}

if (-not (Test-Path -LiteralPath $scoresPath) -or -not (Test-Path -LiteralPath $resultsPath)) {
    throw "Run compare-ollama-models.ps1 before summarizing scores."
}

$scores = @(Import-Csv -LiteralPath $scoresPath)
$parsedResults = Get-Content -LiteralPath $resultsPath -Raw | ConvertFrom-Json
# Windows PowerShell 5.1 emits a JSON root array as one Object[] pipeline item.
$results = @($parsedResults | ForEach-Object { $_ })
$criteria = @(
    "correctness_0_to_2",
    "grounding_0_to_2",
    "uncertainty_0_to_2",
    "completeness_0_to_2",
    "clarity_0_to_2"
)

foreach ($score in $scores) {
    foreach ($criterion in $criteria) {
        $value = 0.0
        if (-not [double]::TryParse($score.$criterion, [ref]$value) -or $value -lt 0 -or $value -gt 2) {
            throw "Complete every score with a number from 0 to 2. Invalid $criterion for $($score.model_label), run $($score.run), case $($score.case_id)."
        }
    }
}

$summaries = @()
foreach ($label in @($scores | Select-Object -ExpandProperty model_label -Unique)) {
    $labelScores = @($scores | Where-Object { $_.model_label -eq $label })
    $labelResults = @($results | Where-Object { $_.Label -eq $label })
    if ($labelScores.Count -ne $labelResults.Count) {
        throw "Score and result counts differ for $label. Do not add or remove score rows."
    }
    $modelNames = @($labelResults | Select-Object -ExpandProperty Model -Unique)
    if ($modelNames.Count -ne 1) {
        throw "Could not resolve one model identity for $label."
    }
    $criterionAverages = @{}
    foreach ($criterion in $criteria) {
        $criterionAverages[$criterion] = ($labelScores | Measure-Object -Property $criterion -Average).Average
    }
    $qualityScores = @($labelScores | ForEach-Object {
        [double]$_.correctness_0_to_2 +
        [double]$_.grounding_0_to_2 +
        [double]$_.uncertainty_0_to_2 +
        [double]$_.completeness_0_to_2 +
        [double]$_.clarity_0_to_2
    })
    $summaries += [pscustomobject]@{
        Label = $label
        Model = $modelNames[0]
        Quality = ($qualityScores | Measure-Object -Average).Average
        Correctness = $criterionAverages["correctness_0_to_2"]
        Grounding = $criterionAverages["grounding_0_to_2"]
        Uncertainty = $criterionAverages["uncertainty_0_to_2"]
        Completeness = $criterionAverages["completeness_0_to_2"]
        Clarity = $criterionAverages["clarity_0_to_2"]
        HardGatePasses = @($labelResults | Where-Object HardGatePass).Count
        TotalCases = $labelResults.Count
        TechnicalFailures = @($labelResults | Where-Object { $_.ActualStatus -eq "TECHNICAL_FAILURE" }).Count
        MedianLatency = Get-Median @($labelResults | Select-Object -ExpandProperty DurationMillis)
    }
}

$ranked = @($summaries | Sort-Object @{ Expression = "Quality"; Descending = $true }, @{ Expression = "HardGatePasses"; Descending = $true }, @{ Expression = "MedianLatency"; Descending = $false })
$report = [System.Collections.Generic.List[string]]::new()
$report.Add("# QIP Ollama model comparison")
$report.Add("")
$report.Add("Human quality scores are averages on a 0-10 scale. Automated gates cover expected status, retrieval, citation validity, response presence, and technical failures.")
$report.Add("")
$report.Add("| Rank | Model | Quality | Hard gates | Technical failures | Median latency |")
$report.Add("| ---: | --- | ---: | ---: | ---: | ---: |")
for ($index = 0; $index -lt $ranked.Count; $index++) {
    $summary = $ranked[$index]
    $report.Add("| $($index + 1) | ``$($summary.Model)`` | $($summary.Quality.ToString('N2'))/10 | $($summary.HardGatePasses)/$($summary.TotalCases) | $($summary.TechnicalFailures) | $([math]::Round($summary.MedianLatency)) ms |")
}
$report.Add("")
$report.Add("## Quality dimensions")
$report.Add("")
$report.Add("| Model | Correctness | Grounding | Uncertainty | Completeness | Clarity |")
$report.Add("| --- | ---: | ---: | ---: | ---: | ---: |")
foreach ($summary in $ranked) {
    $report.Add("| ``$($summary.Model)`` | $($summary.Correctness.ToString('N2')) | $($summary.Grounding.ToString('N2')) | $($summary.Uncertainty.ToString('N2')) | $($summary.Completeness.ToString('N2')) | $($summary.Clarity.ToString('N2')) |")
}
$report | Set-Content -LiteralPath $reportPath -Encoding UTF8
Write-Host "Final comparison report: $reportPath"
