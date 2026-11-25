package wardrobe.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** The server: Spring Boot adapters around the domain (ADR-0002). */
@SpringBootApplication
public class WardrobeApplication {
    public static void main(String[] args) {
        SpringApplication.run(WardrobeApplication.class, args);
    }
}
