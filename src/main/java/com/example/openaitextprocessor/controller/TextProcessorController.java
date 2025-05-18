package com.example.openaitextprocessor.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
//@RequiredArgsConstructor
public class TextProcessorController {

    private final ChatClient chatClient;

    public TextProcessorController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/chatclient")
    public String exchange(@RequestBody String query) {
        //All chatbot messages go through this endpoint and are passed to the LLM
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

}