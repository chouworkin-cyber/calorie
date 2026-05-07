CREATE TABLE [dbo].[Foods] (
    [FoodId]   INT            IDENTITY (1, 1) PRIMARY KEY,
    [FoodName] NVARCHAR (100) NOT NULL,
    [Calories] INT            NOT NULL,
    [Protein]  FLOAT          NULL,
    [Carbs]    FLOAT          NULL,
    [Fat]      FLOAT          NULL,
    [Unit]     NVARCHAR (50)  NULL -- เช่น "จาน", "100 กรัม"
);