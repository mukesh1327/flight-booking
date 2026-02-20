#!/usr/bin/env bash
set -euo pipefail

PORT="${1:-3000}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PYTHON_BIN=""
if command -v python3 >/dev/null 2>&1; then
  PYTHON_BIN="python3"
elif command -v python >/dev/null 2>&1; then
  PYTHON_BIN="python"
fi

if [[ -z "$PYTHON_BIN" ]]; then
  echo "Python not found. Install python3 (or python) to run the PKCE test server." >&2
  exit 1
fi

echo "Serving PKCE test UI at http://localhost:${PORT}/index.html"
exec "$PYTHON_BIN" -m http.server "$PORT"
