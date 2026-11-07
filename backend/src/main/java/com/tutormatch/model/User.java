package com.tutormatch.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
  @Id
  @Column(length = 36)
  private String id;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "full_name", nullable = false, length = 120)
  private String fullName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Role role;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected User() {}

  public User(String email, String fullName, Role role) {
    this.id = UUID.randomUUID().toString();
    this.email = email;
    this.fullName = fullName;
    this.role = role;
    this.createdAt = Instant.now();
  }

  public String getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getFullName() {
    return fullName;
  }

  public Role getRole() {
    return role;
  }
}
