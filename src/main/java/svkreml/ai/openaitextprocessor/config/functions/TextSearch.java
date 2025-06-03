package svkreml.ai.openaitextprocessor.config.functions;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Description("""
Performs full-text search in files within a directory using DFS. 
Input: SearchRequest(directory, regex, maxResults=10, contextBefore=0, contextAfter=0, fileMask="*"). 
Output: SearchResponse(success, results, error). 
Searches files <50KB matching file mask recursively. Returns partial results with errors logged.
Examples: 
  Search in '/docs' for 'error.*' with context → returns first 10 matches with surrounding lines
""")
@Component("textSearch")
public class TextSearch implements Function<TextSearch.SearchRequest, TextSearch.SearchResponse> , AiTool {

    private static final long MAX_FILE_SIZE = 50 * 1024; // 50KB

    @Override
    public SearchResponse apply(SearchRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Starting search: directory={}, regex={}, maxResults={}, contextBefore={}, contextAfter={}, fileMask={}",
                request.directory(), request.regex(), request.maxResults(), 
                request.contextBefore(), request.contextAfter(), request.fileMask());
        
        int maxResults = request.maxResults() > 0 ? request.maxResults() : 10;
        int contextBefore = Math.max(0, request.contextBefore());
        int contextAfter = Math.max(0, request.contextAfter());
        String fileMask = request.fileMask() != null ? request.fileMask() : "*";
        
        Path baseDir;
        try {
            baseDir = Paths.get(request.directory()).toAbsolutePath().normalize();
            if (!Files.isDirectory(baseDir)) {
                String error = "Not a directory: " + baseDir;
                log.error(error);
                return new SearchResponse(false, null, error);
            }
            log.info("Base directory resolved: {}", baseDir);
        } catch (InvalidPathException e) {
            String error = "Invalid directory path: " + request.directory();
            log.error(error, e);
            return new SearchResponse(false, null, error);
        }

        Pattern pattern;
        try {
            pattern = Pattern.compile(request.regex());
            log.info("Regex pattern compiled successfully");
        } catch (PatternSyntaxException e) {
            String error = "Invalid regex pattern: " + e.getMessage();
            log.error(error, e);
            return new SearchResponse(false, null, error);
        }

        List<SearchResult> results = new ArrayList<>();
        int filesProcessed = 0;
        int filesMatched = 0;
        int filesSkipped = 0;
        
        try (Stream<Path> paths = Files.walk(baseDir)) {
            Iterator<Path> iterator = paths.iterator();
            while (iterator.hasNext() && results.size() < maxResults) {
                Path path = iterator.next();
                
                if (Files.isDirectory(path)) {
                    log.debug("Processing directory: {}", path);
                    continue;
                }
                
                filesProcessed++;
                try {
                    if (isFileEligible(path, fileMask)) {
                        log.debug("Processing file: {}", path.getFileName());
                        int resultsBefore = results.size();
                        processFile(path, pattern, maxResults, contextBefore, contextAfter, results);
                        
                        if (results.size() > resultsBefore) {
                            filesMatched++;
                            log.info("Found {} matches in file: {}", results.size() - resultsBefore, path);
                        }
                    } else {
                        filesSkipped++;
                        log.debug("Skipped file: {}", path.getFileName());
                    }
                } catch (Exception e) {
                    filesSkipped++;
                    log.warn("Error processing file {}: {}", path, e.getMessage());
                }
            }
        } catch (IOException | SecurityException e) {
            log.error("Directory traversal failed: {}", e.getMessage(), e);
            return new SearchResponse(false, null, "Directory traversal error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            return new SearchResponse(false, null, "Unexpected error: " + e.getMessage());
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("Search completed: files={} processed, {} matched, {} skipped, matches={}, duration={}ms",
                filesProcessed, filesMatched, filesSkipped, results.size(), duration);
        
        return new SearchResponse(true, results, null);
    }

    private boolean isFileEligible(Path path, String fileMask) throws IOException {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        
        // Check file mask
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + fileMask);
        if (!matcher.matches(path.getFileName())) {
            log.trace("File skipped by mask: {}", path);
            return false;
        }
        
        // Check file size
        long size = Files.size(path);
        if (size > MAX_FILE_SIZE) {
            log.debug("File skipped by size ({} > {} bytes): {}", size, MAX_FILE_SIZE, path);
            return false;
        }
        
        return true;
    }

    private void processFile(Path file, Pattern pattern, int maxResults,
                             int contextBefore, int contextAfter,
                             List<SearchResult> results) {
        if (results.size() >= maxResults) {
            return;
        }
        
        try {
            List<String> allLines = Files.readAllLines(file, StandardCharsets.UTF_8);
            log.debug("Reading file: {} ({} lines)", file.getFileName(), allLines.size());
            
            int matchesInFile = 0;
            for (int i = 0; i < allLines.size() && results.size() < maxResults; i++) {
                String line = allLines.get(i);
                if (pattern.matcher(line).find()) {
                    int startLine = Math.max(0, i - contextBefore);
                    int endLine = Math.min(allLines.size(), i + contextAfter + 1);
                    
                    List<String> context = allLines.subList(startLine, endLine);
                    int matchIndex = i - startLine;
                    
                    results.add(new SearchResult(
                        file.toString(),
                        line,
                        i + 1,
                        new ArrayList<>(context),
                        matchIndex
                    ));
                    matchesInFile++;
                    
                    log.trace("Match found: {}:{} - {}", file, i+1, line);
                }
            }
            
            if (matchesInFile > 0) {
                log.debug("Found {} matches in file: {}", matchesInFile, file);
            }
        } catch (IOException | SecurityException | OutOfMemoryError e) {
            log.warn("Error processing file {}: {}", file, e.getMessage());
        }
    }

    public record SearchRequest(
        String directory,
        String regex,
        int maxResults,
        int contextBefore,
        int contextAfter,
        String fileMask
    ) {
        public SearchRequest {
            if (maxResults <= 0) maxResults = 10;
            if (contextBefore < 0) contextBefore = 0;
            if (contextAfter < 0) contextAfter = 0;
            if (fileMask == null || fileMask.isBlank()) fileMask = "*";
        }
    }

    public record SearchResult(
        String filePath,
        String matchedLine,
        int lineNumber,
        List<String> contextLines,
        int matchIndexInContext
    ) {}

    public record SearchResponse(
        boolean success,
        List<SearchResult> results,
        String error
    ) {}
}