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

if [[ -z "${DATABASE_URL:-}" ]]; then
  echo "DATABASE_URL must be set for PostgreSQL access." >&2
  exit 1
fi

csv_dir="${1:-$root_dir/virtualPatientData/pwdb/PWs/CSV}"
output_file="${2:-$root_dir/backend/build/pwdb_waveforms.csv}"
schema_file="$root_dir/backend/scripts/init-postgres.sql"

if [[ ! -d "$csv_dir" ]]; then
  echo "CSV directory not found: $csv_dir" >&2
  exit 1
fi

python3 "$root_dir/backend/scripts/prepare_pwdb_csv_export.py" \
  --input-dir "$csv_dir" \
  --output-file "$output_file"

psql "$DATABASE_URL" -f "$schema_file"
psql "$DATABASE_URL" -c "\copy pressure_waveforms (patient_id, site, samples) FROM '${output_file}' WITH (FORMAT csv)"

echo "Seeded PostgreSQL from CSVs in $csv_dir."
