package com.tutormatch.model;

public record DocumentContent(
    String id, String tutorId, String filename, String contentType, byte[] content) {}
