package com.example.openaitextprocessor.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeFunction {
    @Data
    public static class TimeFunctionRequest {
        @JsonProperty(required = true)
        @JsonPropertyDescription("The format to return the time in (optional). Default is ISO format.")
        private String format;
    }

    public static String execute(TimeFunctionRequest request) {
        String format = request.getFormat();
        if (format == null || format.isEmpty()) {
            return LocalDateTime.now().toString();
        }
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(format));
    }
}