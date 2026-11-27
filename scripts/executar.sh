#!/usr/bin/env bash
# Abre a aplicação (compila antes se necessário).
set -euo pipefail
cd "$(dirname "$0")/.."
[ -d out ] || ./scripts/compilar.sh
java -cp out cabenobolso.Main
