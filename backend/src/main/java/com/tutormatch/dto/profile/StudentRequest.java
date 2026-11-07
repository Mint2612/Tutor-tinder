package com.tutormatch.dto.profile;

import jakarta.validation.constraints.*;
import java.util.*;

public record StudentRequest(
    @NotBlank @Size(max = 120) String nickname,
    @Min(1) @Max(12) int grade,
    @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 80) String> subjects,
    @NotBlank @Size(max = 100) String province,
    @Pattern(regexp = "ONLINE|IN_PERSON") @NotNull String mode,
    @Min(1) @Max(10000000) int budget,
    @NotEmpty @Size(max = 28)
        List<@NotNull @Pattern(regexp = Slots.PATTERN) String> availability,
    @NotBlank @Size(max = 3000) String needs,
    @NotNull @Size(max = 15) Map<@Size(max = 100) String, @NotNull @Size(max = 500) String> survey,
    boolean consent) {}
