#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_KEY="${ADMIN_KEY:?Set ADMIN_KEY before running the benchmark}"
RUNS="${RUNS:-3}"
OUTPUT="${OUTPUT:-target/full-synchronization-benchmark.csv}"

if ! [[ "${RUNS}" =~ ^[1-9][0-9]*$ ]]; then
  printf 'RUNS must be a positive integer.\n' >&2
  exit 1
fi

response_file="$(mktemp)"
trap 'rm -f "${response_file}"' EXIT

mkdir -p "$(dirname "${OUTPUT}")"
printf 'run,http_status,total_seconds,response_bytes\n' > "${OUTPUT}"

for run in $(seq 1 "${RUNS}"); do
  metrics=$(curl --silent --show-error --output "${response_file}" \
    --write-out '%{http_code},%{time_total},%{size_download}' \
    --request POST \
    --header "X-Admin-Key: ${ADMIN_KEY}" \
    "${BASE_URL}/api/admin/synchronizations")

  http_status="${metrics%%,*}"
  if [[ "${http_status}" != "200" ]]; then
    printf 'Synchronization run %s failed with HTTP %s.\n' "${run}" "${http_status}" >&2
    cat "${response_file}" >&2
    exit 1
  fi

  printf '%s,%s\n' "${run}" "${metrics}" | tee -a "${OUTPUT}"
done

awk -F, '
  NR > 1 {
    total += $3
    count++
  }
  END {
    if (count > 0) {
      printf "Average: %.3f s over %d runs\n", total / count, count
    }
  }
' "${OUTPUT}"
