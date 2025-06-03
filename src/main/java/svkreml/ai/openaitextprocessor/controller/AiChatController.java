package svkreml.ai.openaitextprocessor.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@Slf4j
@RestController
@RequestMapping("/file")
public class AiChatController {

    private final ChatClient fileClient;

    public AiChatController(@Qualifier("fileClient") ChatClient fileClient) {
        this.fileClient = fileClient;
    }



    @PostMapping(value = "/chatSSE", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatSSE(@RequestBody String query) {
        log.info("Received query: {}", query);
        return getEventFlux(query, this::getSentEvent);
    }

    @PostMapping(
            value = "/chat",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE // Чистый текст вместо SSE
    )
    public Flux<String> chat(@RequestBody String query) {
        log.info("Received query: {}", query);
        return getEventFlux(query, c -> c);
    }

    @PostMapping(
            value = "/chatSync",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String chatSync(@RequestBody String query) {
        log.info("Received query: {}", query);
        return fileClient.prompt().user(u -> u.text(query)).call().content();
    }

    private <T> Flux<T> getEventFlux(String query, Function<String, T> mapper) {
        return fileClient.prompt()
                .user(u -> u.text(query))
                .stream()        // Получаем StreamResponseSpec
                .content()       // Берем Flux<String> с готовым контентом
                .map(mapper)
                .doOnError(e -> log.error("Stream error: {}", e.getMessage()))
                .doOnCancel(() -> log.warn("Stream cancelled by client"))
                .doOnComplete(() -> log.info("Stream completed successfully"));
    }

    private ServerSentEvent<String> getSentEvent(String content) {
        return ServerSentEvent.builder(content
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\r", "\\r")
        ).build();
    }
}