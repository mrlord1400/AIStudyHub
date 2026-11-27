-- T3.4: revocable remember-me tokens (selector/validator pattern).
-- Idempotent: safe to run twice. Undo with 001_remember_tokens.rollback.sql.
-- The cookie holds "<selector>.<validator>"; only SHA-256(validator) is stored, so a database leak cannot be replayed.
USE ai_study_hub;
GO

IF OBJECT_ID('dbo.remember_tokens', 'U') IS NULL
BEGIN
    CREATE TABLE remember_tokens (
        token_id INT IDENTITY(1,1) PRIMARY KEY,
        user_id INT NOT NULL,
        selector VARCHAR(32) NOT NULL,
        validator_hash CHAR(64) NOT NULL,
        expires_at DATETIME2 NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP,
        last_used_at DATETIME2 NULL,
        CONSTRAINT UQ_remember_tokens_selector UNIQUE (selector),
        CONSTRAINT FK_remember_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
    );
    CREATE INDEX IX_remember_tokens_user ON remember_tokens(user_id);
    PRINT 'created remember_tokens';
END
ELSE
    PRINT 'remember_tokens already exists';
GO

-- Every long-lived cookie issued before this migration (stateless, 10 years, unrevocable) stops working at deploy time:
-- the code no longer reads it. Users sign in again once.
SELECT COUNT(*) AS remember_tokens_rows FROM remember_tokens;
GO
