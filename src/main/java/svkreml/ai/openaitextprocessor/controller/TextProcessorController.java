package svkreml.ai.openaitextprocessor.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController

public class TextProcessorController {

    private final ChatClient chatClient;

    private final ChatClient fileClient;

    public TextProcessorController( @Qualifier("chatClient") ChatClient chatClient, @Qualifier("fileClient") ChatClient fileClient) {
        this.chatClient = chatClient;
        this.fileClient = fileClient;
    }

    @PostMapping("/chatClient")
    public String chatClient(@RequestBody String query) {
        return
                this.chatClient
                        .prompt()
                        .user(
                                u ->
                                        u.text(query)
                        )
                        .call()
                        .content();
    }


    @PostMapping("/fileClient")
    public String fileClient(@RequestBody String query) {
        return
                this.fileClient
                        .prompt()
                        .user(
                                u ->
                                        u.text(query)
                        )
                        .call()
                        .content();
    }

}