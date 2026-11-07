package com.tutormatch.dto.auth;

/** The signed-in user's own account details; the email is read-only. */
public record AccountProfile(String id, String email, String name, String role, String phone) {}
