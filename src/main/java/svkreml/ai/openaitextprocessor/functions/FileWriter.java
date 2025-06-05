package svkreml.ai.openaitextprocessor.functions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Description("""
        Handles file writing operations within secured base directory. 
        Input: FileWriteRequest(path, content, [operation=OVERWRITE|INSERT_AT_LINE|REPLACE_AT_LINE|APPEND_TO_THE_END], [line]). 
        Operations:
          - OVERWRITE: Replace entire file (default)
          - INSERT_AT_LINE: Insert content at specified line (1-indexed)
          - REPLACE_AT_LINE: Replace content at specified line (1-indexed)
          - APPEND_TO_THE_END: Append content to the end of the file
        Output: WriteResult(success, message, [newPath]). 
        Security: Blocks path traversal, creates parent directories. 
        Throws: SecurityException for invalid paths, RuntimeException for I/O errors.
        Examples: 
          Overwrite: ('config.yaml', 'key: value') → success
          Insert at line: ('script.py', 'print("debug")', INSERT_AT_LINE, 5) → success
          Replace at line: ('index.html', '<div>New</div>', REPLACE_AT_LINE, 10) → replaces line 10
          Append to end: ('logs.txt', 'New log entry', APPEND_TO_THE_END) → appends content
        """)
@Component("fileWriter")
public class FileWriter implements Function<FileWriter.FileWriteRequest, FileWriter.WriteResult> {
    private final Path basePath;

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
                case REPLACE_AT_LINE -> replaceAtLine(resolvedPath, request);
                case APPEND_TO_THE_END -> appendToEnd(resolvedPath, request);
                default -> overwriteFile(resolvedPath, request);
            };
        } catch (NoSuchFileException e) {
            log.error(e.getMessage(), e);
            return new WriteResult(false, "No such file", request.path());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new WriteResult(false, "Error: " + e.getMessage(), request.path());
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

    private WriteResult appendToEnd(Path path, FileWriteRequest request) throws Exception {
        // Create file if doesn't exist, append content to end
        Files.writeString(path, request.content(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
        log.info("Appended to file: {}", path);
        return new WriteResult(true, "Content appended successfully", path.toString());
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

    private WriteResult replaceAtLine(Path path, FileWriteRequest request) throws Exception {
        if (request.line() == null || request.line() < 1) {
            throw new IllegalArgumentException("Invalid line number: " + request.line());
        }

        List<String> lines = new ArrayList<>();
        if (Files.exists(path)) {
            lines = Files.readAllLines(path);
        } else {
            throw new NoSuchFileException("File not found: " + path);
        }

        int lineIndex = request.line() - 1;
        if (lineIndex >= lines.size()) {
            throw new IndexOutOfBoundsException(
                    "Line number " + request.line() + " exceeds file length (" + lines.size() + " lines)"
            );
        }

        List<String> newLines = Arrays.asList(request.content().split("\\R"));
        lines.set(lineIndex, newLines.get(0));

        if (newLines.size() > 1) {
            lines.addAll(lineIndex + 1, newLines.subList(1, newLines.size()));
        }

        Files.write(path, lines, StandardOpenOption.TRUNCATE_EXISTING);

        log.info("Replaced line {} with {} lines in {}", request.line(), newLines.size(), path);
        return new WriteResult(true,
                "Replaced line " + request.line() + " with " + newLines.size() + " lines",
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

    public enum WriteOperation {
        OVERWRITE,
        INSERT_AT_LINE,
        REPLACE_AT_LINE,
        APPEND_TO_THE_END
    }

    public record FileWriteRequest(
            String path,
            String content,
            WriteOperation operation,  // Defaults to OVERWRITE in constructor
            Integer line               // Required for INSERT_AT_LINE and REPLACE_AT_LINE
    ) {
    }

    public record WriteResult(
            boolean success,
            String message,
            String filePath  // Absolute path of modified file
    ) {
    }
}