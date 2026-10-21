#!/bin/sh
# TCL: religa o botão Home da home nova depois de um REINÍCIO COMPLETO (falta de luz, atualização, tomada).
# O firmware (ro.feature.ams_boot_app_manager) zera a liberação de início automático e desliga a acessibilidade
# a cada boot, e só o adb consegue liberar de novo. Desligar e ligar pelo controle (suspensão) não precisa disto.
#   TV=IP:porta sh reativar-home.sh
ADB=${ADB:-adb}; : "${TV:?defina a TV: TV=192.168.0.10:5555 sh reativar-home.sh}"
"$ADB" connect "$TV" >/dev/null 2>&1
s() { "$ADB" -s "$TV" shell "$@" </dev/null | tr -d '\r'; }
s cmd appops set com.gathin.tvhome AUTO_START allow
s settings delete secure enabled_accessibility_services >/dev/null
s settings put secure enabled_accessibility_services com.gathin.tvhome/.HomeKeyService
s settings put secure accessibility_enabled 1
sleep 4
case "$(s dumpsys accessibility | grep -m1 'Bound services')" in
  *Service*) echo "Pronto: o botão Home abre a home nova." ;;
  *) echo "O serviço não subiu. Veja: adb shell logcat -d | grep TclAppBoot" >&2; exit 1 ;;
esac
