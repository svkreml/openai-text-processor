package svkreml.ai.openaitextprocessor.dto;

import org.springframework.lang.NonNull;

public record PetAndHisOwner(
        @NonNull Pet pet,
        @NonNull Owner owner
) {
}