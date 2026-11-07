package com.tutormatch.model;

public record AuthChallenge(String id, String userId, String purpose, String secretHash) {}
