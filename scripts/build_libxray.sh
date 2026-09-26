#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="$ROOT/.native/libXray"
rm -rf "$WORK"
mkdir -p "$ROOT/app/libs"
git clone --depth 1 --branch v26.9.9 https://github.com/XTLS/libXray.git "$WORK"
go install golang.org/x/mobile/cmd/gomobile@latest
go install golang.org/x/mobile/cmd/gobind@latest
export PATH="$(go env GOPATH)/bin:$PATH"
export GOANDROIDAPI=21
gomobile init
python3 "$WORK/build/main.py" android
AAR="$(find "$WORK" -type f -name '*.aar' | head -n 1)"
if [[ -z "$AAR" ]]; then echo "libXray AAR was not produced" >&2; exit 1; fi
cp "$AAR" "$ROOT/app/libs/libXray.aar"
echo "Created $ROOT/app/libs/libXray.aar"
