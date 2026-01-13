package deployment;

import application.ReservoirService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class AppConfig {
    @Bean
    public ReservoirService reservoirService(
            @Value("${app.data.path:../virtualPatientData/pwdb/PWs/CSV}") String dataPath,
            @Value("${app.sample-rate-hz:1000}") double sampleRateHz
    ) {
        return new ReservoirService(Path.of(dataPath), sampleRateHz);
    }
}
