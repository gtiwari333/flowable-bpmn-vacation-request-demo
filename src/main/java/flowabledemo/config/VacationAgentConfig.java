package flowabledemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class VacationAgentConfig {

    @Value("classpath:/vacation-agent-system-prompt.txt")
    private Resource agentSystemDefault;

    @Bean
    public ChatClient buildVacationAgentChatClient(ChatClient.Builder builder, VectorStore vacationRuleVectorStore) {
        return builder
                .defaultSystem(agentSystemDefault)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vacationRuleVectorStore).build())
                .build();
    }
}
