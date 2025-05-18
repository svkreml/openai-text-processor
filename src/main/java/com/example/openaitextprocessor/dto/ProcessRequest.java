package com.example.openaitextprocessor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRequest {

    @NotBlank(message = "Content is required")
    @Size(max = 100000, message = "Content must be less than 100,000 characters")
    private String content;
    
    @NotBlank(message = "Operation is required")
    private String operation; // "spellcheck" or "translate"
    
    private String targetLanguage; // required for translation
}