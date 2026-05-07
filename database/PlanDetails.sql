CREATE TABLE [dbo].[PlanDetails] (
    [DetailId]        INT            IDENTITY (1, 1) PRIMARY KEY,
    [PlanId]          INT            NOT NULL,
    [DayNumber]       INT            NOT NULL, -- วันที่ 1, 2, 3...
    [ActivityName]    NVARCHAR (100) NOT NULL,
    [DurationMinutes] INT            NOT NULL,
    
    CONSTRAINT [FK_PlanDetails_ExercisePlans] FOREIGN KEY ([PlanId]) REFERENCES [dbo].[ExercisePlans] ([PlanId]) ON DELETE CASCADE
);