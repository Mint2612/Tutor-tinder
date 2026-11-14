package com.tutormatch.dto.admin;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Read-only operational snapshot for administrators. Counts only: no names of families or contacts. */
public final class MonitorDtos {
  private MonitorDtos() {}

  public record Accounts(long parents, long tutors, long admins, long newLast7Days, long activeSessions) {}

  public record Tutors(long approved, long pendingReview, long draft, long rejected, long hidden, long paused,
      Instant oldestPendingSince) {}

  public record Students(long total, long hidden) {}

  /** Funnel of the two-way matching flow. {@code waiting} = one side said OK, the other has not answered. */
  public record Matching(long waiting, long matched, long awaitingConfirmation, long connected,
      long confirmationExpired, long skipped, long expired) {}

  public record Trials(long pending, long confirmed, long cancelled, long expired, long reviews, Double averageRating) {}

  public record Day(LocalDate day, long newAccounts, long oks, long matches, long trials) {}

  public record WaitingTutor(String id, String displayName, Instant submittedAt) {}

  public record Server(Instant startedAt, long uptimeSeconds, String javaVersion, boolean database,
      boolean aiConfigured) {}

  public record Snapshot(Instant generatedAt, Accounts accounts, Tutors tutors, Students students, Matching matching,
      Trials trials, long unreadNotifications, List<Day> activity, List<WaitingTutor> waitingTutors, Server server) {}
}
