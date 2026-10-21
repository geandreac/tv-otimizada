#!/bin/sh
# Desliga (pm disable-user) pacotes, um por vez, conferindo depois de cada um que a TV ainda responde.
# Nunca desinstala. Cada pacote vai para o log e para desfazer-tudo.sh.
#   TV=IP:porta sh desligar-apps.sh ../listas/firetv-stick-lite.txt
#   TV=IP:porta sh desligar-apps.sh com.exemplo.app outro.pacote
# Uma lista é um arquivo com um pacote por linha; '#' começa um comentário.
cd "$(dirname "$0")"
. ./_log.sh
[ $# -gt 0 ] || { echo "Passe uma lista (arquivo) ou nomes de pacotes." >&2; exit 1; }

for arg in "$@"; do
  if [ -f "$arg" ]; then sed 's/#.*//' "$arg"; else echo "$arg"; fi
done | tr -d ' \t\r' | grep -v '^$' | while read -r p; do
  if [ -n "$(tv pm list packages -d "$p" | grep -x "package:$p")" ]; then echo "ja desligado  $p"; continue; fi
  [ -n "$(tv pm list packages "$p" | grep -x "package:$p")" ] || { echo "nao instalado $p"; continue; }
  out=$(tv pm disable-user --user 0 "$p" 2>&1)
  case "$out" in
    *"disabled-user"*)
      log "pm disable-user --user 0 $p" "pm enable $p"
      [ "$("$ADB" -s "$TV" get-state 2>/dev/null)" = device ] || { echo "A TV parou de responder depois de $p. Parando." >&2; exit 1; }
      echo "desligado     $p" ;;
    *) echo "RECUSADO      $p (pacote protegido pelo sistema)" ;;
  esac
done
