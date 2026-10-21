#!/bin/sh
# Instala a home glass e o app Dormir e liga o serviço que leva o botão Home para a home nova.
# A home original NUNCA é desligada: na TCL G08 isso derrubou o Wi-Fi, e no Fire OS ela é protegida.
#   TV=IP:porta sh instalar-home.sh        (compile antes: cd launcher && ./gradlew assembleRelease)
cd "$(dirname "$0")"
. ./_log.sh
ME=com.gathin.tvhome
SVC=$ME/.HomeKeyService
for apk in ../launcher/app/build/outputs/apk/release/app-release.apk ../launcher/dormir/build/outputs/apk/release/dormir-release.apk; do
  [ -f "$apk" ] || { echo "Falta $apk. Compile antes: cd launcher && ./gradlew assembleRelease" >&2; exit 1; }
  pkg=$(case "$apk" in *dormir*) echo com.gathin.dormir ;; *) echo $ME ;; esac)
  out=$("$ADB" -s "$TV" install -r "$apk" 2>&1) || { echo "Falha ao instalar $pkg: $out" >&2; exit 1; }
  log "adb install $pkg" "pm uninstall $pkg"
done
tv pm grant $ME android.permission.ACCESS_FINE_LOCATION   # nome do Wi-Fi no botão de rede
tv cmd package compile -m speed -f $ME >/dev/null

case "$(tv getprop ro.product.manufacturer)" in
  Amazon)
    # Fire OS segura a tecla Home; o serviço vê a home da Amazon aparecer e volta por um alarme,
    # que só dispara na hora com min_futurity=0.
    set_kv global alarm_manager_constants min_futurity=0 ;;
  TCL)
    # O gerenciador de inicialização da TCL bloqueia serviços de terceiros sem esta liberação.
    tv cmd appops set $ME AUTO_START allow && log "cmd appops set $ME AUTO_START allow" "cmd appops set $ME AUTO_START ignore" ;;
esac

# Religa o serviço do zero: uma reinstalação deixa a acessibilidade com a versão antiga.
old=$(tv settings get secure enabled_accessibility_services)
tv settings delete secure enabled_accessibility_services >/dev/null
set_kv secure enabled_accessibility_services "$SVC"
[ "$old" = null ] || [ -z "$old" ] || [ "$old" = "$SVC" ] || echo "Aviso: havia outro serviço de acessibilidade ($old). Religue-o se precisar."
set_kv secure accessibility_enabled 1
sleep 4
case "$(tv dumpsys accessibility | grep -m1 'Bound services')" in
  *Service*) echo "Pronto: aperte Home no controle." ;;
  *) echo "O serviço não subiu. Veja: adb shell logcat -d | grep -i -e TclAppBoot -e HomeKeyService" >&2; exit 1 ;;
esac
