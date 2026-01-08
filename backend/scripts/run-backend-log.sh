#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root_dir="$script_dir"
while [[ ! -f "$root_dir/gradlew" && "$root_dir" != "/" ]]; do
  root_dir="$(dirname "$root_dir")"
done
if [[ ! -f "$root_dir/gradlew" ]]; then
  echo "Could not find gradlew; please run from within the repository." >&2
  exit 1
fi

log_file="$root_dir/backend/logs/run.log"
timestamp="$(date +"%Y-%m-%d %H:%M:%S %Z")"

run_output="$(mktemp)"
header="===== ${timestamp} ====="

mkdir -p "$(dirname "$log_file")"

{
  echo "$header"
  echo "Command: ./gradlew -p backend run"
  echo
  "$root_dir/gradlew" -p "$root_dir/backend" run
  echo
} | tee "$run_output"

if [[ -f "$log_file" ]]; then
  cat "$run_output" "$log_file" > "${log_file}.tmp"
  mv "${log_file}.tmp" "$log_file"
else
  mv "$run_output" "$log_file"
fi

if [[ -f "$run_output" ]]; then
  rm -f "$run_output"
fi