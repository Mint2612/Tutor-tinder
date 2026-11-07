package com.tutormatch.controller;

import com.tutormatch.exception.BusinessException;
import com.tutormatch.integration.storage.FileStorage;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Serves stored images to signed-in users; the storage server itself is never exposed. */
@RestController
@RequestMapping("/api/media")
public class MediaController {
  private static final Pattern AVATAR =
      Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg");
  private final FileStorage storage;

  public MediaController(FileStorage storage) {
    this.storage = storage;
  }

  @GetMapping("/avatars/{name}")
  public ResponseEntity<byte[]> avatar(@PathVariable String name) {
    if (!AVATAR.matcher(name).matches()) throw new BusinessException(404, "Không tìm thấy ảnh");
    var file =
        storage
            .get(FileStorage.AVATARS, name)
            .orElseThrow(() -> new BusinessException(404, "Không tìm thấy ảnh"));
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_JPEG)
        // The name is a random UUID per upload, so a given URL never changes content.
        .header("Cache-Control", "private, max-age=31536000, immutable")
        .header("X-Content-Type-Options", "nosniff")
        .body(file.content());
  }
}
