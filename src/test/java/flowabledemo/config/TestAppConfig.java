package flowabledemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@TestConfiguration
@Lazy // allows for initialization after the server port is known
public class TestAppConfig {
    @LocalServerPort
    int port;

    @Bean
    VacationApi vacationApiClient() {
        var webClient = WebClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        var factory = HttpServiceProxyFactory.builder(WebClientAdapter.forClient(webClient)).build();

        return factory.createClient(VacationApi.class);
    }

    @Bean
    DefinitionsApi definitionsApi() {
        var webClient = WebClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        var factory = HttpServiceProxyFactory.builder(WebClientAdapter.forClient(webClient)).build();

        return factory.createClient(DefinitionsApi.class);
    }
}
