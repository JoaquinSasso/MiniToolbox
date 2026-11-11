#!/usr/bin/env bash
# Mide el arranque en frío de MiniToolbox en un dispositivo conectado por adb.
#
# Requisitos: la build `benchmark` instalada (.\gradlew.bat assembleBenchmark y
# adb install -r app/build/outputs/apk/benchmark/app-benchmark.apk).
#
# Uso: scripts/measure-cold-start.sh <launcher|widget> [corridas]
#   launcher: intent MAIN/LAUNCHER, como tocar el ícono.
#   widget:   el mismo intent que arma el widget de agua (startRouteJson = water).
#
# Imprime una línea CSV por corrida: modo,corrida,TTID_ms,TTFD_ms.
#   TTID: "TotalTime" de `am start -W` (primer frame).
#   TTFD: "Fully drawn" de ActivityTaskManager, que reporta MainActivity cuando la
#         pantalla pedida queda sola en pantalla, sin transición en curso.
# Al final imprime la mediana de cada columna.
#
# Antes de medir, compilá en un estado fijo para que las corridas sean comparables:
#   adb shell cmd package compile -f -m verify com.joasasso.minitoolbox
set -euo pipefail

MODE=${1:?uso: $0 <launcher|widget> [corridas]}
RUNS=${2:-10}
PKG=com.joasasso.minitoolbox
ACTIVITY=$PKG/.MainActivity

median() { sort -n | awk '{a[NR]=$1} END {if (NR%2) print a[(NR+1)/2]; else print (a[NR/2]+a[NR/2+1])/2}'; }

TTIDS=(); TTFDS=()
for i in $(seq 1 "$RUNS"); do
  adb shell am force-stop "$PKG"
  sleep 1
  adb logcat -c
  case "$MODE" in
    launcher)
      OUT=$(adb shell am start -W -S -n "$ACTIVITY" \
        -a android.intent.action.MAIN -c android.intent.category.LAUNCHER) ;;
    widget)
      # 0x24000000 = FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP, como el widget.
      OUT=$(adb shell "am start -W -S -n $ACTIVITY -f 0x24000000 \
        --es startRouteJson '{\"type\":\"water\"}' --es startSource widget") ;;
    *) echo "modo desconocido: $MODE" >&2; exit 1 ;;
  esac
  sleep 3
  TTID=$(echo "$OUT" | grep TotalTime | tr -dc '0-9')
  # "Fully drawn ...: +612ms" o "+1s034ms"
  TTFD=$(adb logcat -d -s ActivityTaskManager:I | grep "Fully drawn" | tail -1 \
    | sed -E 's/.*\+(([0-9]+)s)?([0-9]+)ms.*/\2 \3/' | awk '{ if (NF==2) print $1*1000+$2; else print $1 }')
  echo "$MODE,$i,$TTID,$TTFD"
  TTIDS+=("$TTID"); TTFDS+=("$TTFD")
done
echo "mediana,$MODE,$(printf '%s\n' "${TTIDS[@]}" | median),$(printf '%s\n' "${TTFDS[@]}" | median)"
