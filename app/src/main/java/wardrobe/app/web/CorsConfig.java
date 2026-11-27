package wardrobe.app.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The Expo web build runs on another local port; it may read (GET) the API, change a status (PUT) and dress the
 * avatar (POST with a JSON body). Nothing else is opened.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "PUT", "POST"); // request headers: Spring's default, any (Content-Type included)
    }
}
