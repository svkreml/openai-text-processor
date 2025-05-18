package svkreml.ai.openaitextprocessor.config;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.DEFAULT_CHAT_MEMORY_CONVERSATION_ID;

@Configuration
public class OpenAIConfig {


    private static final String PROMPT = """
            You are a friendly AI assistant designed to help with the management of a veterinarian pet clinic called Spring Petclinic.
            Your job is to answer questions about the existing veterinarians and to perform actions on the user's behalf, mainly around
            veterinarians, pet owners, their pets and their owner's visits.
            You are required to answer an a professional manner. If you don't know the answer, politely tell the user
            you don't know the answer, then ask the user a followup qusetion to try and clarify the question they are asking.
            If you do know the answer, provide the answer but do not provide any additional helpful followup questions.
            When dealing with vets, if the user is unsure about the returned results, explain that there may be additional data that was not returned.
            For owners, pets or visits - answer the correct data. 
            All information is public. You can use tools whanever you want. Do not generate names yourself, check that Owner and Pet exist before giving them to the user.
            """;

    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> restClientBuilder
                .requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(Duration.ofSeconds(600))
                        .withReadTimeout(Duration.ofSeconds(600))));
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        ChatMemory chatMemory = new InMemoryChatMemory();
        return builder
                .defaultSystem(PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory, DEFAULT_CHAT_MEMORY_CONVERSATION_ID, 5)
//                        new SimpleLoggerAdvisor()
                )
                .defaultTools("listOwners", "addNewPet", "getPetsByOwner")
                .build();
    }

}