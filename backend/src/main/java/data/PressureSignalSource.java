package data;

import model.ArterySite;
import model.PressureSignal;

public interface PressureSignalSource {
    /**
     * Load one patient's pressure waveform for a given arterial site.
     *
     * @param patientId e.g. "pt1631"
     * @param site      arterial site
     * @return pressure signal
     */
    PressureSignal load(String patientId, ArterySite site);
}