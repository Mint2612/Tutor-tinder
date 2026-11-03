package com.tutormatch.platform;

import static com.tutormatch.platform.ApiErrors.require;

import jakarta.validation.constraints.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

public final class ProfileData {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  public static final Set<String> SUBJECTS =
      Set.of("Toán", "Ngữ văn", "Tiếng Anh", "Vật lý", "Hóa học", "Sinh học", "Tin học");

  private ProfileData() {}

  public record Student(
      @NotBlank @Size(max = 120) String nickname,
      @Min(1) @Max(12) int grade,
      @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 80) String> subjects,
      @NotBlank @Size(max = 100) String province,
      @Pattern(regexp = "ONLINE|IN_PERSON") @NotNull String mode,
      @Min(1) @Max(10000000) int budget,
      @NotEmpty @Size(max = 28)
          List<@Pattern(regexp = "[1-7]_(AM|PM|EVENING)") String> availability,
      @NotBlank @Size(max = 3000) String needs,
      @NotNull @Size(max = 15) Map<@Size(max = 100) String, @Size(max = 500) String> survey,
      boolean consent) {}

  public record Tutor(
      @NotBlank @Size(max = 120) String displayName,
      @Size(max = 500) @NotNull String photoUrl,
      @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 80) String> subjects,
      @NotEmpty @Size(max = 12) List<@Min(1) @Max(12) Integer> grades,
      @NotEmpty @Size(max = 2) List<@Pattern(regexp = "ONLINE|IN_PERSON") String> modes,
      @NotBlank @Size(max = 100) String province,
      @Min(1) @Max(10000000) int hourlyRate,
      @NotBlank @Size(max = 3000) String methodology,
      @NotBlank @Size(max = 500) String style,
      @NotBlank @Size(max = 3000) String background,
      @Min(0) @Max(60) int experience,
      @NotEmpty @Size(max = 28)
          List<@Pattern(regexp = "[1-7]_(AM|PM|EVENING)") String> availability) {}

  public static String json(Object value) {
    return JSON.writeValueAsString(value);
  }

  public static Object parse(Object value) {
    return JSON.readValue(value.toString(), Object.class);
  }

  @SuppressWarnings("unchecked")
  public static List<String> strings(Object json) {
    return (List<String>) parse(json);
  }

  public static void distinct(Collection<?> values) {
    require(new HashSet<>(values).size() == values.size(), 400, "Không được lặp lại lựa chọn");
  }
}
