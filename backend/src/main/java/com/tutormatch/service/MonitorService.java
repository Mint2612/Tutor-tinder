package com.tutormatch.service;

import com.tutormatch.dto.admin.MonitorDtos.*;
import com.tutormatch.repository.MonitorRepository;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitorService {
  static final ZoneId LOCAL = ZoneId.of("Asia/Ho_Chi_Minh");
  static final int DAYS = 7;
  private final MonitorRepository repository;
  private final boolean aiConfigured;

  public MonitorService(
      MonitorRepository repository, @Value("${GEMINI_API_KEY:}") String geminiKey) {
    this.repository = repository;
    this.aiConfigured = !geminiKey.isBlank();
  }

  @Transactional(readOnly = true)
  public Snapshot snapshot() {
    var roles = repository.accountsByRole();
    var tutors = repository.tutorsByState();
    var pairs = repository.pairsByStatus();
    var trials = repository.trialsByStatus();
    var runtime = ManagementFactory.getRuntimeMXBean();
    return new Snapshot(
        Instant.now(),
        new Accounts(
            roles.getOrDefault("PARENT", 0L),
            roles.getOrDefault("TUTOR", 0L),
            roles.getOrDefault("ADMIN", 0L),
            repository.newAccountsSince(7),
            repository.activeSessions()),
        new Tutors(
            tutors.getOrDefault("APPROVED", 0L),
            tutors.getOrDefault("PENDING_REVIEW", 0L),
            tutors.getOrDefault("DRAFT", 0L),
            tutors.getOrDefault("REJECTED", 0L),
            repository.hiddenApprovedTutors(),
            repository.pausedApprovedTutors(),
            repository.oldestPending()),
        new Students(repository.students(), repository.hiddenStudents()),
        new Matching(
            repository.waitingPairs(),
            pairs.getOrDefault("MATCHED", 0L),
            repository.awaitingConfirmation(),
            repository.connectedPairs(),
            repository.confirmationExpired(),
            pairs.getOrDefault("SKIPPED", 0L),
            pairs.getOrDefault("EXPIRED", 0L)),
        new Trials(
            trials.getOrDefault("PENDING", 0L),
            trials.getOrDefault("CONFIRMED", 0L),
            trials.getOrDefault("CANCELLED", 0L),
            trials.getOrDefault("EXPIRED", 0L),
            repository.reviews(),
            repository.averageRating()),
        repository.unreadNotifications(),
        activity(),
        repository.waitingTutors(5).stream()
            .map(w -> new WaitingTutor(w.id(), w.displayName(), w.submittedAt()))
            .toList(),
        new Server(
            Instant.ofEpochMilli(runtime.getStartTime()),
            runtime.getUptime() / 1000,
            System.getProperty("java.version"),
            repository.ping(),
            aiConfigured));
  }

  /** The last seven local days, oldest first, with zeros for quiet days. */
  private List<Day> activity() {
    var accounts = repository.perDay("users", "created_at", "deleted_at IS NULL", DAYS);
    var oks = repository.perDay("matching_pairs", "first_liked_at", null, DAYS);
    var matches = repository.perDay("matching_pairs", "matched_at", null, DAYS);
    var trials = repository.perDay("trial_schedules", "created_at", null, DAYS);
    var today = LocalDate.now(LOCAL);
    var days = new ArrayList<Day>();
    for (int i = DAYS - 1; i >= 0; i--) {
      var day = today.minusDays(i);
      days.add(
          new Day(
              day,
              accounts.getOrDefault(day, 0L),
              oks.getOrDefault(day, 0L),
              matches.getOrDefault(day, 0L),
              trials.getOrDefault(day, 0L)));
    }
    return days;
  }
}
