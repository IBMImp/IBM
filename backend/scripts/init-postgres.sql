CREATE TABLE IF NOT EXISTS pressure_waveforms (
    patient_id TEXT NOT NULL,
    site TEXT NOT NULL,
    samples TEXT NOT NULL,
    PRIMARY KEY (patient_id, site)
);
