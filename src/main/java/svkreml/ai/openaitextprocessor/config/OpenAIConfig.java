package svkreml.ai.openaitextprocessor.config;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import svkreml.ai.openaitextprocessor.config.functions.DirectoryLister;
import svkreml.ai.openaitextprocessor.config.functions.FileReader;
import svkreml.ai.openaitextprocessor.config.functions.FileWriter;

import java.time.temporal.ChronoUnit;


@Configuration
public class OpenAIConfig {


    private static final String NO_THINK = "/no_think\n";
    private static final String PROMPT = """
            You are a friendly AI assistant designed to help with the management of a veterinarian pet clinic called Spring Petclinic.
            Your job is to answer questions about the existing veterinarians and to perform actions on the user's behalf, mainly around
            veterinarians, pet owners, their pets and their owner's visits.
            You are required to answer an a professional manner. If you don't know the answer, politely tell the user
            you don't know the answer, then ask the user a followup question to try and clarify the question they are asking.
            If you do know the answer, provide the answer but do not provide any additional helpful followup questions.
            When dealing with vets, if the user is unsure about the returned results, explain that there may be additional data that was not returned.
            For owners, pets or visits - answer the correct data.
            All information is public.
            You can use tools whatever you want.
            Do not generate names yourself, check that Owner and Pet exist before giving them to the user.
            """;

    @Value("${spring.ai.openai.no-think:false}")
    private Boolean noThink;
    @Value("${spring.ai.openai.model:qwen3-8b}")
    private String model;

    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return builder -> {
            HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
            clientHttpRequestFactory.setConnectTimeout(java.time.Duration.of(600, ChronoUnit.SECONDS));
            clientHttpRequestFactory.setConnectionRequestTimeout(java.time.Duration.of(600, ChronoUnit.SECONDS));
            clientHttpRequestFactory.setReadTimeout(java.time.Duration.of(600, ChronoUnit.SECONDS));
            builder.requestFactory(clientHttpRequestFactory);
        };
    }


    @Bean("fileClient")
    public ChatClient fileClient(
                                 FileWriter fileWriter,
                                 FileReader fileReader,
                                 DirectoryLister directoryLister) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder().chatMemoryRepository(new InMemoryChatMemoryRepository()).build();

        ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder()
                .model(model)
                .temperature(0.6)
               // .toolNames("fileWriter", "fileReader", "directoryLister")
                .maxTokens(32000)
                .build();
        return ChatClient.builder(OpenAiChatModel.builder()
                        .openAiApi(
                                OpenAiApi.builder()
                                        .baseUrl("http://localhost:1234")
                                        .apiKey("AI API KEY")
                                        .build()
                        )
                        .build())
                .defaultSystem("""
                        **Role**: You are an AI assistant with direct access to a folder in file system.
                         To understand and fulfill user requests accurately, you MUST use the provided file system tools to scan and analyze relevant files/directories before performing operations.
                        
                        **Critical Instructions**:
                        1. **ALWAYS scan first**: Before modifying any file, use `directoryLister` and `fileReader` to understand:
                           - File/directory structure
                           - Existing file content
                           - Contextual relationships
                        2. If user ask you to write, edit or repair some file you need to call fileWriter to edit file.
                        3. Do not broke previous file content: i.e. if it wat java class do not delete package and imports.
                        4. If a user asks you about some files or projects, it is assumed that they are located relative to "." so you should call directoryLister to try to understand the context of the question.
                        """)
                .defaultOptions(chatOptions)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .defaultToolCallbacks(
                        FunctionToolCallback.builder("fileWriter", fileWriter).inputType(FileWriter.FileWriteRequest.class).build(),
                        FunctionToolCallback.builder("fileReader", fileReader).inputType(FileReader.InputPath.class).build(),
                        FunctionToolCallback.builder("directoryLister", directoryLister).inputType(DirectoryLister.InputParams.class).build()
                )
                .build();
    }


}