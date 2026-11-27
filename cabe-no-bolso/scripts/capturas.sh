#!/usr/bin/env bash
# Gera as capturas de tela da interface real em docs/capturas/.
# Em máquina sem monitor (servidor), usa xvfb-run; com monitor, roda direto.
set -euo pipefail
cd "$(dirname "$0")/.."
[ -d out ] || ./scripts/compilar.sh
if [ -z "${DISPLAY:-}" ] && command -v xvfb-run >/dev/null; then
  xvfb-run -a -s "-screen 0 1500x1000x24" java -cp out cabenobolso.CapturaTelas docs/capturas
else
  java -cp out cabenobolso.CapturaTelas docs/capturas
fi
