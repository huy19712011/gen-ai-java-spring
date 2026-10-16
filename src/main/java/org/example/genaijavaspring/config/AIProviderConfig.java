package org.example.genaijavaspring.config;

import org.example.genaijavaspring.chat.advisor.ErrorWrappingAdvisor;
import org.example.genaijavaspring.chat.advisor.SystemPromptAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AIProviderConfig {

    @Bean("openAIChatClient")
    ChatClient openAIChatClient(OpenAiChatModel openAiChatModel,
                                SimpleLoggerAdvisor simpleLoggerAdvisor,
                                SafeGuardAdvisor safeGuardAdvisor,
                                ErrorWrappingAdvisor errorWrappingAdvisor,
                                SystemPromptAdvisor systemPromptAdvisor) {

        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(simpleLoggerAdvisor, systemPromptAdvisor, safeGuardAdvisor, errorWrappingAdvisor)
                .build();
    }

    @Bean("ollamaChatClient")
    ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel) {

        return ChatClient.builder(ollamaChatModel).build();
    }

    @Bean
    SimpleLoggerAdvisor simpleLoggerAdvisor() {

        return new SimpleLoggerAdvisor(-1);
    }

    @Bean
    SafeGuardAdvisor safeGuardAdvisor() {

        return SafeGuardAdvisor.builder().sensitiveWords(List.of(
                "password", "ssn", "credit card", "iban", "bank account",
                "api-key", "secret", "private_key", "token",
                "confidential", "classified", "internal only", "system prompt", "hack"
        )).order(1).build();
    }


}
