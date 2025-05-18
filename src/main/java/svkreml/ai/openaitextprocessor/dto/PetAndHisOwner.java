package svkreml.ai.openaitextprocessor.dto;

import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.lang.NonNull;

public record PetAndHisOwner(
        @ToolParam @NonNull Pet pet,
        @ToolParam @NonNull Owner owner
) {
}