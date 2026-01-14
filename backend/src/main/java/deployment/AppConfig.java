package deployment;

import service.FrontendService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class AppConfig {
    @Bean
    public FrontendService reservoirService(
            @Value("${app.data.path:../virtualPatientData/pwdb/PWs/SQL/pressure_waveforms.db}") String dataPath,
            @Value("${app.sample-rate-hz:1000}") double sampleRateHz
    ) {
        return new FrontendService(Path.of(dataPath), sampleRateHz);
    }
}
