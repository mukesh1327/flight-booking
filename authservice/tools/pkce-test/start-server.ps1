param(
    [int]$Port = 3000
)

Set-Location -Path $PSScriptRoot

$python = Get-Command python -ErrorAction SilentlyContinue
if (-not $python) {
    $python = Get-Command py -ErrorAction SilentlyContinue
}

if (-not $python) {
    Write-Error "Python not found. Install Python or use another static server."
    exit 1
}

Write-Host "Serving PKCE callback page at http://localhost:$Port/callback.html"
if ($python.Name -eq "py.exe") {
    & py -m http.server $Port
} else {
    & python -m http.server $Port
}
