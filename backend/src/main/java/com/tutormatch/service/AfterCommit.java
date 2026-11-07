package com.tutormatch.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Runs side effects on external systems only once the database change has really committed. */
final class AfterCommit {
  private AfterCommit() {}

  static void run(Runnable action) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      action.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            action.run();
          }
        });
  }
}
