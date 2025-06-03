package svkreml.ai.openaitextprocessor.config.functions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextSearchTest {
    private TextSearch textSearch;
    private Path testDir;

    public static void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    System.out.println(file.toAbsolutePath());
                    Files.delete(file); // Удаляем файл
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    System.out.println(dir.toAbsolutePath());
                    Files.delete(dir); // Удаляем директорию после всех файлов
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            System.out.println("Директория не существует.");
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        testDir = Files.createTempDirectory("unit-tests");
        Files.createDirectories(testDir);

        textSearch = new TextSearch();
    }

    @AfterEach
    void tearDown() throws IOException {
        deleteDirectory(testDir);
    }

    @Test
    void testBasicSearch() throws IOException {
        // Создаем тестовые файлы
        Path file1 = testDir.resolve("file1.txt");
        Files.write(file1, List.of(
                "Hello world",
                "This is a test",
                "Search me: pattern"
        ), StandardCharsets.UTF_8);

        Path file2 = testDir.resolve("file2.log");
        Files.write(file2, List.of(
                "Another file",
                "With pattern inside"
        ), StandardCharsets.UTF_8);

        // Выполняем поиск
        TextSearch.SearchRequest request = new TextSearch.SearchRequest(
                testDir.toString(),
                "pattern",
                10,
                0,
                0,
                "*"
        );

        TextSearch.SearchResponse response = textSearch.apply(request);

        // Проверяем результаты
        assertTrue(response.success());
        assertNull(response.error());
        assertEquals(2, response.results().size());

        // Проверяем первый результат
        TextSearch.SearchResult result1 = response.results().get(0);
        assertEquals(file1.toString(), result1.filePath());
        assertEquals("Search me: pattern", result1.matchedLine());
        assertEquals(3, result1.lineNumber());

        // Проверяем второй результат
        TextSearch.SearchResult result2 = response.results().get(1);
        assertEquals(file2.toString(), result2.filePath());
        assertEquals("With pattern inside", result2.matchedLine());
        assertEquals(2, result2.lineNumber());
    }

    @Test
    void testFileMaskFilter() throws IOException {
        // Создаем тестовые файлы разных типов
        Path javaFile = testDir.resolve("Main.java");
        Files.write(javaFile, List.of(
                "public class Main {",
                "  // TODO: implement",
                "  System.out.println(\"pattern\");"
        ));

        Path textFile = testDir.resolve("notes.txt");
        Files.write(textFile, List.of("Find pattern here"));

        Path logFile = testDir.resolve("app.log");
        Files.write(logFile, List.of("ERROR: pattern found"));

        // Ищем только в Java файлах
        TextSearch.SearchRequest request = new TextSearch.SearchRequest(
                testDir.toString(),
                "pattern",
                10,
                0,
                0,
                "*.java"
        );

        TextSearch.SearchResponse response = textSearch.apply(request);

        // Проверяем результаты
        assertTrue(response.success());
        assertEquals(1, response.results().size());
        assertEquals(javaFile.toString(), response.results().get(0).filePath());
    }

    @Test
    void testContextLines() throws IOException {
        // Создаем тестовый файл
        Path file = testDir.resolve("context.txt");
        Files.write(file, List.of(
                "Line 1",
                "Line 2",
                "MATCH: pattern",
                "Line 4",
                "Line 5"
        ));

        // Запрашиваем контекст: 1 строка до и 2 после
        TextSearch.SearchRequest request = new TextSearch.SearchRequest(
                testDir.toString(),
                "pattern",
                10,
                1,
                2,
                "*"
        );

        TextSearch.SearchResponse response = textSearch.apply(request);

        // Проверяем результаты
        assertTrue(response.success());
        assertEquals(1, response.results().size());

        TextSearch.SearchResult result = response.results().get(0);
        assertEquals(3, result.lineNumber());

        // Проверяем контекст
        List<String> expectedContext = List.of(
                "Line 2",
                "MATCH: pattern",
                "Line 4",
                "Line 5"
        );
        assertEquals(expectedContext, result.contextLines());
        assertEquals(1, result.matchIndexInContext()); // Индекс совпадения в контексте
    }

    @Test
    void testMaxResults() throws IOException {
        // Создаем несколько файлов с совпадениями
        for (int i = 1; i <= 5; i++) {
            Path file = testDir.resolve("file" + i + ".txt");
            Files.write(file, List.of("pattern " + i));
        }

        // Ограничиваем результаты 3 совпадениями
        TextSearch.SearchRequest request = new TextSearch.SearchRequest(
                testDir.toString(),
                "pattern",
                3,
                0,
                0,
                "*"
        );

        TextSearch.SearchResponse response = textSearch.apply(request);

        // Проверяем результаты
        assertTrue(response.success());
        assertEquals(3, response.results().size());
    }

    @Test
    void testLargeFileSkipped() throws IOException {
        // Создаем файл больше 50KB
        Path largeFile = testDir.resolve("large.log");
        StringBuilder content = new StringBuilder();
        while (content.length() < 60000) { // 60KB > 50KB
            content.append("This is a line of text. ");
        }
        Files.write(largeFile, List.of(content.toString()));

        // Создаем маленький файл
        Path smallFile = testDir.resolve("small.txt");
        Files.write(smallFile, List.of("pattern"));

        // Выполняем поиск
        TextSearch.SearchRequest request = new TextSearch.SearchRequest(
                testDir.toString(),
                "pattern",
                10,
                0,
                0,
                "*"
        );

        TextSearch.SearchResponse response = textSearch.apply(request);

        // Проверяем что найден только маленький файл
        assertTrue(response.success());
        assertEquals(1, response.results().size());
        assertEquals(smallFile.toString(), response.results().get(0).filePath());
    }

    @Test
    void testInvalidDirectory() {
        // Пытаемся искать в несуществующей директории
        TextSearch.SearchRequest request = new TextSearch.SearchRequest(
                "/invalid/path",
                "pattern",
                10,
                0,
                0,
                "*"
        );

        TextSearch.SearchResponse response = textSearch.apply(request);

        // Проверяем ошибку
        assertFalse(response.success());
        assertNotNull(response.error());
        assertTrue(response.error().contains("Not a directory") ||
                   response.error().contains("Invalid directory path"));
        assertNull(response.results());
    }

    @Test
    void testInvalidRegex() throws IOException {
        // Создаем тестовый файл
        Path file = testDir.resolve("test.txt");
        Files.write(file, List.of("content"));

        // Используем невалидное регулярное выражение
        TextSearch.SearchRequest request = new TextSearch.SearchRequest(
                testDir.toString(),
                "[invalid-regex",
                10,
                0,
                0,
                "*"
        );

        TextSearch.SearchResponse response = textSearch.apply(request);

        // Проверяем ошибку
        assertFalse(response.success());
        assertNotNull(response.error());
        assertTrue(response.error().contains("Invalid regex pattern"));
        assertNull(response.results());
    }

    @Test
    void testEdgeCases() throws IOException {
        // Проверяем граничные условия
        Path file = testDir.resolve("edges.txt");
        Files.write(file, List.of(
                "first line",
                "pattern at start",
                "middle pattern",
                "pattern at end"
        ));

        // Тест 1: Контекст в начале файла
        TextSearch.SearchRequest request1 = new TextSearch.SearchRequest(
                testDir.toString(),
                "pattern at start",
                1,
                1,
                0,
                "*"
        );

        TextSearch.SearchResponse response1 = textSearch.apply(request1);
        List<String> context1 = response1.results().get(0).contextLines();
        assertEquals(List.of("first line", "pattern at start"), context1);

        // Тест 2: Контекст в конце файла
        TextSearch.SearchRequest request2 = new TextSearch.SearchRequest(
                testDir.toString(),
                "pattern at end",
                1,
                0,
                1,
                "*"
        );

        TextSearch.SearchResponse response2 = textSearch.apply(request2);
        List<String> context2 = response2.results().get(0).contextLines();
        assertEquals(List.of("pattern at end"), context2); // Нет следующей строки

        // Тест 3: Отрицательный контекст (должен корректироваться до 0)
        TextSearch.SearchRequest request3 = new TextSearch.SearchRequest(
                testDir.toString(),
                "pattern",
                1,
                -1,
                -1,
                "*"
        );

        TextSearch.SearchResponse response3 = textSearch.apply(request3);
        assertEquals(1, response3.results().get(0).contextLines().size());
    }
}