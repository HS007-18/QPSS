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

CREATE TABLE IF NOT EXISTS question_bank_imports (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id  BIGINT NOT NULL,
    session_id  BIGINT NOT NULL,
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);

CREATE TABLE IF NOT EXISTS source_documents (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    import_batch_id    BIGINT NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name   VARCHAR(255) NOT NULL,
    file_extension     VARCHAR(10) NOT NULL,
    content_type       VARCHAR(100),
    file_size          BIGINT NOT NULL,
    checksum           VARCHAR(64) NOT NULL,
    uploaded_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (import_batch_id) REFERENCES question_bank_imports(id)
);

CREATE INDEX IF NOT EXISTS idx_source_document_checksum ON source_documents(checksum);

CREATE TABLE IF NOT EXISTS questions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id          BIGINT NOT NULL,
    session_id          BIGINT NOT NULL,
    source_document_id  BIGINT,
    unit                INT NOT NULL,
    co                  VARCHAR(10) NOT NULL,
    marks               INT NOT NULL,
    serial_no           INT,
    question_content    TEXT NOT NULL,
    raw_ooxml           LONGTEXT,
    source_file_name    VARCHAR(255),
    t                   INT NOT NULL,
    rbt                 VARCHAR(10) NOT NULL DEFAULT 'R',
    marks_split         VARCHAR(20),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (session_id) REFERENCES sessions(id),
    FOREIGN KEY (source_document_id) REFERENCES source_documents(id)
);

CREATE TABLE IF NOT EXISTS exam_section_configs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_type       VARCHAR(20) NOT NULL,
    marks           INT NOT NULL,
    total_required  INT NOT NULL
);

CREATE TABLE IF NOT EXISTS exam_configs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_type       VARCHAR(20) NOT NULL,
    unit            INT NOT NULL,
    marks           INT NOT NULL,
    required_count  INT NOT NULL,
    distribution_pct DECIMAL(5,2),
    t1_pct          DECIMAL(5,2),
    t2_pct          DECIMAL(5,2),
    t1_required_count INT,
    t2_required_count INT
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
    display_rbt     VARCHAR(10),
    FOREIGN KEY (paper_id) REFERENCES generated_papers(id),
    FOREIGN KEY (question_id) REFERENCES questions(id)
);

ALTER TABLE questions ADD COLUMN IF NOT EXISTS rbt VARCHAR(10) NOT NULL DEFAULT 'R';
ALTER TABLE questions ADD COLUMN IF NOT EXISTS marks_split VARCHAR(20);
ALTER TABLE questions ADD COLUMN IF NOT EXISTS question_type VARCHAR(10);
ALTER TABLE questions ADD COLUMN IF NOT EXISTS structured_content LONGTEXT;

ALTER TABLE subjects ADD COLUMN IF NOT EXISTS code VARCHAR(20);
CREATE INDEX IF NOT EXISTS idx_subjects_code ON subjects(code);

ALTER TABLE generated_papers ADD COLUMN IF NOT EXISTS exam_session VARCHAR(100);
ALTER TABLE generated_papers ADD COLUMN IF NOT EXISTS exam_title VARCHAR(255);
ALTER TABLE generated_papers ADD COLUMN IF NOT EXISTS duration VARCHAR(50);
