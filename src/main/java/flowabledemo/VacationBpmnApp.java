package flowabledemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.common.rest.exception.BaseExceptionHandlerAdvice;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class VacationBpmnApp {

    static void main(String[] args) {
        SpringApplication.run(VacationBpmnApp.class, args);
    }

    @Configuration  //required for flowable rest + swagger
    @ComponentScan({"org.flowable.rest.**" }) //required for flowable rest + swagger
    static class ProcessEngineRestConfiguration {

        @Bean
        public RestResponseFactory restResponseFactory(ObjectMapper objectMapper) {
            return new RestResponseFactory(objectMapper);
        }
    }

}



