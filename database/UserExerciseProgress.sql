CREATE TABLE [dbo].[UserExerciseProgress] (
    [ProgressId]  INT            IDENTITY (1, 1) PRIMARY KEY,
    [UserId]      INT            NOT NULL,
    [PlanId]      INT            NOT NULL,
    [Status]      NVARCHAR (20)  DEFAULT 'In Progress', -- In Progress, Completed, Cancelled
    [StartDate]   DATETIME2      DEFAULT GETDATE(),
    [EndDate]     DATETIME2      NULL,

    CONSTRAINT [FK_UserExerciseProgress_Users] FOREIGN KEY ([UserId]) REFERENCES [dbo].[Users] ([UserId]) ON DELETE CASCADE,
    CONSTRAINT [FK_UserExerciseProgress_Plans] FOREIGN KEY ([PlanId]) REFERENCES [dbo].[ExercisePlans] ([PlanId])
);