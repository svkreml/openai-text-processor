package com.example.openaitextprocessor.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

public class CalculatorFunction {
    @Data
    public static class CalculatorRequest {
        @JsonProperty(required = true)
        @JsonPropertyDescription("First number for calculation")
        private double a;

        @JsonProperty(required = true)
        @JsonPropertyDescription("Second number for calculation")
        private double b;

        @JsonProperty(required = true)
        @JsonPropertyDescription("Operation to perform: add, subtract, multiply, or divide")
        private String operation;
    }

    public static double execute(CalculatorRequest request) {
        return switch (request.getOperation().toLowerCase()) {
            case "add" -> request.getA() + request.getB();
            case "subtract" -> request.getA() - request.getB();
            case "multiply" -> request.getA() * request.getB();
            case "divide" -> {
                if (request.getB() == 0) {
                    throw new IllegalArgumentException("Cannot divide by zero");
                }
                yield request.getA() / request.getB();
            }
            default -> throw new IllegalArgumentException("Unknown operation: " + request.getOperation());
        };
    }
}