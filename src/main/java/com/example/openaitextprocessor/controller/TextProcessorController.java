package com.example.openaitextprocessor.controller;

import com.example.openaitextprocessor.dto.ProcessResponse;
import com.example.openaitextprocessor.model.ProcessingHistory;
import com.example.openaitextprocessor.service.OpenAIService;
import com.example.openaitextprocessor.service.ProcessingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class TextProcessorController {

    private final OpenAIService openAIService;
    private final ProcessingHistoryService historyService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("history", historyService.getAllHistory());
        return "index";
    }
    
    @PostMapping("/process")
    @ResponseBody
    public ProcessResponse processText(
            @RequestParam("file") MultipartFile file,
            @RequestParam("operation") String operation,
            @RequestParam(value = "targetLanguage", required = false) String targetLanguage) throws IOException {
        
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String processedContent;
        String operationDescription;
        
        if ("spellcheck".equals(operation)) {
            processedContent = openAIService.fixSpelling(content);
            operationDescription = "Spelling fixed";
        } else if ("translate".equals(operation)) {
            processedContent = openAIService.translate(content, targetLanguage);
            operationDescription = "Translated to " + targetLanguage;
        } else {
            throw new IllegalArgumentException("Invalid operation: " + operation);
        }
        
        // Save to history
        ProcessingHistory history = ProcessingHistory.builder()
                .originalFilename(file.getOriginalFilename())
                .operation(operation)
                .operationDescription(operationDescription)
                .originalContent(content)
                .processedContent(processedContent)
                .build();
        
        historyService.saveHistory(history);
        
        return ProcessResponse.builder()
                .id(history.getId())
                .originalContent(content)
                .processedContent(processedContent)
                .build();
    }
    
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadProcessedFile(@PathVariable("id") Long id) {
        ProcessingHistory history = historyService.getById(id);
        ByteArrayResource resource = new ByteArrayResource(
                history.getProcessedContent().getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"processed_" + history.getOriginalFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .body(resource);
    }


    @GetMapping("/time")
    @ResponseBody
    public ProcessResponse downloadProcessedFile() {
        String string = openAIService.getTime();


        return ProcessResponse.builder()
                .id(0L)
                .originalContent("Hello")
                .processedContent(string)
                .build();
    }

    @GetMapping("/history")
    @ResponseBody
    public List<ProcessingHistory> getHistory() {
        return historyService.getAllHistory();
    }
    
    @DeleteMapping("/history/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteHistoryItem(@PathVariable("id") Long id) {
        historyService.deleteHistory(id);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/history")
    @ResponseBody
    public ResponseEntity<Void> clearHistory() {
        historyService.clearAllHistory();
        return ResponseEntity.ok().build();
    }
}