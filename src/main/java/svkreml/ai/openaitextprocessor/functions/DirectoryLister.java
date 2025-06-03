package svkreml.ai.openaitextprocessor.functions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Description("""
        Lists directory contents or full file tree within secured base path.
        Input: 
          path: Relative directory path (empty for root)
          recursive: true for full tree, false for flat listing (default)
        Output: 
          DirectoryListing(path, contents[FileInfo(name, type, size, modified, children[])])
          Children present only in recursive mode
        Security: Blocks path traversal.
        Types: 'DIR' for directories, 'FILE' for files, 'SYMLINK' for symbolic links.
        Throws: SecurityException for invalid paths, NotDirectoryException if path not a folder.
        Examples: 
          Flat: ('docs', false) → immediate children of docs/
          Tree: ('src', true) → full recursive tree of src/
        """)
@Component("directoryLister")
public class DirectoryLister implements Function<DirectoryLister.InputParams, DirectoryLister.DirectoryListing>, AiTool {
    private static final Logger log = LoggerFactory.getLogger(DirectoryLister.class);
    private final Path basePath;
    private final int maxDepth;

    @Autowired
    public DirectoryLister(
            @Value("${file.base.dir:./}") String baseDir,
            @Value("${file.tree.max-depth:10}") int maxDepth
    ) {
        this.basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        this.maxDepth = maxDepth;
        log.info("Secure directory listing base path: {}", this.basePath);
    }

    @Tool
    @Override
    public DirectoryListing apply(InputParams input) {
        try {
            Path resolvedPath = resolveSecurePath(input.path());
            BasicFileAttributes attrs = Files.readAttributes(
                    resolvedPath,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS
            );

            if (!attrs.isDirectory()) {
                throw new NotDirectoryException("Path is not a directory: " + input.path());
            }

            List<FileSystemNode> contents = new ArrayList<>();
            if (input.recursive()) {
                // Recursive tree traversal
                contents = buildTree(resolvedPath, 0);
            } else {
                // Flat directory listing
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(resolvedPath)) {
                    for (Path path : stream) {
                        contents.add(createNode(path));
                    }
                }
            }

            log.info("Listed {} items in {} (recursive: {})",
                    contents.size(), input.path(), input.recursive());

            return new DirectoryListing(
                    resolvedPath.toString(),
                    contents,
                    null
            );
        } catch (NoSuchFileException e) {
            log.error("Directory listing failed: {}", input.path(), e);
            return new DirectoryListing(null, null, "No such file");
        } catch (Exception e) {
            log.error("Directory listing failed: {}", input.path(), e);
            return new DirectoryListing(null, null, "%s: %s".formatted(e.getClass(), e.getMessage()));
        }
    }

    private List<FileSystemNode> buildTree(Path dir, int currentDepth) throws IOException {
        if (currentDepth > maxDepth) {
            log.warn("Max depth reached at {}", dir);
            return Collections.emptyList();
        }

        List<FileSystemNode> nodes = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                FileSystemNode node = createNode(path);
                if (node != null) {
                    if ("DIR".equals(node.type()) && currentDepth < maxDepth) {
                        List<FileSystemNode> children = buildTree(path, currentDepth + 1);
                        node = new FileSystemNode(
                                node.name(),
                                node.type(),
                                node.size(),
                                node.modified(),
                                children
                        );
                    }
                    nodes.add(node);
                }
            }
        }
        return nodes;
    }

    private FileSystemNode createNode(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(
                    path,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS
            );

            String type = "FILE";
            if (attrs.isDirectory()) type = "DIR";
            else if (attrs.isSymbolicLink()) type = "SYMLINK";

            return new FileSystemNode(
                    path.getFileName().toString(),
                    type,
                    attrs.size(),
                    attrs.lastModifiedTime().toString(),
                    Collections.emptyList()  // Children populated later in recursion
            );
        } catch (IOException e) {
            log.warn("Skipping inaccessible path: {}", path, e);
            return null;
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
    public record InputParams(String path, boolean recursive) {
        public InputParams(String path) {
            this(path, false);
        }
    }

    public record FileSystemNode(
            String name,
            String type,  // DIR, FILE, SYMLINK
            long size,
            String modified,
            List<FileSystemNode> children  // Populated in recursive mode
    ) {
    }

    public record DirectoryListing(
            String path,
            List<FileSystemNode> contents,
            String error
    ) {
    }
}