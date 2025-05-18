package com.example.openaitextprocessor.service;

import com.example.openaitextprocessor.function.CalculatorFunction;
import com.example.openaitextprocessor.function.TimeFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAIService {

    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.model}")
    private String model;

    public String fixSpelling(String content) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(),
                "You are a helpful assistant that specializes in fixing spelling and grammar mistakes. " +
                        "Fix the spelling and grammar in the following text, but maintain the original meaning and style. " +
                        "Return only the corrected text without any explanations or remarks. " +
                        "You can use the getCurrentTime function to add timestamps and calculator for any numerical corrections."));
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), content));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .maxTokens(16000)
                .messages(messages)
                .functions(getFunctionDefinitions())
                .functionCall(ChatCompletionRequest.ChatCompletionRequestFunctionCall.of("auto"))
                .build();

        var response = openAiService.createChatCompletion(request);
        var responseMessage = response.getChoices().get(0).getMessage();

        // Handle function calls if any
        if (responseMessage.getFunctionCall() != null) {
            String result = handleFunctionCall(responseMessage.getFunctionCall());
            messages.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), responseMessage.getContent()));
            messages.add(new ChatMessage(ChatMessageRole.FUNCTION.value(), result));

            request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .build();

            response = openAiService.createChatCompletion(request);
            responseMessage = response.getChoices().get(0).getMessage();
        }

        return responseMessage.getContent();
    }


    public String getTime() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(),
                "You are a helpful assistant that specializes in telling time. " +
                        "You can use the getCurrentTime function to add timestamps"));
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), "Hello! use getCurrentTime function and tell me result"));


        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .maxTokens(16000)
                .messages(messages)
                .functions(getFunctionDefinitions())
                .functionCall(
                        ChatCompletionRequest.ChatCompletionRequestFunctionCall.of("getCurrentTime")
                )
                .build();

        var response = openAiService.createChatCompletion(request);
        var responseMessage = response.getChoices().get(0).getMessage();

        // Handle function calls if any
        if (responseMessage.getFunctionCall() != null) {
            String result = handleFunctionCall(responseMessage.getFunctionCall());
            messages.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), responseMessage.getContent()));
            messages.add(new ChatMessage(ChatMessageRole.FUNCTION.value(), result));

            request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .build();

            response = openAiService.createChatCompletion(request);
            responseMessage = response.getChoices().get(0).getMessage();
        }

        return responseMessage.getContent();
    }

    public String translate(String content, String targetLanguage) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(),
                "You are a helpful assistant that specializes in translation. " +
                        "Translate the following text to " + targetLanguage + " language. " +
                        "Return only the translated text without any explanations or remarks. " +
                        "You can use the getCurrentTime function to add timestamps and calculator for any numerical translations."));
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), content));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .maxTokens(16000)
                .messages(messages)
                .functions(getFunctionDefinitions())
                .functionCall(ChatCompletionRequest.ChatCompletionRequestFunctionCall.of("auto"))
                .build();

        var response = openAiService.createChatCompletion(request);
        var responseMessage = response.getChoices().get(0).getMessage();

        // Handle function calls if any
        if (responseMessage.getFunctionCall() != null) {
            String result = handleFunctionCall(responseMessage.getFunctionCall());
            messages.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), responseMessage.getContent()));
            messages.add(new ChatMessage(ChatMessageRole.FUNCTION.value(), result));

            request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .build();

            response = openAiService.createChatCompletion(request);
            responseMessage = response.getChoices().get(0).getMessage();
        }

        return responseMessage.getContent();
    }

    private List<Map<String, Object>> getFunctionDefinitions() {
        List<Map<String, Object>> functions = new ArrayList<>();

        // Time function
        Map<String, Object> timeFunction = new HashMap<>();
        timeFunction.put("name", "getCurrentTime");
        timeFunction.put("description", "Get the current time in a specified format");
        Map<String, Object> timeParameters = new HashMap<>();
        timeParameters.put("type", "object");
        timeParameters.put("properties", Map.of(
                "format", Map.of(
                        "type", "string",
                        "description", "The format to return the time in (optional). Default is ISO format."
                )
        ));
        timeFunction.put("parameters", timeParameters);
        functions.add(timeFunction);

        // Calculator function
        Map<String, Object> calcFunction = new HashMap<>();
        calcFunction.put("name", "calculate");
        calcFunction.put("description", "Perform basic arithmetic calculations");
        Map<String, Object> calcParameters = new HashMap<>();
        calcParameters.put("type", "object");
        calcParameters.put("required", List.of("a", "b", "operation"));
        Map<String, Object> calcProperties = new HashMap<>();
        calcProperties.put("a", Map.of(
                "type", "number",
                "description", "First number for calculation"
        ));
        calcProperties.put("b", Map.of(
                "type", "number",
                "description", "Second number for calculation"
        ));
        calcProperties.put("operation", Map.of(
                "type", "string",
                "description", "Operation to perform: add, subtract, multiply, or divide"
        ));
        calcParameters.put("properties", calcProperties);
        calcFunction.put("parameters", calcParameters);
        functions.add(calcFunction);

        return functions;
    }

    private String handleFunctionCall(com.theokanning.openai.completion.chat.ChatFunctionCall functionCall) {
        try {
            return switch (functionCall.getName()) {
                case "getCurrentTime" -> {
                    TimeFunction.TimeFunctionRequest request = objectMapper.readValue(
                            functionCall.getArguments().toString(), TimeFunction.TimeFunctionRequest.class);
                    yield TimeFunction.execute(request);
                }
                case "calculate" -> {
                    CalculatorFunction.CalculatorRequest request = objectMapper.readValue(
                            functionCall.getArguments().toString(), CalculatorFunction.CalculatorRequest.class);
                    yield String.valueOf(CalculatorFunction.execute(request));
                }
                default -> throw new IllegalArgumentException("Unknown function: " + functionCall.getName());
            };
        } catch (Exception e) {
            log.error("Error executing function: " + functionCall.getName(), e);
            return "Error: " + e.getMessage();
        }
    }
}