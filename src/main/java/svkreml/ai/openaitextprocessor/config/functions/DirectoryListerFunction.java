package svkreml.ai.openaitextprocessor.config.functions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.time.Instant;
import java.util.function.Function;
import java.util.*;
import org.slf4j.*;


@Description("""
Lists directory contents within secured base path.
Input: Relative directory path (empty for root).
Output: DirectoryListing(path, contents[FileInfo(name, type, size, modified)]).
Security: Blocks path traversal.
Types: 'DIR' for directories, 'FILE' for files.
Throws: SecurityException for invalid paths, NotDirectoryException if path not a folder.
Example: ('docs') → lists contents of docs/ directory
""")
@Component("directoryLister")
public class DirectoryListerFunction implements Function<DirectoryListerFunction.InputPath, DirectoryListerFunction.DirectoryListing> {
    private static final Logger log = LoggerFactory.getLogger(DirectoryListerFunction.class);
    private final Path basePath;

    @Autowired
    public DirectoryListerFunction(@Value("${file.base.dir:./}") String baseDir) {
        this.basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        log.info("Secure directory listing base path: {}", this.basePath);
    }

    @Override
    public DirectoryListing apply(InputPath inputPath) {
        String relativePath = inputPath.path();
        try {

            Path resolvedPath = resolveSecurePath(relativePath);
            
            if (!Files.isDirectory(resolvedPath)) {
                throw new NotDirectoryException("Path is not a directory: " + relativePath);
            }

            List<FileInfo> contents = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(resolvedPath)) {
                for (Path path : stream) {
                    contents.add(new FileInfo(
                        path.getFileName().toString(),
                        Files.isDirectory(path) ? "DIR" : "FILE",
                        Files.size(path),
                        Files.getLastModifiedTime(path).toInstant()
                    ));
                }
            }
            
            log.info("Listed {} items in {}", contents.size(), relativePath);
            return new DirectoryListing(
                resolvedPath.toString(),
                contents
            );
        } catch (Exception e) {
            log.error("Directory listing failed: {}", relativePath, e);
            throw new RuntimeException("Listing error: " + e.getMessage(), e);
        }
    }

    private Path resolveSecurePath(String relativePath) {
        Path normalized = basePath.resolve(relativePath).normalize();
        
        if (!normalized.startsWith(basePath)) {
            throw new SecurityException("Path traversal attempt blocked: " + relativePath);
        }
        return normalized;
    }

    // Record definitions
    public record InputPath(String path) {}
    public record FileInfo(String name, String type, long size, Instant modified) {}
    public record DirectoryListing(String path, List<FileInfo> contents) {}
}