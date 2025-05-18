package com.example.openaitextprocessor.service;

import com.example.openaitextprocessor.model.ProcessingHistory;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProcessingHistoryService {

    private final Map<Long, ProcessingHistory> historyMap = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    
    public ProcessingHistory saveHistory(ProcessingHistory history) {
        Long id = idCounter.getAndIncrement();
        history.setId(id);
        history.setProcessedAt(LocalDateTime.now());
        historyMap.put(id, history);
        return history;
    }
    
    public List<ProcessingHistory> getAllHistory() {
        return new ArrayList<>(historyMap.values());
    }
    
    public ProcessingHistory getById(Long id) {
        ProcessingHistory history = historyMap.get(id);
        if (history == null) {
            throw new RuntimeException("History with id " + id + " not found");
        }
        return history;
    }
    
    public void deleteHistory(Long id) {
        historyMap.remove(id);
    }
    
    public void clearAllHistory() {
        historyMap.clear();
    }
}