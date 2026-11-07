package com.tutormatch.model;

/** Authenticated identity. Never contains credentials or contact phone numbers. */
public record Account(String id, String email, String name, String role) {}
