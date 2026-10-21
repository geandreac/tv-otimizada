# Funções compartilhadas pelos scripts: tv (adb shell sem \r) e log (anota a mudança e como desfazer).
# Cada mudança vai para alteracoes-log.txt e para desfazer-tudo.sh, que roda da mais nova para a mais velha.
: "${TV:?defina a TV: TV=192.168.0.10:5555 sh <script>}"
ADB=${ADB:-adb}
"$ADB" connect "$TV" >/dev/null 2>&1
[ "$("$ADB" -s "$TV" get-state 2>/dev/null)" = device ] || { echo "Não consegui falar com $TV. Ligue a depuração e aceite o pedido na TV." >&2; exit 1; }
tv() { "$ADB" -s "$TV" shell "$@" </dev/null | tr -d '\r'; }

UNDO=desfazer-tudo.sh
[ -f "$UNDO" ] || cat > "$UNDO" <<'EOF'
#!/bin/sh
# Desfaz todas as mudanças feitas na TV, na ordem inversa. Gerado automaticamente.
#   sh desfazer-tudo.sh IP:porta      (ADB=/caminho/adb se o adb não estiver no PATH)
ADB=${ADB:-adb}; TV=${1:?uso: sh desfazer-tudo.sh IP:porta}
"$ADB" connect "$TV" >/dev/null 2>&1
u() { echo "> $*"; "$ADB" -s "$TV" shell "$@" </dev/null; }
# --- mudanças (a mais recente fica no topo) ---
# --- fim ---
echo "Pronto. Reinicie a TV: $ADB -s $TV reboot"
EOF

# log <comando feito> <comando que desfaz>
log() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') | feito: $1 | desfazer: $2" >> alteracoes-log.txt
  awk -v l="u $2" '{print} /^# --- mudanças/{print l}' "$UNDO" > "$UNDO.tmp" && mv "$UNDO.tmp" "$UNDO"
}

# set_kv <namespace> <chave> <valor>: muda uma configuração guardando o valor antigo.
set_kv() {
  old=$(tv settings get "$1" "$2")
  [ "$old" = "$3" ] && { echo "ja estava     $1 $2 = $3"; return; }
  tv settings put "$1" "$2" "$3"
  if [ "$old" = null ] || [ -z "$old" ]; then undo="settings delete $1 $2"; else undo="settings put $1 $2 $old"; fi
  log "settings put $1 $2 $3" "$undo"
  echo "alterado      $1 $2: $old -> $3"
}
