/*
package svkreml.ai.openaitextprocessor.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController()
@RequestMapping("/pet")
public class PetClinicController {

    private final ChatClient chatClient;

    public PetClinicController(@Qualifier("chatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/chat")
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



}*/
