package org.example.genaijavaspring.config;

import org.example.genaijavaspring.chat.advisor.ErrorWrappingAdvisor;
import org.example.genaijavaspring.chat.advisor.SystemPromptAdvisor;
import org.example.genaijavaspring.chat.advisor.ValidationAdvisor;
import org.example.genaijavaspring.chat.openai.jailbreak.demo.BankingTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
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
                                SystemPromptAdvisor systemPromptAdvisor,
                                ValidationAdvisor validationAdvisor) {

        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(simpleLoggerAdvisor, systemPromptAdvisor, safeGuardAdvisor, errorWrappingAdvisor, validationAdvisor)
                .build();
    }

    @Bean("openAIGeneralChatClient")
    ChatClient openAIGeneralChatClient(OpenAiChatModel openAiChatModel, BankingTools bankingTools) {
        return ChatClient.builder(openAiChatModel)
                .defaultTools(bankingTools)
                .build();
    }

    @Bean("ollamaChatClient")
    ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel) {

        return ChatClient.builder(ollamaChatModel).build();
    }

    @Bean("openAIChatClientWithMemory")
    ChatClient openAIChatClientWithMemory(OpenAiChatModel openAiChatModel,
                                          ChatMemory chatMemory) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
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
