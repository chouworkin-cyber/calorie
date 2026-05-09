-- ข้อมูลตัวอย่างที่เคยอยู่ใน Java
IF NOT EXISTS (SELECT * FROM Users WHERE Username = 'admin')
BEGIN
    INSERT INTO Users (Username, Email, PasswordHash, TotalPoints, CreatedAt)
    VALUES ('admin', 'admin@example.com', '$2a$10$xyz...', 100, GETDATE());
END