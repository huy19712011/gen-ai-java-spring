package org.example.genaijavaspring.chat.openai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/openai/chat")
public class OpenAIChatController {

    private final static String SYSTEM_PROMPT = "You are a helpful assistant that summarize any given content. " +
            "Ensure the summary is concise, informative, and captures the key points. " +
            "Use a friendly and approachable tone while maintaining professionalism." +
            "Do not answer anything other than the summarization. If the question is not about summarization," +
            "respond with 'I can only help with summarization tasks.'";

    private final ChatClient chatClient;

    public OpenAIChatController(@Qualifier("openAIChatClient") ChatClient chatClient) {

        this.chatClient = chatClient;
    }

    @PostMapping("/summarize")
    public ChatClientResponse summarize(@RequestBody String message) {

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .chatClientResponse();
    }
}
