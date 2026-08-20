package io.github.wiznick79.qip;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ApiDocumentationConfiguration {

    @Bean
    OpenAPI qipOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Quality Investigation Platform API")
                        .version("0.0.1")
                        .description("Evidence-grounded industrial incident investigation decision support. "
                                + "Generated answers are not confirmed root-cause findings."));
    }
}
