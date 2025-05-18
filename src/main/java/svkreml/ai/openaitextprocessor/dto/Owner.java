package svkreml.ai.openaitextprocessor.dto;

import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.lang.NonNull;

public record Owner(@NonNull @ToolParam(description = "name in latin letters") String name) {
}
