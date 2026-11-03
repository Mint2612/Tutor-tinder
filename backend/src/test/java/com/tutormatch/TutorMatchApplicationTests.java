package com.tutormatch;

import com.tutormatch.config.DemoDataSeeder;
import com.tutormatch.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("demo")
class TutorMatchApplicationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired DemoDataSeeder seeder;

    @Test
    void migrationsJpaAndDemoSeedWorkTogether() {
        assertTrue(users.findByEmail("tutor1@example.test").isPresent());
        Long before = jdbc.queryForObject("SELECT COUNT(*) FROM tutor_profiles", Long.class);
        seeder.run(new DefaultApplicationArguments());
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM tutor_profiles", Long.class));
        assertEquals(20L, jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email LIKE 'tutor%@example.test'", Long.class));
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM tutor_profiles t JOIN users u ON u.id=t.user_id WHERE t.verification_status = 'APPROVED' AND u.email LIKE 'tutor%@example.test'", Long.class));
    }
}
