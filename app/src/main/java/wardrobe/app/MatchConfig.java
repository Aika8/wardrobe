package wardrobe.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import wardrobe.domain.match.Combination;
import wardrobe.domain.match.CombinationRule;

/** Which combination rules the server judges with: the domain's standard set, built in one place. */
@Configuration
public class MatchConfig {

    @Bean
    public CombinationRule combination() {
        return Combination.standard();
    }
}
