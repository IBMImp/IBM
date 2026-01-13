#!/usr/bin/env python3
import argparse
import csv
import sqlite3
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert PWDB CSV pressure waveforms into a SQLite database."
    )
    parser.add_argument(
        "--csv-dir",
        type=Path,
        required=True,
        help="Directory containing PWs_<Site>_P.csv files.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        required=False,
        help=(
            "Output SQLite database file path. Defaults to "
            "<csv-dir>/../SQL/pressure_waveforms.db."
        ),
    )
    return parser.parse_args()


def create_schema(conn: sqlite3.Connection) -> None:
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS pressure_waveforms (
            patient_id TEXT NOT NULL,
            site TEXT NOT NULL,
            samples TEXT NOT NULL,
            PRIMARY KEY (patient_id, site)
        )
        """
    )


def site_token_from_filename(filename: str) -> str:
    name = Path(filename).stem
    if not name.startswith("PWs_") or not name.endswith("_P"):
        raise ValueError(f"Unexpected PWDB filename format: {filename}")
    return name.removeprefix("PWs_").removesuffix("_P")


def load_csv_file(csv_file: Path, conn: sqlite3.Connection) -> int:
    site_token = site_token_from_filename(csv_file.name)
    inserted = 0
    with csv_file.open(newline="") as handle:
        reader = csv.reader(handle)
        for row in reader:
            if not row:
                continue
            patient_id = row[0].strip()
            if not patient_id or patient_id.lower() in {"patient", "patientid"}:
                continue
            samples = ",".join(value.strip() for value in row[1:])
            conn.execute(
                """
                INSERT OR REPLACE INTO pressure_waveforms (patient_id, site, samples)
                VALUES (?, ?, ?)
                """,
                (patient_id, site_token, samples),
            )
            inserted += 1
    return inserted


def main() -> None:
    args = parse_args()
    csv_dir = args.csv_dir.expanduser().resolve()
    if args.output is None:
        output = (csv_dir.parent / "SQL" / "pressure_waveforms.db").resolve()
    else:
        output = args.output.expanduser().resolve()

    if not csv_dir.is_dir():
        raise SystemExit(f"CSV directory not found: {csv_dir}")

    output.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(output)
    try:
        create_schema(conn)
        total = 0
        for csv_file in sorted(csv_dir.glob("PWs_*_P.csv")):
            total += load_csv_file(csv_file, conn)
        conn.commit()
    finally:
        conn.close()

    print(f"Wrote {total} waveforms into {output}")


if __name__ == "__main__":
    main()
