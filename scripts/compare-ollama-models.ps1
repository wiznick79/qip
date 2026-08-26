param(
    [string[]]$Models = @("qwen3.5:9b", "gemma4:12b", "qwen3-coder:30b"),
    [ValidateRange(1, 5)]
    [int]$Runs = 1,
    [ValidateRange(2048, 32768)]
    [int]$ContextLength = 8192,
    [string]$EmbeddingModel = "nomic-embed-text:latest"
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$mavenWrapper = Join-Path $repositoryRoot "mvnw.cmd"
$outputDirectory = Join-Path $repositoryRoot "target/model-comparison"
$rawDirectory = Join-Path $outputDirectory "raw"
$runResult = Join-Path $outputDirectory "run-result.json"

function Restore-EnvironmentVariable {
    param([string]$Name, [AllowNull()][string]$Value)
    if ($null -eq $Value) {
        Remove-Item -Path "Env:$Name" -ErrorAction SilentlyContinue
    } else {
        Set-Item -Path "Env:$Name" -Value $Value
    }
}

function Escape-Markdown {
    param([AllowNull()][string]$Value)
    if ($null -eq $Value) {
        return ""
    }
    return ($Value -replace "\|", "\|" -replace "`r?`n", " ").Trim()
}

function Get-Median {
    param([double[]]$Values)
    $ordered = @($Values | Sort-Object)
    if ($ordered.Count -eq 0) {
        return 0
    }
    $middle = [math]::Floor($ordered.Count / 2)
    if ($ordered.Count % 2 -eq 1) {
        return $ordered[$middle]
    }
    return ($ordered[$middle - 1] + $ordered[$middle]) / 2
}

if (-not (Get-Command ollama -ErrorAction SilentlyContinue)) {
    throw "Ollama is not installed or is not available on PATH."
}
if ($Models.Count -lt 2) {
    throw "Supply at least two chat models for a comparison."
}
if (($Models | Select-Object -Unique).Count -ne $Models.Count) {
    throw "Each chat model may appear only once."
}

$installedModels = @(& ollama list | Select-Object -Skip 1 | ForEach-Object { ($_ -split "\s+")[0] })
if ($LASTEXITCODE -ne 0) {
    throw "Ollama is not running. Start it before running the comparison."
}
$missingModels = @($Models | Where-Object { $_ -notin $installedModels })
if ($EmbeddingModel -notin $installedModels) {
    $missingModels += $EmbeddingModel
}
if ($missingModels.Count -gt 0) {
    throw "These models are not installed: $($missingModels -join ', '). This script never downloads models automatically."
}

$previousEnvironment = @{
    QIP_OLLAMA_MODEL_COMPARISON = $env:QIP_OLLAMA_MODEL_COMPARISON
    QIP_OLLAMA_CHAT_MODEL = $env:QIP_OLLAMA_CHAT_MODEL
    QIP_OLLAMA_CHAT_CONTEXT_LENGTH = $env:QIP_OLLAMA_CHAT_CONTEXT_LENGTH
    QIP_OLLAMA_EMBEDDING_MODEL = $env:QIP_OLLAMA_EMBEDDING_MODEL
    QIP_OLLAMA_CHAT_THINK = $env:QIP_OLLAMA_CHAT_THINK
}

New-Item -ItemType Directory -Path $rawDirectory -Force | Out-Null
$assignments = @()
$shuffledModels = @($Models | Sort-Object { Get-Random })
for ($index = 0; $index -lt $shuffledModels.Count; $index++) {
    $assignments += [pscustomobject]@{
        Label = "Model $([char](65 + $index))"
        Model = $shuffledModels[$index]
    }
}

$records = @()
$processorSnapshots = @()
Push-Location $repositoryRoot
try {
    $env:QIP_OLLAMA_MODEL_COMPARISON = "true"
    $env:QIP_OLLAMA_CHAT_CONTEXT_LENGTH = $ContextLength.ToString()
    $env:QIP_OLLAMA_EMBEDDING_MODEL = $EmbeddingModel
    $env:QIP_OLLAMA_CHAT_THINK = "false"

    for ($run = 1; $run -le $Runs; $run++) {
        $runOrder = @($assignments | Sort-Object { Get-Random })
        for ($candidate = 0; $candidate -lt $runOrder.Count; $candidate++) {
            $assignment = $runOrder[$candidate]
            Write-Host "Evaluating candidate $($candidate + 1)/$($runOrder.Count), run $run/$Runs..."
            $env:QIP_OLLAMA_CHAT_MODEL = $assignment.Model
            & ollama stop $assignment.Model | Out-Null
            & $mavenWrapper "-Dtest=OllamaModelComparisonLiveTests" test
            if ($LASTEXITCODE -ne 0) {
                throw "Model comparison test failed for candidate $($assignment.Label), run $run."
            }
            if (-not (Test-Path -LiteralPath $runResult)) {
                throw "The model comparison test did not create $runResult."
            }

            $safeModelName = $assignment.Model -replace "[^A-Za-z0-9._-]", "_"
            $rawResult = Join-Path $rawDirectory "$safeModelName-run-$run.json"
            Copy-Item -LiteralPath $runResult -Destination $rawResult -Force
            $report = Get-Content -LiteralPath $runResult -Raw | ConvertFrom-Json
            foreach ($case in $report.cases) {
                $records += [pscustomobject]@{
                    Label = $assignment.Label
                    Model = $assignment.Model
                    Run = $run
                    CaseId = $case.id
                    Question = $case.question
                    ExpectedStatus = $case.expectedStatus
                    ReviewCriteria = @($case.reviewCriteria)
                    ActualStatus = $case.actualStatus
                    Answer = $case.answer
                    RetrievedPage = $case.retrievedPage
                    RetrievalHit = $case.retrievalHit
                    CitationsValid = $case.citationsValid
                    HardGatePass = $case.hardGatePass
                    DurationMillis = $case.durationMillis
                    AnswerCharacters = $case.answerCharacters
                    Failure = $case.failure
                }
            }
            $processorSnapshots += "[$($assignment.Label), run $run]`r`n$(& ollama ps | Out-String)"
            & ollama stop $assignment.Model | Out-Null
        }
    }
} finally {
    foreach ($entry in $previousEnvironment.GetEnumerator()) {
        Restore-EnvironmentVariable -Name $entry.Key -Value $entry.Value
    }
    Pop-Location
}

$scoreRows = $records | ForEach-Object {
    [pscustomobject]@{
        model_label = $_.Label
        run = $_.Run
        case_id = $_.CaseId
        correctness_0_to_2 = ""
        grounding_0_to_2 = ""
        uncertainty_0_to_2 = ""
        completeness_0_to_2 = ""
        clarity_0_to_2 = ""
        notes = ""
    }
}
$scoreRows | Export-Csv -LiteralPath (Join-Path $outputDirectory "scores.csv") -NoTypeInformation -Encoding UTF8

$blind = [System.Collections.Generic.List[string]]::new()
$blind.Add("# QIP blinded Ollama model comparison")
$blind.Add("")
$blind.Add("Fixture: ``v2``  ")
$blind.Add("Context: ``$ContextLength`` tokens  ")
$blind.Add("Thinking: disabled  ")
$blind.Add("Runs per model: ``$Runs``  ")
$blind.Add("Embedding: ``$EmbeddingModel``")
$blind.Add("")
$blind.Add("Score every response from 0 to 2 for correctness, grounding, uncertainty, completeness, and clarity. Record scores in ``scores.csv`` before opening ``reveal.md``.")
$blind.Add("")

$caseIds = @($records | Select-Object -ExpandProperty CaseId -Unique)
foreach ($caseId in $caseIds) {
    $caseRecords = @($records | Where-Object { $_.CaseId -eq $caseId } | Sort-Object Run, Label)
    $first = $caseRecords[0]
    $blind.Add("## $caseId")
    $blind.Add("")
    $blind.Add("**Question:** $(Escape-Markdown $first.Question)")
    $blind.Add("")
    $blind.Add("**Expected status:** ``$($first.ExpectedStatus)``")
    $blind.Add("")
    $blind.Add("Review criteria:")
    $blind.Add("")
    foreach ($criterion in $first.ReviewCriteria) {
        $blind.Add("- $(Escape-Markdown $criterion)")
    }
    $blind.Add("")
    foreach ($record in $caseRecords) {
        $blind.Add("### $($record.Label) - run $($record.Run)")
        $blind.Add("")
        $blind.Add("Automated gate: **$(if ($record.HardGatePass) { 'PASS' } else { 'FAIL' })**; status ``$($record.ActualStatus)``; retrieval $(if ($record.RetrievalHit) { 'PASS' } else { 'FAIL' }); citations $(if ($record.CitationsValid) { 'PASS' } else { 'FAIL' }); latency $($record.DurationMillis) ms.")
        $blind.Add("")
        if ([string]::IsNullOrWhiteSpace($record.Answer)) {
            $blind.Add("> No answer returned. $(Escape-Markdown $record.Failure)")
        } else {
            $blind.Add("> $(Escape-Markdown $record.Answer)")
        }
        $blind.Add("")
    }
}
$blind | Set-Content -LiteralPath (Join-Path $outputDirectory "scorecard.md") -Encoding UTF8

$reveal = [System.Collections.Generic.List[string]]::new()
$reveal.Add("# QIP Ollama model comparison reveal")
$reveal.Add("")
$reveal.Add("Open this only after completing ``scores.csv``.")
$reveal.Add("")
$reveal.Add("| Blind label | Chat model | Hard gates | Median latency |")
$reveal.Add("| --- | --- | ---: | ---: |")
foreach ($assignment in $assignments | Sort-Object Label) {
    $modelRecords = @($records | Where-Object { $_.Label -eq $assignment.Label })
    $passes = @($modelRecords | Where-Object HardGatePass).Count
    $median = [math]::Round((Get-Median @($modelRecords | Select-Object -ExpandProperty DurationMillis)), 0)
    $reveal.Add("| $($assignment.Label) | ``$($assignment.Model)`` | $passes/$($modelRecords.Count) | $median ms |")
}
$reveal.Add("")
$reveal.Add("## Ollama processor snapshots")
$reveal.Add("")
$reveal.Add("``````text")
$reveal.Add(($processorSnapshots -join "`r`n"))
$reveal.Add("``````")
$reveal | Set-Content -LiteralPath (Join-Path $outputDirectory "reveal.md") -Encoding UTF8

$records | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $rawDirectory "all-results.json") -Encoding UTF8
Write-Host "Comparison complete. Score blinded answers first: $outputDirectory\scorecard.md"
Write-Host "Enter human scores in: $outputDirectory\scores.csv"
Write-Host "Reveal model identities afterward: $outputDirectory\reveal.md"
