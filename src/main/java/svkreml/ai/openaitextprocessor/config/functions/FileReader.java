package svkreml.ai.openaitextprocessor.config.functions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;


@Slf4j
@Description("""
        Reads text file content within secured base directory. 
        Input: Relative path (baseDir: ${file.base.dir:-./}). 
        Output: File content as String. 
        Security: Blocks path traversal attempts. 
        Throws: SecurityException for invalid paths, RuntimeException for I/O errors.
        Example: 'docs/notes.txt' → file content
        """)
@Component("fileReader")
public class FileReader implements Function<FileReader.InputPath, FileReader.Content> {
    private final Path basePath;

    @Autowired
    public FileReader(@Value("${file.base.dir:./}") String baseDir) {
        this.basePath = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    @Override
    public Content apply(InputPath inputPath) {
        String relativePath = inputPath.path();
        try {
            Path resolvedPath = resolveSecurePath(relativePath);
            log.info("Resolved path: {}", resolvedPath);
            return new Content(Files.readString(resolvedPath), null);
        } catch (Exception e) {
            return new Content(null, "%s: %s".formatted(e.getClass(), e.getMessage()));
        }
    }

    private Path resolveSecurePath(String relativePath) {
        Path normalized = basePath.resolve(relativePath).normalize();

        if (!normalized.startsWith(basePath)) {
            throw new SecurityException("Attempted directory traversal: " + relativePath);
        }
        return normalized;
    }

    public record InputPath(String path) {}
    public record Content(String text, String error) {}
}