package com.tutormatch.model;

public record UserCredentials(
    Account account, String passwordHash, String phone, boolean emailVerified) {}
