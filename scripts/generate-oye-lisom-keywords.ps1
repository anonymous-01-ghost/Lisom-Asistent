param(
    [Parameter(Mandatory = $true)]
    [string]$ModelDirectory
)

$ErrorActionPreference = "Stop"

$tokens = Join-Path $ModelDirectory "tokens.txt"
$bpeModel = Join-Path $ModelDirectory "bpe.model"
$rawKeywords = Join-Path $PSScriptRoot "..\docs\assets\oye-lisom-keywords.raw.txt"
$outputKeywords = Join-Path $ModelDirectory "keywords.txt"

foreach ($path in @($tokens, $bpeModel, $rawKeywords)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required keyword-generation file was not found: $path"
    }
}

$resolvedRaw = (Resolve-Path -LiteralPath $rawKeywords).Path
& sherpa-onnx-cli text2token --tokens $tokens --tokens-type bpe --bpe-model $bpeModel $resolvedRaw $outputKeywords

if ($LASTEXITCODE -ne 0) {
    throw "sherpa-onnx-cli text2token failed with exit code $LASTEXITCODE."
}

if (-not (Test-Path -LiteralPath $outputKeywords -PathType Leaf)) {
    throw "Keyword conversion did not create: $outputKeywords"
}

Write-Host "Generated wake-word tokens: $outputKeywords"
