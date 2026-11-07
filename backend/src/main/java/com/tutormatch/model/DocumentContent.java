package com.tutormatch.model;

/**
 * Evidence file. {@code content} is filled for rows written before object storage existed;
 * newer rows carry a {@code storageKey} and the service loads the bytes from the store.
 */
public record DocumentContent(
    String id,
    String tutorId,
    String filename,
    String contentType,
    byte[] content,
    String storageKey) {}
