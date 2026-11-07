package com.tutormatch.dto.profile;

import jakarta.validation.constraints.*;
import java.util.*;

public record TutorRequest(
    // Legal full name as on the identity documents. The public display name is the account's name
    // (set at registration, changed on the account) and cannot be set through the profile.
    @NotBlank @Size(max = 120) String fullName,
    @Size(max = 500) @NotNull String photoUrl,
    @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 80) String> subjects,
    @NotEmpty @Size(max = 12) List<@NotNull @Min(1) @Max(12) Integer> grades,
    @NotEmpty @Size(max = 2) List<@NotNull @Pattern(regexp = "ONLINE|IN_PERSON") String> modes,
    @NotBlank @Size(max = 100) String province,
    @Min(1) @Max(10000000) int hourlyRate,
    @NotBlank @Size(max = 3000) String methodology,
    @NotBlank @Size(max = 500) String style,
    @NotBlank @Size(max = 3000) String background,
    @Min(0) @Max(60) int experience,
    @NotEmpty @Size(max = 28)
        List<@NotNull @Pattern(regexp = Slots.PATTERN) String> availability,
    @Size(max = 1000) String achievements) {}
