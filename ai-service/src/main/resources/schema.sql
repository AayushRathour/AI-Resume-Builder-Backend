ALTER TABLE ai_requests
    MODIFY request_type VARCHAR(32) NOT NULL;

ALTER TABLE ai_requests
    MODIFY status VARCHAR(32) NOT NULL;