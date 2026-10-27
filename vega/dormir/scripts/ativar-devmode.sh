#!/bin/bash
# Liga o Developer Mode do Fire TV com Vega OS: login na conta Amazon Developer e envio do código da TV.
# No WSL não há chaveiro de senhas, e o vega guarda o token nele. Por isso tudo roda numa única sessão D-Bus
# com um gnome-keyring temporário (apt install gnome-keyring dbus-x11):
#   dbus-run-session -- bash ativar-devmode.sh
# Em Ubuntu com desktop ou macOS basta: vega devmode login && vega devmode enable-device --code CODIGO
source ~/vega/env
echo -n dormir | gnome-keyring-daemon --unlock --components=secrets >/dev/null
vega devmode login || exit 1
echo "Na TV: Configurações > Minha Fire TV > Opções do desenvolvedor > Developer Mode (o código vale 5 minutos)."
read -r -p "Código da TV: " code
vega devmode enable-device --code "$(echo "$code" | tr -d '[:space:]')"
