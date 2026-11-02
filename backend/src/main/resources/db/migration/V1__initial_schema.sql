CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    role VARCHAR(16) NOT NULL CHECK (role IN ('PARENT', 'TUTOR', 'ADMIN')),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE tutor_profiles (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    province VARCHAR(100) NOT NULL,
    hourly_rate INT NOT NULL CHECK (hourly_rate >= 0),
    teaching_description TEXT NOT NULL,
    verification_status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (verification_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    availability JSON NOT NULL,
    survey JSON NOT NULL,
    embedding JSON NULL,
    embedding_model VARCHAR(120) NULL,
    verified_at TIMESTAMP(6) NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_tutor_visibility (verification_status, province)
);
CREATE TABLE tutor_subjects (
    tutor_id VARCHAR(36) NOT NULL,
    subject VARCHAR(80) NOT NULL,
    PRIMARY KEY (tutor_id, subject),
    FOREIGN KEY (tutor_id) REFERENCES tutor_profiles(id) ON DELETE CASCADE
);
CREATE TABLE tutor_grades (
    tutor_id VARCHAR(36) NOT NULL,
    grade INT NOT NULL CHECK (grade BETWEEN 1 AND 12),
    PRIMARY KEY (tutor_id, grade),
    FOREIGN KEY (tutor_id) REFERENCES tutor_profiles(id) ON DELETE CASCADE
);
CREATE TABLE tutor_modes (
    tutor_id VARCHAR(36) NOT NULL,
    mode VARCHAR(16) NOT NULL CHECK (mode IN ('ONLINE', 'IN_PERSON')),
    PRIMARY KEY (tutor_id, mode),
    FOREIGN KEY (tutor_id) REFERENCES tutor_profiles(id) ON DELETE CASCADE
);

CREATE TABLE student_profiles (
    id VARCHAR(36) PRIMARY KEY,
    parent_id VARCHAR(36) NOT NULL,
    nickname VARCHAR(120) NOT NULL,
    grade INT NOT NULL CHECK (grade BETWEEN 1 AND 12),
    province VARCHAR(100) NOT NULL,
    mode VARCHAR(16) NOT NULL CHECK (mode IN ('ONLINE', 'IN_PERSON')),
    max_hourly_rate INT NOT NULL CHECK (max_hourly_rate >= 0),
    availability JSON NOT NULL,
    survey JSON NOT NULL,
    needs TEXT NOT NULL,
    consent_at TIMESTAMP(6) NOT NULL,
    embedding JSON NULL,
    embedding_model VARCHAR(120) NULL,
    FOREIGN KEY (parent_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_student_parent (parent_id)
);
CREATE TABLE student_subjects (
    student_id VARCHAR(36) NOT NULL,
    subject VARCHAR(80) NOT NULL,
    PRIMARY KEY (student_id, subject),
    FOREIGN KEY (student_id) REFERENCES student_profiles(id) ON DELETE CASCADE
);
CREATE TABLE trial_requests (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    tutor_id VARCHAR(36) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'COMPLETED')),
    message TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (student_id) REFERENCES student_profiles(id) ON DELETE CASCADE,
    FOREIGN KEY (tutor_id) REFERENCES tutor_profiles(id) ON DELETE CASCADE,
    INDEX idx_trial_student (student_id, status),
    INDEX idx_trial_tutor (tutor_id, status)
);
CREATE TABLE reviews (
    id VARCHAR(36) PRIMARY KEY,
    trial_request_id VARCHAR(36) NOT NULL UNIQUE,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (trial_request_id) REFERENCES trial_requests(id) ON DELETE CASCADE
);
