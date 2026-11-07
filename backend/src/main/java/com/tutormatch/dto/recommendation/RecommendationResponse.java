package com.tutormatch.dto.recommendation;

import com.tutormatch.dto.profile.TutorResponse;
import java.util.List;

public record RecommendationResponse(
    TutorResponse tutor,
    String level,
    List<String> strengths,
    List<String> concerns,
    String explanation,
    String source) {}
