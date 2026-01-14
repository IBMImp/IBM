# IBM Arterial Reservoir Pressure Monitor

## Overview
This project provides a desktop application for visualizing arterial pressure waveforms and computing arterial reservoir pressure using a Java-based frontend and backend. The frontend handles UI, plotting, and interaction, while the backend performs waveform processing, parameter estimation, and reservoir pressure computation.

## Project Structure
- `frontend/`: Swing-based UI, charts, and interaction logic.
- `backend/`: Computational pipeline for waveform processing and reservoir pressure calculation.
- `virtualPatientData/`: Sample waveform datasets used by the application.
- `build.gradle` / `settings.gradle`: Gradle build configuration.

## Branch Guide
- `main`: Primary working application branch.
- `master`: Deployment/testing branch.
- `websockets`: Continuous waveform testing branch.
- `matlab`: MATLAB reference testing branch.
- Other branches: Deprecated or archival branches.

## Getting Started
### Prerequisites
- Java (JDK) compatible with the Gradle build (depends on the branch - e.g. 17 used for deployment on the master branch)

### Build
```bash
./gradle build
```

### Run (Application)
```bash
./gradlew run
```

### Run (Frontend)
```bash
./gradlew :frontend:run
```

### Run (Backend)
```bash
./gradlew :backend:run
```

### Run (bootRun - booting the Tomcat servlet for backend)
```bash
./gradlew bootRun
```

## Data
- By default, the application looks for waveform CSV data under `virtualPatientData/pwdb/PWs/CSV`.
- You can configure the data path via environment or system properties supported by the app.

## Notes
- The help dialog in the UI includes guidance on systole/diastole detection and contact information.
- Invalid patient IDs or sample rates will surface errors during setup.
