-- Undo 001_remember_tokens.sql (drops every stored remember-me token; users sign in again).
USE ai_study_hub;
GO
IF OBJECT_ID('dbo.remember_tokens', 'U') IS NOT NULL
BEGIN
    DROP TABLE remember_tokens;
    PRINT 'dropped remember_tokens';
END
GO
