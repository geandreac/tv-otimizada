#!/bin/sh
# Zera as animações e desliga o protetor de tela, guardando o valor antigo de cada um.
#   TV=IP:porta sh ajustes.sh                             os ajustes abaixo
#   TV=IP:porta sh ajustes.sh <namespace> <chave> <valor>  só esse (ex.: DNS que bloqueia anúncios)
cd "$(dirname "$0")"
. ./_log.sh

[ $# -eq 3 ] && { set_kv "$@"; exit; }
set_kv global window_animation_scale 0
set_kv global transition_animation_scale 0
set_kv global animator_duration_scale 0
set_kv secure screensaver_enabled 0
set_kv secure screensaver_activate_on_sleep 0
set_kv secure screensaver_activate_on_dock 0
