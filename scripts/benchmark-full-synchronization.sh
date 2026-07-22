#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_KEY="${ADMIN_KEY:?Set ADMIN_KEY before running the benchmark}"
RUNS="${RUNS:-3}"
OUTPUT="${OUTPUT:-target/full-synchronization-benchmark.csv}"

mkdir -p "$(dirname "${OUTPUT}")"
printf 'run,http_status,total_seconds,response_bytes\n' > "${OUTPUT}"

for run in $(seq 1 "${RUNS}"); do
  metrics=$(curl --silent --show-error --output /tmp/valorant-sync-response.json \
    --write-out '%{http_code},%{time_total},%{size_download}' \
    --request POST \
    --header "X-Admin-Key: ${ADMIN_KEY}" \
    "${BASE_URL}/api/admin/synchronizations")
  printf '%s,%s\n' "${run}" "${metrics}" | tee -a "${OUTPUT}"
done

awk -F, 'NR > 1 {total += $3; count++} END {if (count > 0) printf "Average: %.3f s over %d runs\n", total / count, count}' "${OUTPUT}"
