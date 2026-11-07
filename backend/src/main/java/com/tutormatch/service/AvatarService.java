package com.tutormatch.service;

import com.tutormatch.integration.storage.FileStorage;
import com.tutormatch.model.TutorProfile;
import com.tutormatch.repository.TutorRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tutor profile photo: stored in object storage, referenced by an API path in photo_url. */
@Service
public class AvatarService {
  public static final String URL_PREFIX = "/api/media/avatars/";
  private final TutorService tutors;
  private final TutorRepository repository;
  private final FileStorage storage;

  public AvatarService(TutorService tutors, TutorRepository repository, FileStorage storage) {
    this.tutors = tutors;
    this.repository = repository;
    this.storage = storage;
  }

  @Transactional
  public TutorProfile replace(String user, byte[] upload) {
    var tutor = tutors.lockOwn(user);
    byte[] jpeg = AvatarImages.normalize(upload);
    String name = UUID.randomUUID() + ".jpg";
    storage.put(FileStorage.AVATARS, name, jpeg, "image/jpeg");
    try {
      repository.setPhoto(tutor.id(), URL_PREFIX + name);
    } catch (RuntimeException e) {
      storage.delete(FileStorage.AVATARS, name);
      throw e;
    }
    discard(tutor.photoUrl());
    return tutors.own(user);
  }

  @Transactional
  public TutorProfile remove(String user) {
    var tutor = tutors.lockOwn(user);
    if (tutor.photoUrl() != null && !tutor.photoUrl().isBlank()) {
      repository.setPhoto(tutor.id(), "");
      discard(tutor.photoUrl());
    }
    return tutors.own(user);
  }

  /** Deletes the replaced object after commit. Legacy external URLs have nothing to delete. */
  private void discard(String photoUrl) {
    if (photoUrl == null || !photoUrl.startsWith(URL_PREFIX)) return;
    String name = photoUrl.substring(URL_PREFIX.length());
    AfterCommit.run(() -> storage.delete(FileStorage.AVATARS, name));
  }
}
