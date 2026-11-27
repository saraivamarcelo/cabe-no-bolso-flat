#!/usr/bin/env bash
# Compila e executa todos os testes. Grava as evidências em docs/evidencias-testes.md.
# Código de saída 0 = todas as verificações passaram.
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/compilar.sh
java -cp out cabenobolso.TestesRunner docs/evidencias-testes.md
