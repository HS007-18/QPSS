CREATE TABLE IF NOT EXISTS subjects (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sessions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id  BIGINT NOT NULL,
    status      VARCHAR(20) DEFAULT 'ACTIVE',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at   TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id)
);

CREATE TABLE IF NOT EXISTS questions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id          BIGINT NOT NULL,
    session_id          BIGINT NOT NULL,
    unit                INT NOT NULL,
    co                  VARCHAR(10) NOT NULL,
    marks               INT NOT NULL,
    serial_no           INT,
    question_content    TEXT NOT NULL,
    source_file_name    VARCHAR(255),
    source_page_number  INT,
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);

CREATE TABLE IF NOT EXISTS exam_configs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_type       VARCHAR(20) NOT NULL,
    unit            INT NOT NULL,
    marks           INT NOT NULL,
    required_count  INT NOT NULL,
    distribution_pct DECIMAL(5,2)
);

CREATE TABLE IF NOT EXISTS exam_co_rules (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_type   VARCHAR(20) NOT NULL,
    co          VARCHAR(10) NOT NULL
);

CREATE TABLE IF NOT EXISTS generated_papers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id      BIGINT NOT NULL,
    subject_id      BIGINT NOT NULL,
    exam_type       VARCHAR(20) NOT NULL,
    set_label       VARCHAR(10),
    generation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_final        BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (session_id) REFERENCES sessions(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id)
);

CREATE TABLE IF NOT EXISTS paper_questions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    paper_id        BIGINT NOT NULL,
    question_id     BIGINT NOT NULL,
    section         VARCHAR(10) NOT NULL,
    question_number INT NOT NULL,
    choice_label    VARCHAR(5),
    pair_index      INT,
    FOREIGN KEY (paper_id) REFERENCES generated_papers(id),
    FOREIGN KEY (question_id) REFERENCES questions(id)
);
