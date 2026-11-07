package com.tutormatch.service;

import com.tutormatch.model.*;
import java.util.Collections;
import org.springframework.stereotype.Component;

@Component
public class EligibilityPolicy {
  public boolean eligible(StudentProfile s, TutorProfile t) {
    return t.workflowState().equals("APPROVED")
        && t.verificationStatus().equals("APPROVED")
        && t.visible()
        && t.hourlyRate() <= s.budget()
        && t.grades().contains(s.grade())
        && t.modes().contains(s.mode())
        && (s.mode().equals("ONLINE") || s.province().equalsIgnoreCase(t.province()))
        && t.subjects().containsAll(s.subjects())
        && !Collections.disjoint(s.availability(), t.availability());
  }

  public int score(StudentProfile s, TutorProfile t) {
    long overlap = s.availability().stream().filter(t.availability()::contains).count();
    return Math.min(15, t.experience())
        + (int) overlap * 3
        + (t.hourlyRate() <= s.budget() * 0.8 ? 3 : 0);
  }
}
