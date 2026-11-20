-- ==========================================================
-- BÀI TOÁN: XÂY DỰNG HỆ THỐNG QUẢN LÝ THƯ VIỆN QUA MẠNG
-- FILE: schema.sql - Cấu trúc bảng và dữ liệu mẫu SQLite
-- Hỗ trợ đầy đủ chức năng cơ bản & các yêu cầu nâng cao (11-15)
-- ==========================================================

-- 1. Bảng Sach: Quản lý danh mục sách thư viện
CREATE TABLE IF NOT EXISTS Sach (
    MaSach VARCHAR(10) PRIMARY KEY,
    TenSach VARCHAR(100) NOT NULL,
    TacGia VARCHAR(100) NOT NULL,
    NamXB INT NOT NULL,
    SoLuong INT NOT NULL CHECK(SoLuong >= 0)
);

-- 2. Bảng TaiKhoan: Quản lý tài khoản và xác thực (Yêu cầu nâng cao 11)
CREATE TABLE IF NOT EXISTS TaiKhoan (
    Username VARCHAR(50) PRIMARY KEY,
    Password VARCHAR(50) NOT NULL,
    HoTen VARCHAR(100) NOT NULL,
    Role VARCHAR(20) NOT NULL DEFAULT 'USER'
);

-- 3. Bảng PhieuMuon: Quản lý thông tin mượn và trả sách (Yêu cầu nâng cao 12)
CREATE TABLE IF NOT EXISTS PhieuMuon (
    MaPhieu INTEGER PRIMARY KEY AUTOINCREMENT,
    Username VARCHAR(50) NOT NULL,
    MaSach VARCHAR(10) NOT NULL,
    NgayMuon TEXT NOT NULL,
    TrangThai TEXT NOT NULL DEFAULT 'DANG_MUON', -- 'DANG_MUON' hoặc 'DA_TRA'
    FOREIGN KEY (MaSach) REFERENCES Sach(MaSach),
    FOREIGN KEY (Username) REFERENCES TaiKhoan(Username)
);

-- 4. Dữ liệu mẫu bảng Sach (theo Hình 3 đề thi)
INSERT OR REPLACE INTO Sach (MaSach, TenSach, TacGia, NamXB, SoLuong) VALUES
('S001', 'Lập trình java', 'Nguyễn Văn A', 2024, 20),
('S002', 'Mạng máy tính', 'Trần Thị B', 2023, 15),
('S003', 'Cơ sở dữ liệu', 'Lê Văn C', 2022, 12),
('S004', 'Lập trình mạng', 'Phạm Văn D', 2025, 18);

-- 5. Dữ liệu mẫu bảng TaiKhoan
INSERT OR REPLACE INTO TaiKhoan (Username, Password, HoTen, Role) VALUES
('admin', 'admin123', 'Quản trị viên Thư viện', 'ADMIN'),
('sinhvien', '123456', 'Nguyễn Sinh Viên', 'USER'),
('sinhvien1', '123456', 'Trần Sinh Viên 1', 'USER'),
('sinhvien2', '123456', 'Lê Sinh Viên 2', 'USER');
