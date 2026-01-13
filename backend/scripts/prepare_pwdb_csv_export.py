#!/usr/bin/env python3
import argparse
import csv
from pathlib import Path


def normalize_patient_id(raw: str) -> str:
    trimmed = raw.strip().lower()
    if not trimmed:
        return trimmed
    if trimmed.startswith("pt"):
        suffix = trimmed[2:].strip()
        return f"pt{suffix}"
    if trimmed.isdigit():
        return f"pt{trimmed}"
    return trimmed


def iter_site_files(csv_dir: Path):
    for path in sorted(csv_dir.glob("PWs_*_P.csv")):
        token = path.name[len("PWs_") : -len("_P.csv")]
        if token:
            yield token, path


def build_rows(csv_dir: Path):
    for site_token, csv_path in iter_site_files(csv_dir):
        with csv_path.open(newline="") as handle:
            reader = csv.reader(handle)
            for row in reader:
                if not row or len(row) < 2:
                    continue
                patient_id = normalize_patient_id(row[0])
                if not patient_id:
                    continue
                samples = ",".join(value.strip() or "NaN" for value in row[1:])
                yield patient_id, site_token, samples


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--input-dir",
        type=Path,
        required=True,
        help="Directory containing PWDB CSV files (PWs_<Site>_P.csv).",
    )
    parser.add_argument(
        "--output-file",
        type=Path,
        required=True,
        help="CSV output path for PostgreSQL import.",
    )
    args = parser.parse_args()

    if not args.input_dir.is_dir():
        raise SystemExit(f"Input directory not found: {args.input_dir}")

    rows = list(build_rows(args.input_dir))
    if not rows:
        raise SystemExit("No CSV rows found to export.")

    args.output_file.parent.mkdir(parents=True, exist_ok=True)
    with args.output_file.open("w", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerows(rows)


if __name__ == "__main__":
    main()
