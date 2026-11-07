package com.tutormatch.service;

import com.tutormatch.model.*;
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
        && Schedule.coveredSlots(s.availability(), t.availability()) > 0;
  }

  public int score(StudentProfile s, TutorProfile t) {
    // Each slot the family wants that the tutor really covers counts as before (3 points).
    int covered = Schedule.coveredSlots(s.availability(), t.availability());
    return Math.min(15, t.experience())
        + covered * 3
        + (t.hourlyRate() <= s.budget() * 0.8 ? 3 : 0);
  }
}
