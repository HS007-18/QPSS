DELETE FROM exam_configs;
DELETE FROM exam_section_configs;
DELETE FROM exam_co_rules;

INSERT INTO exam_section_configs (exam_type, marks, total_required) VALUES
('INTERNAL_1', 2, 10),
('INTERNAL_1', 16, 10),
('INTERNAL_2', 2, 10),
('INTERNAL_2', 16, 10),
('SEMESTER', 2, 10),
('SEMESTER', 16, 10);

INSERT INTO exam_configs (exam_type, unit, marks, required_count, distribution_pct, t1_pct, t2_pct, t1_required_count, t2_required_count) VALUES
('INTERNAL_1', 1, 2,  4, 40.00, 50.00, 50.00, 2, 2),
('INTERNAL_1', 2, 2,  4, 40.00, 50.00, 50.00, 2, 2),
('INTERNAL_1', 3, 2,  2, 20.00, 100.00, 0.00, 2, 0),
('INTERNAL_1', 1, 16, 4, 40.00, 50.00, 50.00, 2, 2),
('INTERNAL_1', 2, 16, 4, 40.00, 50.00, 50.00, 2, 2),
('INTERNAL_1', 3, 16, 2, 20.00, 100.00, 0.00, 2, 0),

('INTERNAL_2', 3, 2,  2, 20.00, 0.00, 100.00, 0, 2),
('INTERNAL_2', 4, 2,  4, 40.00, 50.00, 50.00, 2, 2),
('INTERNAL_2', 5, 2,  4, 40.00, 50.00, 50.00, 2, 2),
('INTERNAL_2', 3, 16, 2, 20.00, 0.00, 100.00, 0, 2),
('INTERNAL_2', 4, 16, 4, 40.00, 50.00, 50.00, 2, 2),
('INTERNAL_2', 5, 16, 4, 40.00, 50.00, 50.00, 2, 2),

('SEMESTER', 1, 2,  2, 20.00, 50.00, 50.00, 1, 1),
('SEMESTER', 2, 2,  2, 20.00, 50.00, 50.00, 1, 1),
('SEMESTER', 3, 2,  2, 20.00, 50.00, 50.00, 1, 1),
('SEMESTER', 4, 2,  2, 20.00, 50.00, 50.00, 1, 1),
('SEMESTER', 5, 2,  2, 20.00, 50.00, 50.00, 1, 1),
('SEMESTER', 1, 16, 2, 20.00, 50.00, 50.00, 1, 1),
('SEMESTER', 2, 16, 2, 20.00, 50.00, 50.00, 1, 1),
('SEMESTER', 3, 16, 2, 20.00, 50.00, 50.00, 1, 1),
('SEMESTER', 4, 16, 2, 20.00, 50.00, 50.00, 1, 1),
('SEMESTER', 5, 16, 2, 20.00, 50.00, 50.00, 1, 1);

INSERT INTO exam_co_rules (exam_type, co) VALUES
('INTERNAL_1', 'CO1'),
('INTERNAL_1', 'CO2'),
('INTERNAL_1', 'CO3'),
('INTERNAL_2', 'CO3'),
('INTERNAL_2', 'CO4'),
('INTERNAL_2', 'CO5'),
('SEMESTER', 'CO1'),
('SEMESTER', 'CO2'),
('SEMESTER', 'CO3'),
('SEMESTER', 'CO4'),
('SEMESTER', 'CO5');
