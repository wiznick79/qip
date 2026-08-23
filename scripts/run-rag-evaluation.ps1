param(
    [switch]$Ollama
)

$ErrorActionPreference = "Stop"
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$mavenWrapper = Join-Path $repositoryRoot "mvnw.cmd"

Push-Location $repositoryRoot
try {
    if ($Ollama) {
        if (-not (Get-Command ollama -ErrorAction SilentlyContinue)) {
            throw "Ollama is not installed or is not available on PATH."
        }
        ollama list | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "Ollama is not running. Start it before running the live evaluation."
        }
        $previousEvaluationFlag = $env:QIP_OLLAMA_EVALUATION
        $env:QIP_OLLAMA_EVALUATION = "true"
        try {
            & $mavenWrapper "-Dtest=RagEvaluationOllamaLiveTests" test
        } finally {
            $env:QIP_OLLAMA_EVALUATION = $previousEvaluationFlag
        }
        $report = Join-Path $repositoryRoot "target/rag-evaluation/ollama-report.md"
    } else {
        & $mavenWrapper "-Dtest=RagEvaluationTests" test
        $report = Join-Path $repositoryRoot "target/rag-evaluation/report.md"
    }
    if ($LASTEXITCODE -ne 0) {
        throw "RAG evaluation failed with exit code $LASTEXITCODE."
    }
    Write-Host "RAG evaluation passed. Report: $report"
} finally {
    Pop-Location
}
