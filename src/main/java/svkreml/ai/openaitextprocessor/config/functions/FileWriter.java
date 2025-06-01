package svkreml.ai.openaitextprocessor.config.functions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Description("""
Handles file writing operations within secured base directory. 
Input: FileWriteRequest(path, content, [operation=OVERWRITE|INSERT_AT_LINE], [line]). 
Operations:
  - OVERWRITE: Replace entire file (default)
  - INSERT_AT_LINE: Insert content at specified line (1-indexed)
Output: WriteResult(success, message, [newPath]). 
Security: Blocks path traversal, creates parent directories. 
Throws: SecurityException for invalid paths, RuntimeException for I/O errors.
Examples: 
  Overwrite: ('config.yaml', 'key: value') → success
  Insert at line: ('script.py', 'print("debug")', INSERT_AT_LINE, 5) → success
""")

@Component("fileWriter")
public class FileWriter implements Function<FileWriter.FileWriteRequest, FileWriter.WriteResult> {
    private final Path basePath;

    public enum WriteOperation {
        OVERWRITE,
        INSERT_AT_LINE
    }

    @Autowired
    public FileWriter(@Value("${file.base.dir:./}") String baseDir) {
        this.basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        log.info("FileWriter base path: {}", basePath);
    }

    @Override
    public WriteResult apply(FileWriteRequest request) {
        try {
            Path resolvedPath = resolveSecurePath(request.path());
            Files.createDirectories(resolvedPath.getParent());

            return switch (request.operation()) {
                case INSERT_AT_LINE -> insertAtLine(resolvedPath, request);
                default -> overwriteFile(resolvedPath, request);
            };
        } catch (Exception e) {
            return new WriteResult(false, "Error: " + e.getMessage(), null);
        }
    }

    private WriteResult overwriteFile(Path path, FileWriteRequest request) throws Exception {
        Files.writeString(path, request.content(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
        log.info("Overwritten file: {}", path);
        return new WriteResult(true, "File overwritten successfully", path.toString());
    }

    private WriteResult insertAtLine(Path path, FileWriteRequest request) throws Exception {
        if (request.line() == null || request.line() < 1) {
            throw new IllegalArgumentException("Invalid line number: " + request.line());
        }

        List<String> lines = new ArrayList<>();
        if (Files.exists(path)) {
            lines = Files.readAllLines(path);
        }

        int insertPosition = Math.min(request.line() - 1, lines.size());
        List<String> newContent = Arrays.asList(request.content().split("\\R"));

        lines.addAll(insertPosition, newContent);
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        log.info("Inserted {} lines at position {} in {}", newContent.size(), insertPosition + 1, path);
        return new WriteResult(true,
                "Inserted " + newContent.size() + " lines at position " + (insertPosition + 1),
                path.toString()
        );
    }

    private Path resolveSecurePath(String relativePath) {
        Path normalized = basePath.resolve(relativePath).normalize();
        if (!normalized.startsWith(basePath)) {
            throw new SecurityException("Path traversal blocked: " + relativePath);
        }
        return normalized;
    }

    public record FileWriteRequest(
            String path,
            String content,
            WriteOperation operation,  // Defaults to OVERWRITE in constructor
            Integer line               // Required for INSERT_AT_LINE
    ){}

    public record WriteResult(
            boolean success,
            String message,
            String filePath  // Absolute path of modified file
    ) {}
}