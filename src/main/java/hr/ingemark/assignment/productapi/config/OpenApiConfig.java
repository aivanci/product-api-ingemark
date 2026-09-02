package hr.ingemark.assignment.productapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productApiOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Product API")
                .version("v1")
                .description("REST service managing a product catalog. `price_usd` is computed "
                        + "server-side from `price_eur` using the HNB daily middle exchange rate "
                        + "and cannot be supplied by the client."));
    }
}
