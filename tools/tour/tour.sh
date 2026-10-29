#!/usr/bin/env bash
# Regenerates the README media from the real app on a copy of a squad's database:
# docs/media/tour.webp and docs/media/screen-*.png. See tools/tour/README.md.
# Usage: tools/tour/tour.sh <dump.sql.gz>
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TOUR="$ROOT/tools/tour"
MEDIA="$ROOT/docs/media"
DUMP="${1:?Usage: tools/tour/tour.sh <dump.sql.gz>, a plain pg_dump of a ValoQuests database}"

# The images of compose.yaml under a project of their own: a throwaway database, the app on 8100.
COMPOSE=(docker compose -p valoquests-tour -f "$ROOT/compose.yaml" -f "$TOUR/compose.tour.yaml")

WORK="$(mktemp -d)"

# Stops the stack and drops its database whatever happened; keeps the logs of a failed run.
cleanup() {
  local status=$?
  "${COMPOSE[@]}" logs >"$WORK/compose.log" 2>&1 || true
  "${COMPOSE[@]}" down --volumes >/dev/null 2>&1 || true
  if [ "$status" -eq 0 ]; then rm -rf "$WORK"; else echo "Logs kept in $WORK" >&2; fi
}
trap cleanup EXIT

python3 -I -c 'from PIL import features; assert features.check("webp")' ||
  { echo 'Python 3 with Pillow (WebP support) is needed.' >&2; exit 1; }

echo '>> Images'
"${COMPOSE[@]}" build >"$WORK/build.log" 2>&1

echo '>> Squad'
# Restored before the backend first starts, so Flyway finds the schema already migrated.
"${COMPOSE[@]}" up --detach --wait postgres >>"$WORK/build.log" 2>&1
gunzip --stdout "$DUMP" |
  "${COMPOSE[@]}" exec -T postgres psql --quiet -v ON_ERROR_STOP=1 -U valorant -d valo_quests >"$WORK/restore.log"
"${COMPOSE[@]}" up --detach --wait >>"$WORK/build.log" 2>&1

echo '>> Tour'
node "$TOUR/record.mjs" "$WORK/record"
python3 -I "$TOUR/encode_tour.py" "$WORK/record" "$MEDIA/tour.webp"

echo '>> Screens'
node "$TOUR/shots.mjs" "$WORK/shots"
python3 -I "$TOUR/frame_screens.py" "$WORK/shots" "$MEDIA"
