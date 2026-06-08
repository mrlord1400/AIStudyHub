-- ==========================================
-- Database Schema for AI Study Hub
-- Author: Long
-- Target: Microsoft SQL Server
-- ==========================================

-- Check if database exists, create if it doesn't
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'ai_study_hub')
BEGIN
    CREATE DATABASE ai_study_hub;
END
GO

USE ai_study_hub;
GO

-- -----------------------------------------------------
-- 1. SUBSCRIPTIONS & TIERS
-- Handles upgrade logic and rate limits
-- -----------------------------------------------------
CREATE TABLE subscriptions (
    tier_id INT IDENTITY(1,1) PRIMARY KEY,
    tier_name NVARCHAR(50) NOT NULL UNIQUE, -- e.g., 'Free', 'Premium'
    max_storage_mb INT NOT NULL,            -- Storage quota
    ai_prompt_limit_per_day INT NOT NULL,   -- Rate limiting for API costs
    price DECIMAL(10, 2) DEFAULT 0.00
);
GO

INSERT INTO subscriptions (tier_name, max_storage_mb, ai_prompt_limit_per_day, price) 
VALUES 
('Guest', 50, 3, 0.00),     -- Very restricted: 50MB storage, 3 AI prompts
('Free', 500, 10, 0.00),
('Premium', 5000, 100, 9.99);
GO

-- -----------------------------------------------------
-- 2. USERS
-- Core account management
-- -----------------------------------------------------
CREATE TABLE users (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL,
    email NVARCHAR(100) UNIQUE NULL,
    password_hash NVARCHAR(255) NULL,   
    role NVARCHAR(10) DEFAULT 'STUDENT' CHECK (role IN ('GUEST', 'STUDENT', 'ADMIN')), 
    tier_id INT DEFAULT 1, 
    balance INT DEFAULT 0, 
    status NVARCHAR(10) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'BANNED')),
    expires_at DATETIME2 NULL,     
    created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tier_id) REFERENCES subscriptions(tier_id)
);
GO

-- Trigger to replicate MySQL's ON UPDATE CURRENT_TIMESTAMP for the users table
CREATE TRIGGER trg_users_updated_at
ON users
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE users
    SET updated_at = CURRENT_TIMESTAMP
    WHERE user_id IN (SELECT DISTINCT user_id FROM Inserted);
END;
GO

-- -----------------------------------------------------
-- 3. FOLDERS / CATEGORIES
-- For document organization
-- -----------------------------------------------------
CREATE TABLE folders (
    folder_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    folder_name NVARCHAR(100) NOT NULL,
    created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
GO

-- -----------------------------------------------------
-- 4. DOCUMENTS
-- Metadata and cloud storage references
-- -----------------------------------------------------
CREATE TABLE documents (
    document_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    folder_id INT DEFAULT NULL,
    title NVARCHAR(255) NOT NULL,
    cloud_storage_url NVARCHAR(500) NOT NULL, -- AWS or similar storage link
    file_size_mb DECIMAL(5,2) NOT NULL,
    ai_parsing_status NVARCHAR(20) DEFAULT 'PENDING' CHECK (ai_parsing_status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED')), 
    sharing_permission NVARCHAR(20) DEFAULT 'PRIVATE' CHECK (sharing_permission IN ('PRIVATE', 'FRIENDS_ONLY', 'PUBLIC')), -- Permission management
    share_link_token NVARCHAR(100) UNIQUE,    -- For sharing via links
    is_flagged BIT DEFAULT 0,                 -- Content moderation (BIT: 0=False, 1=True)
    created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id), -- Cascade removed to prevent multiple path errors
    FOREIGN KEY (folder_id) REFERENCES folders(folder_id) -- Set Null removed to prevent multiple path errors
);
GO

-- -----------------------------------------------------
-- 5. BOOKMARKS
-- Tracks recent access and pinned materials
-- -----------------------------------------------------
CREATE TABLE bookmarks (
    bookmark_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    document_id INT NOT NULL,
    created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id), -- Cascade removed to prevent multiple path errors
    FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE,
    CONSTRAINT UQ_user_document UNIQUE(user_id, document_id) -- Prevents duplicate bookmarks
);
GO

-- -----------------------------------------------------
-- 6. AI CHAT HISTORY
-- Logs Q&A against document context
-- -----------------------------------------------------
CREATE TABLE chat_sessions (
    session_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    document_id INT NOT NULL,
    created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id), -- Cascade removed to prevent multiple path errors
    FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE
);
GO

CREATE TABLE chat_messages (
    message_id INT IDENTITY(1,1) PRIMARY KEY,
    session_id INT NOT NULL,
    sender NVARCHAR(10) NOT NULL CHECK (sender IN ('USER', 'BOT')),
    message_content NVARCHAR(MAX) NOT NULL,
    created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE
);
GO

-- -----------------------------------------------------
-- 7. TRANSACTIONS
-- Handles user deposits, withdrawals, and payments
-- -----------------------------------------------------
CREATE TABLE transactions (
    transaction_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL, -- Recommended over FLOAT for financial exactness
    type NVARCHAR(20) NOT NULL CHECK (type IN ('DEPOSIT', 'WITHDRAW')),
    status NVARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'CANCELLED')),
    started_at DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME2 NULL,
    
    -- Link to the existing users table
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
GO
