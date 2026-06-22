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
    price DECIMAL(10, 2) DEFAULT 0.00,
	total_storage_mb INT NOT NULL DEFAULT 0
);
GO

INSERT INTO subscriptions (tier_name, max_storage_mb, ai_prompt_limit_per_day, price,total_storage_mb ) 
VALUES 
('Guest', 0, 3, 0.00, 0),     -- Very restricted: 0MB storage, 3 AI prompts
('Free', 50, 10, 0.00, 5120),
('Premium', 100, 100, 99000, 51200);
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
    tier_id INT DEFAULT 2, 
    balance INT DEFAULT 0, 
	ai_prompts_today INT DEFAULT 0,
	last_prompt_reset DATETIME DEFAULT GETDATE(),
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
    parent_folder_id INT DEFAULT NULL, -- ADDED: Enables folder hierarchy (child folders)
    folder_name NVARCHAR(100) NOT NULL,
    sharing_permission NVARCHAR(20) DEFAULT 'PRIVATE' CHECK (sharing_permission IN ('PRIVATE', 'FRIENDS_ONLY', 'PUBLIC')), -- ADDED: Folder level permissions
    created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (parent_folder_id) REFERENCES folders(folder_id) -- Self-referencing foreign key
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
    file_extension NVARCHAR(10) NOT NULL, -- ADDED: Tracks file extension (e.g., 'pdf', 'docx')
    cloud_storage_url NVARCHAR(500) NOT NULL, -- AWS or similar storage link
    file_size_mb DECIMAL(5,2) NOT NULL,
    ai_parsing_status NVARCHAR(20) DEFAULT 'PENDING' CHECK (ai_parsing_status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED')), 
    sharing_permission NVARCHAR(20) DEFAULT 'PRIVATE' CHECK (sharing_permission IN ('PRIVATE', 'FRIENDS_ONLY', 'PUBLIC')), -- Permission management
    share_link_token NVARCHAR(100) UNIQUE,    -- For sharing via links
    is_flagged BIT DEFAULT 0,                 -- Content moderation (BIT: 0=False, 1=True)
    created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 DEFAULT CURRENT_TIMESTAMP, -- ADDED: Tracks the modification day
    FOREIGN KEY (user_id) REFERENCES users(user_id), -- Cascade removed to prevent multiple path errors
    FOREIGN KEY (folder_id) REFERENCES folders(folder_id) -- Set Null removed to prevent multiple path errors
);
GO

-- Trigger to handle automated updated_at timestamps for documents
CREATE TRIGGER trg_documents_updated_at
ON documents
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE documents
    SET updated_at = CURRENT_TIMESTAMP
    WHERE document_id IN (SELECT DISTINCT document_id FROM Inserted);
END;
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
	session_name NVARCHAR(255) NOT NULL,
    user_id INT NOT NULL,
    created_at DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    is_pinned BIT DEFAULT 0,   
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
GO

CREATE TABLE chat_messages (
    message_id INT IDENTITY(1,1) PRIMARY KEY,
    session_id INT NOT NULL,
    sender NVARCHAR(10) NOT NULL CHECK (sender IN ('USER', 'BOT')),
    message_content NVARCHAR(MAX) NOT NULL,
	display BIT DEFAULT 1,   
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

CREATE TABLE document_extracted_text (
    extraction_id  INT IDENTITY(1,1) PRIMARY KEY,
    document_id    INT NOT NULL UNIQUE,
    extracted_text NVARCHAR(MAX) NOT NULL,
    created_at     DATETIME2 DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE
);
GO
