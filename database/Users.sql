CREATE TABLE [dbo].[Users] (
    [UserId]       INT            IDENTITY (1, 1) PRIMARY KEY,
    [Username]     NVARCHAR (50)  NOT NULL UNIQUE,
    [Email]        NVARCHAR (100) NOT NULL UNIQUE,
    [PasswordHash] NVARCHAR (MAX) NOT NULL,
    [TotalPoints]  INT            DEFAULT 0,
    [CreatedAt]    DATETIME2      DEFAULT GETDATE(),
    [LastLogin]    DATETIME2      NULL
);