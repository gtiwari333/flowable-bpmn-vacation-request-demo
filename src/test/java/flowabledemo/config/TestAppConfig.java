package flowabledemo.config;

import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@Lazy // allows for initialization after the server port is known
public class TestAppConfig {
    @LocalServerPort
    int port;

    @Bean
    VacationApi vacationApiClient() {
        var webClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(webClient)).build();

        return factory.createClient(VacationApi.class);
    }

    @Bean
    DefinitionsApi definitionsApi() {
        var webClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(webClient)).build();

        return factory.createClient(DefinitionsApi.class);
    }
}
