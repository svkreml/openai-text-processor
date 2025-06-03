package svkreml.ai.openaitextprocessor.functions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import java.util.regex.PatternSyntaxException;

@Slf4j
@Description("""
        Searches for files in the base directory (and subdirectories) matching a glob pattern.
        Input: Glob pattern (relative to baseDir: ${file.base.dir:-./}).
        Output: List of relative file paths.
        Security: Blocks directory traversal and restricts to base directory.
        Example: '*.txt' → ['file1.txt', 'docs/notes.txt']
        """)
@Component("fileSearcher")
public class FileSearcher implements Function<FileSearcher.SearchPattern, FileSearcher.SearchResult>, AiTool {

    private final Path basePath;

    @Autowired
    public FileSearcher(@Value("${file.base.dir:./}") String baseDir) {
        this.basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        log.info("FileSearcher base directory: {}", basePath);
    }

    @Override
    public SearchResult apply(SearchPattern pattern) {
        String globPattern = pattern.pattern();
        try {
            // Validate pattern syntax early
            if (!isValidPattern(globPattern)) {
                throw new IllegalArgumentException("Invalid pattern syntax: " + globPattern);
            }

            // Convert to normalized system format
            String normalizedPattern = globPattern.replace('/', File.separatorChar);
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + normalizedPattern);
            
            List<String> matches = new ArrayList<>();
            Files.walkFileTree(basePath, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!attrs.isRegularFile()) return FileVisitResult.CONTINUE;
                    
                    Path relative = basePath.relativize(file);
                    if (matcher.matches(relative)) {
                        matches.add(relative.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    log.warn("Access denied to: {}", file, e);
                    return FileVisitResult.CONTINUE;
                }
            });

            log.info("Found {} files matching '{}'", matches.size(), globPattern);
            return new SearchResult(matches, null);
        } catch (InvalidPathException e) {
            String error = "Invalid path in pattern: " + globPattern;
            log.error(error);
            return new SearchResult(null, error);
        } catch (IllegalArgumentException e) {
            log.error("Pattern error: {}", e.getMessage());
            return new SearchResult(null, e.getMessage());
        } catch (NoSuchFileException e) {
            String error = "Base directory not found: " + basePath;
            log.error(error);
            return new SearchResult(null, error);
        } catch (IOException e) {
            String error = "I/O error: " + e.getMessage();
            log.error("Search failed", e);
            return new SearchResult(null, error);
        } catch (SecurityException e) {
            log.error("Security error", e);
            return new SearchResult(null, "Access denied");
        }
    }

    private boolean isValidPattern(String pattern) {
        try {
            FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public record SearchPattern(String pattern) {}
    public record SearchResult(List<String> paths, String error) {}
}