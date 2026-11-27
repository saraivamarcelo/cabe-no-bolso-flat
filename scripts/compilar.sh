#!/usr/bin/env bash
# Compila todo o projeto (código e testes) para a pasta out/. Execute a partir de qualquer lugar.
set -euo pipefail
cd "$(dirname "$0")/.."
rm -rf out && mkdir out
javac -encoding UTF-8 -d out $(find src -name '*.java')
echo "Compilado em out/"
