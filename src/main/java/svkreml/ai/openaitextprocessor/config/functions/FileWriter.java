package svkreml.ai.openaitextprocessor.config.functions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.function.BiFunction;
import java.util.function.Function;


@Description("""
        Writes text content to file within secured base directory. 
        Input: (1) Relative path, (2) Content string. 
        Output: true on success. 
        Security: Blocks path traversal, creates parent directories automatically. 
        Behavior: Overwrites existing files. 
        Throws: SecurityException for invalid paths, RuntimeException for I/O errors.
        Example: ('logs/app.log', 'New log entry') → true
        """)
@Component("fileWriter")
public class FileWriter implements Function<FileWriter.FileToWrite, FileWriter.Result> {
    private final Path basePath;

    @Autowired
    public FileWriter(@Value("${file.base.dir:./}") String baseDir) {
        this.basePath = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    @Override
    public Result apply(FileToWrite fileToWrite) {
        try {
            Path resolvedPath = resolveSecurePath(fileToWrite.path());
            Files.createDirectories(resolvedPath.getParent());

            Files.writeString(
                    resolvedPath,
                    fileToWrite.content(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            return new Result(true);
        } catch (Exception e) {
            throw new RuntimeException("File write error: " + e.getMessage(), e);
        }
    }

    private Path resolveSecurePath(String relativePath) {
        Path normalized = basePath.resolve(relativePath).normalize();

        if (!normalized.startsWith(basePath)) {
            throw new SecurityException("Attempted directory traversal: " + relativePath);
        }
        return normalized;
    }

    public record FileToWrite(String path, String content) {}
    public record Result(Boolean result) {}

}
