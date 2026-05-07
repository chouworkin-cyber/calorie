CREATE TABLE [dbo].[PointTransactions] (
    [TransactionId] INT            IDENTITY (1, 1) PRIMARY KEY,
    [UserId]        INT            NOT NULL,
    [PointsChanged] INT            NOT NULL,
    [Reason]        NVARCHAR (255) NULL,
    [CreatedAt]     DATETIME2      DEFAULT GETDATE(),
    
    CONSTRAINT [FK_PointTransactions_Users] FOREIGN KEY ([UserId]) REFERENCES [dbo].[Users] ([UserId]) ON DELETE CASCADE
);