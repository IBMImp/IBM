package deployment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "deployment",
        "application",
        "calculation",
        "data",
        "estimation",
        "model",
        "preprocessing",
        "service"
})
public class Main {
    public static void main(String[] args) {
        System.out.println("BOOT: Main starting. PWDB_IMPORT=" + System.getenv("PWDB_IMPORT"));
        System.out.println("BOOT: PWDB_IMPORT_URL=" + System.getenv("PWDB_IMPORT_URL"));
        SpringApplication.run(Main.class, args);
    }
}
