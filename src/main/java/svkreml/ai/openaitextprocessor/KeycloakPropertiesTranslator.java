/*
package svkreml.ai.openaitextprocessor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import svkreml.ai.openaitextprocessor.utils.BatchSplitter;
import svkreml.ai.openaitextprocessor.utils.OrderedProperties;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
@SpringBootApplication
public class KeycloakPropertiesTranslator implements ApplicationRunner {





//        public static final String enPropsPath = "/home/svkreml/IdeaProjects/dit/dit-id/keycloak/js/apps/account-ui/maven-resources/theme/keycloak.v3/account/messages/messages_en.properties";
//        public static final String ruPropsPath = "/home/svkreml/IdeaProjects/dit/dit-id/keycloak/js/apps/account-ui/maven-resources-community/theme/keycloak.v3/account/messages/messages_ru.properties";
//


    public static final String enPropsPath = "/home/svkreml/IdeaProjects/dit/dit-id/keycloak/js/apps/admin-ui/maven-resources/theme/keycloak.v2/admin/messages/messages_en.properties";
    public static final String ruPropsPath = "messages_ru_lib.properties";

    public static final String ruResultPropsPath = "messages_ru_new.properties";
    public static final String dictPropsPath = "messages_ru_dict.properties";
    private final ChatClient translateChatClient;

    public KeycloakPropertiesTranslator(@Qualifier("translate") ChatClient translateChatClient) {
        this.translateChatClient = translateChatClient;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        OrderedProperties enProperties = getProperties(enPropsPath);
        OrderedProperties ruProperties = getProperties(ruPropsPath);
        //  Properties dictProperties = getProperties(dictPropsPath);
        OrderedProperties resultProperties = getProperties(ruResultPropsPath);



        //resultProperties.putAll(enProperties);
        //resultProperties.store(Files.newOutputStream(Path.of(ruResultPropsPath)), "");

        doTranslate(enProperties, ruProperties, resultProperties);
        System.exit(0);
    }


    public static void main(String[] args) {
        SpringApplication.run(KeycloakPropertiesTranslator.class, args);
    }

    private static Map<String, String> toMap(Properties enProperties) {
        return enProperties.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().toString(),
                e -> e.getValue().toString(),
                (existing, replacement) -> existing
        ));
    }

    private static Map<String, String> getNotExistMap(Map<String, String> bigMap, Map<String, String> smallMap) {
        Map<String, String> hashMap = new HashMap<>();
        for (Map.Entry<String, String> entry : bigMap.entrySet()) {
            if (!smallMap.containsKey(entry.getKey())) {
                hashMap.put(entry.getKey(), entry.getValue());
            }
        }
        return hashMap;
    }

    private static OrderedProperties getProperties(String path) throws IOException {

        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }

        OrderedProperties properties = new OrderedProperties();
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    public static boolean haveWrongBlocks(String s1, String s2) {
        Pattern pattern = Pattern.compile("\\$\\{([^}]*)\\}");
        return !getBlockValues(s1, pattern).equals(getBlockValues(s2, pattern));
    }

    private static Set<String> getBlockValues(String s, Pattern pattern) {
        Set<String> blocks = new HashSet<>();
        Matcher matcher = pattern.matcher(s);
        while (matcher.find()) blocks.add(matcher.group(1));
        return blocks;
    }

    public static boolean containsChinese(String s) {
        return s != null && s.chars().anyMatch(c -> c >= 0x4E00 && c <= 0x9FFF);
    }

    public static boolean containsKorean(String input) {
        return input.matches(".*[\\uAC00-\\uD7AF\\u1100-\\u11FF\\u3130-\\u318F\\uA960-\\uA97F\\uD7B0-\\uD7FF\\u4E00-\\u9FFF].*");
    }
    public static boolean containsJapanese(String input) {
        return input.matches(".*[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uD840-\\uD87F\\uDC00-\\uDFFF].*");
    }

    public static boolean doNotContainsRussian(String s) {
        s = removeBlocks(s).trim();
        return s != null && s.contains(" ") && s.chars().noneMatch(c -> c >= 0x0400 && c <= 0x04FF);
    }

    public static String removeBlocks(String input) {
        if (input == null) return null;
        return input.replaceAll("\\{\\{.*?}}", "");
    }

    public static boolean containsNewLines(String s) {
        if (s == null) return false;
        return s.chars().anyMatch(c -> c == '\n' || c == '\r');
    }


    private void doTranslate(Properties enProperties, Properties ruProperties, Properties resultProperties) throws IOException {
        for (List<Map.Entry<Object, Object>> splitIntoBatch : BatchSplitter.splitIntoBatches(enProperties.entrySet(), 20)) {
            List<String> keys = splitIntoBatch.stream().map(Map.Entry::getKey).map(String::valueOf).toList();

            Map<String, String> toTranslate = new HashMap<>();

            for (String key : keys) {
                if (!ruProperties.containsKey(key)) {
                    toTranslate.put(key, enProperties.get(key).toString());
                }
            }
            Map<String, String> translated = new HashMap<>();


            for (int i = 0; i < 10; i++) {
                try {
                    translated.putAll(translate(toTranslate));
                } catch (Exception e) {
                    continue;
                }
                boolean repeat = false;
                for (Map.Entry<String, String> entry : translated.entrySet()) {
                    String key = entry.getKey();
                    String translatedValue = entry.getValue();
                    if (
                            StringUtils.isAllBlank(translatedValue)
                            || haveWrongBlocks(translatedValue, enProperties.get(key).toString())
                            || containsChinese(translatedValue)
                            || containsKorean(translatedValue)
                            || containsJapanese(translatedValue)
                            || containsNewLines(translatedValue)
                                    //   || doNotContainsRussian(translatedValue)
                            || "null".equals(translatedValue)
                    ) {
                        log.warn("ERROR \t\t\t{}={}", key, translatedValue);
                        repeat = true;
                    }
                }
                if(!repeat) {
                    break;
                }
            }

            for (String key : keys) {
                if (ruProperties.containsKey(key)) {
                    log.info("FROM PROPS \t\t{}={}",key,  ruProperties.get(key));
                    resultProperties.put(key, ruProperties.get(key));
                } else {
                    log.info("TRANSLATE \t\t{}={}",key, translated.get(key));
                    resultProperties.put(key, translated.get(key));
                }
            }


            resultProperties.store(Files.newOutputStream(Path.of(ruResultPropsPath)), "");
        }
    }

    private Map<String, String> translate(Map<String, String> toTranslate) {
        String[] split = Objects.requireNonNull(this.translateChatClient.prompt()
                .user(u -> u.text(String.join("%%%", toTranslate.values())))
                .call()
                .content(), "translateChatClient вернул null").split("%%%");

        if (split.length != toTranslate.size()) {
            throw new RuntimeException("Переводчик вернул неверное количество переводов");
        }
        Map<String, String> translated = new HashMap<>();
        int i = 0;
        for (String key : toTranslate.keySet()) {
            translated.put(key, split[i++].trim());
        }
        return translated;
    }

}*/
