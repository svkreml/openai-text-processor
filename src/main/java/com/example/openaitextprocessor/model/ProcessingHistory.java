package com.example.openaitextprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingHistory {
    private Long id;
    private String originalFilename;
    private String operation;
    private String operationDescription;
    private String originalContent;
    private String processedContent;
    private LocalDateTime processedAt;
}