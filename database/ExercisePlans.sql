CREATE TABLE [dbo].[ExercisePlans] (
    [PlanId]          INT            IDENTITY (1, 1) PRIMARY KEY,
    [PlanName]        NVARCHAR (100) NOT NULL,
    [Description]     NVARCHAR (MAX) NULL,
    [DifficultyLevel] NVARCHAR (20)  NULL, -- Easy, Medium, Hard
    [DurationWeeks]   INT            DEFAULT 1,
    [CreatedAt]       DATETIME2      DEFAULT GETDATE()
);