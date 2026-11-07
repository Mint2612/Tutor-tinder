package com.tutormatch.config;

import com.tutormatch.repository.DemoDataRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit fictional fixtures; never enabled in the default app profile. */
@Component
@Profile("demo")
public class DemoDataSeeder implements ApplicationRunner {
  private final DemoDataRepository fixtures;

  public DemoDataSeeder(DemoDataRepository fixtures) {
    this.fixtures = fixtures;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    fixtures.seed();
  }
}
