ALTER TABLE tech_stack
    MODIFY create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    MODIFY update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE daily_rank
    MODIFY create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    MODIFY update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;


ALTER TABLE job_role
    MODIFY create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    MODIFY update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE notice
    MODIFY create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    MODIFY update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
