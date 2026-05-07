CREATE TABLE [dbo].[UserFoodLogs] (
    [LogId]      INT            IDENTITY (1, 1) PRIMARY KEY,
    [UserId]     INT            NOT NULL,
    [FoodId]     INT            NOT NULL,
    [MealType]   NVARCHAR (20)  NULL, -- Breakfast, Lunch, Dinner, Snack
    [Quantity]   FLOAT          DEFAULT 1,
    [ConsumedAt] DATETIME2      DEFAULT GETDATE(),

    CONSTRAINT [FK_UserFoodLogs_Users] FOREIGN KEY ([UserId]) REFERENCES [dbo].[Users] ([UserId]) ON DELETE CASCADE,
    CONSTRAINT [FK_UserFoodLogs_Foods] FOREIGN KEY ([FoodId]) REFERENCES [dbo].[Foods] ([FoodId])
);