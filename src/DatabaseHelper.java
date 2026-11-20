import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Lớp đối tượng Sách đại diện cho cấu trúc bảng Sach trong CSDL.
 */
class Book {
    private String maSach;
    private String tenSach;
    private String tacGia;
    private int namXB;
    private int soLuong;

    public Book() {
    }

    public Book(String maSach, String tenSach, String tacGia, int namXB, int soLuong) {
        this.maSach = maSach;
        this.tenSach = tenSach;
        this.tacGia = tacGia;
        this.namXB = namXB;
        this.soLuong = soLuong;
    }

    public String getMaSach() {
        return maSach;
    }

    public void setMaSach(String maSach) {
        this.maSach = maSach;
    }

    public String getTenSach() {
        return tenSach;
    }

    public void setTenSach(String tenSach) {
        this.tenSach = tenSach;
    }

    public String getTacGia() {
        return tacGia;
    }

    public void setTacGia(String tacGia) {
        this.tacGia = tacGia;
    }

    public int getNamXB() {
        return namXB;
    }

    public void setNamXB(int namXB) {
        this.namXB = namXB;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
    }

    @Override
    public String toString() {
        return "Book{" + "maSach='" + maSach + '\'' + ", tenSach='" + tenSach + '\'' +
                ", tacGia='" + tacGia + '\'' + ", namXB=" + namXB + ", soLuong=" + soLuong + '}';
    }
}

/**
 * Lớp UserAccount đại diện cho tài khoản người dùng (Yêu cầu nâng cao 11).
 */
class UserAccount {
    private String username;
    private String password;
    private String hoTen;
    private String role; // ADMIN hoặc USER

    public UserAccount(String username, String password, String hoTen, String role) {
        this.username = username;
        this.password = password;
        this.hoTen = hoTen;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHoTen() {
        return hoTen;
    }

    public String getRole() {
        return role;
    }
}

/**
 * Lớp BorrowTicket đại diện cho phiếu mượn sách (Yêu cầu nâng cao 12).
 */
class BorrowTicket {
    private int maPhieu;
    private String username;
    private String maSach;
    private String ngayMuon;
    private String trangThai;

    public BorrowTicket(int maPhieu, String username, String maSach, String ngayMuon, String trangThai) {
        this.maPhieu = maPhieu;
        this.username = username;
        this.maSach = maSach;
        this.ngayMuon = ngayMuon;
        this.trangThai = trangThai;
    }

    public int getMaPhieu() {
        return maPhieu;
    }

    public String getUsername() {
        return username;
    }

    public String getMaSach() {
        return maSach;
    }

    public String getNgayMuon() {
        return ngayMuon;
    }

    public String getTrangThai() {
        return trangThai;
    }
}

/**
 * Lớp DatabaseHelper quản lý toàn bộ thao tác kết nối và xử lý CSDL SQLite.
 * Hỗ trợ:
 * - Khởi tạo bảng Sach, TaiKhoan, PhieuMuon.
 * - CRUD Sách.
 * - Đăng nhập & xác thực người dùng (Yêu cầu 11).
 * - Mượn & Trả sách (Yêu cầu 12).
 * - Xử lý tương tranh & nhất quán dữ liệu với Transaction SQLite (Yêu cầu 15).
 */
public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:library.db";

    static {
        try {
            System.setOut(new java.io.PrintStream(
                    new java.io.FileOutputStream(java.io.FileDescriptor.out), true, java.nio.charset.StandardCharsets.UTF_8));
            System.setErr(new java.io.PrintStream(
                    new java.io.FileOutputStream(java.io.FileDescriptor.err), true, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseHelper] Không tìm thấy driver SQLite JDBC: " + e.getMessage());
        }
    }

    /**
     * Mở kết nối đến file SQLite library.db
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Khởi tạo CSDL: tự động tạo bảng Sach, TaiKhoan, PhieuMuon và nạp dữ liệu mẫu ban đầu.
     */
    public static synchronized void initDatabase() {
        String createSachSQL = "CREATE TABLE IF NOT EXISTS Sach ("
                + "MaSach VARCHAR(10) PRIMARY KEY, "
                + "TenSach VARCHAR(100) NOT NULL, "
                + "TacGia VARCHAR(100) NOT NULL, "
                + "NamXB INT NOT NULL, "
                + "SoLuong INT NOT NULL CHECK(SoLuong >= 0)"
                + ");";

        String createTaiKhoanSQL = "CREATE TABLE IF NOT EXISTS TaiKhoan ("
                + "Username VARCHAR(50) PRIMARY KEY, "
                + "Password VARCHAR(50) NOT NULL, "
                + "HoTen VARCHAR(100) NOT NULL, "
                + "Role VARCHAR(20) NOT NULL DEFAULT 'USER'"
                + ");";

        String createPhieuMuonSQL = "CREATE TABLE IF NOT EXISTS PhieuMuon ("
                + "MaPhieu INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "Username VARCHAR(50) NOT NULL, "
                + "MaSach VARCHAR(10) NOT NULL, "
                + "NgayMuon TEXT NOT NULL, "
                + "TrangThai TEXT NOT NULL DEFAULT 'DANG_MUON', "
                + "FOREIGN KEY (MaSach) REFERENCES Sach(MaSach), "
                + "FOREIGN KEY (Username) REFERENCES TaiKhoan(Username)"
                + ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createSachSQL);
            stmt.execute(createTaiKhoanSQL);
            stmt.execute(createPhieuMuonSQL);

            // 1. Kiểm tra và chèn dữ liệu mẫu bảng Sach
            ResultSet rsSach = stmt.executeQuery("SELECT COUNT(*) FROM Sach");
            if (rsSach.next() && rsSach.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO Sach (MaSach, TenSach, TacGia, NamXB, SoLuong) VALUES "
                        + "('S001', 'Lập trình java', 'Nguyễn Văn A', 2024, 20), "
                        + "('S002', 'Mạng máy tính', 'Trần Thị B', 2023, 15), "
                        + "('S003', 'Cơ sở dữ liệu', 'Lê Văn C', 2022, 12), "
                        + "('S004', 'Lập trình mạng', 'Phạm Văn D', 2025, 18);");
                System.out.println("[DatabaseHelper] Đã chèn dữ liệu mẫu vào bảng Sach.");
            }

            // 2. Kiểm tra và chèn tài khoản mẫu (Yêu cầu 11)
            ResultSet rsTK = stmt.executeQuery("SELECT COUNT(*) FROM TaiKhoan");
            if (rsTK.next() && rsTK.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO TaiKhoan (Username, Password, HoTen, Role) VALUES "
                        + "('admin', 'admin123', 'Quản trị viên Thư viện', 'ADMIN'), "
                        + "('sinhvien', '123456', 'Nguyễn Sinh Viên', 'USER'), "
                        + "('sinhvien1', '123456', 'Trần Sinh Viên 1', 'USER'), "
                        + "('sinhvien2', '123456', 'Lê Sinh Viên 2', 'USER');");
                System.out.println("[DatabaseHelper] Đã chèn dữ liệu mẫu tài khoản vào bảng TaiKhoan.");
            }

            System.out.println("[DatabaseHelper] Khởi tạo CSDL SQLite (library.db) hoàn tất!");
        } catch (SQLException e) {
            System.err.println("[DatabaseHelper] Lỗi khi khởi tạo CSDL: " + e.getMessage());
        }
    }

    // =========================================================================
    // YÊU CẦU NÂNG CAO 11: XÁC THỰC TÀI KHOẢN NGƯỜI DÙNG QUA SERVER
    // =========================================================================

    /**
     * Xác thực thông tin đăng nhập từ Client.
     * @return Đối tượng UserAccount nếu chính xác; null nếu sai thông tin.
     */
    public static synchronized UserAccount authenticateUser(String username, String password) {
        if (username == null || password == null) return null;
        String sql = "SELECT Username, Password, HoTen, Role FROM TaiKhoan WHERE Username = ? AND Password = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, password.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserAccount(
                            rs.getString("Username"),
                            rs.getString("Password"),
                            rs.getString("HoTen"),
                            rs.getString("Role")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseHelper] Lỗi authenticateUser: " + e.getMessage());
        }
        return null;
    }

    // =========================================================================
    // YÊU CẦU NÂNG CAO 12 & 15: MƯỢN SÁCH & TRẢ SÁCH (KIỂM SOÁT ĐỒNG THỜI ATOMIC)
    // =========================================================================

    /**
     * Mượn sách có kiểm soát tương tranh và bảo toàn tính nhất quán (ACID Transaction).
     * Tránh lỗi hai Client cùng mượn khi sách chỉ còn 1 cuốn.
     * @return null nếu thành công; chuỗi thông báo lỗi nếu thất bại.
     */
    public static synchronized String borrowBook(String username, String maSach) {
        if (username == null || username.trim().isEmpty()) {
            return "Tên người dùng không hợp lệ!";
        }
        if (maSach == null || maSach.trim().isEmpty()) {
            return "Vui lòng chọn mã sách muốn mượn!";
        }

        maSach = maSach.trim();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Bắt đầu Transaction

            // 1. Cập nhật số lượng nguyên tử: chỉ trừ khi SoLuong > 0
            String updateSql = "UPDATE Sach SET SoLuong = SoLuong - 1 WHERE MaSach = ? AND SoLuong > 0";
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                psUpdate.setString(1, maSach);
                int rowsAffected = psUpdate.executeUpdate();

                if (rowsAffected == 0) {
                    // Kiểm tra xem do hết sách hay mã sách không tồn tại
                    conn.rollback();
                    if (!isBookExists(maSach)) {
                        return "Mã sách '" + maSach + "' không tồn tại trong thư viện!";
                    } else {
                        return "Sách '" + maSach + "' hiện đã hết số lượng trong kho, không thể mượn!";
                    }
                }
            }

            // 2. Tạo bản ghi phiếu mượn mới
            String insertLoanSql = "INSERT INTO PhieuMuon (Username, MaSach, NgayMuon, TrangThai) VALUES (?, ?, ?, 'DANG_MUON')";
            try (PreparedStatement psInsert = conn.prepareStatement(insertLoanSql)) {
                psInsert.setString(1, username);
                psInsert.setString(2, maSach);
                psInsert.setString(3, dateStr);
                psInsert.executeUpdate();
            }

            conn.commit(); // Hoàn tất Transaction
            return null;   // Mượn thành công
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return "Lỗi CSDL khi mượn sách: " + e.getMessage();
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Trả sách đã mượn: Cập nhật trạng thái phiếu mượn và hoàn trả lại số lượng sách.
     * @return null nếu thành công; chuỗi thông báo lỗi nếu thất bại.
     */
    public static synchronized String returnBook(String username, String maSach) {
        if (username == null || username.trim().isEmpty()) return "Tên người dùng không hợp lệ!";
        if (maSach == null || maSach.trim().isEmpty()) return "Vui lòng chọn mã sách cần trả!";

        maSach = maSach.trim();
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Bắt đầu Transaction

            // 1. Tìm phiếu mượn hợp lệ đang mượn của người dùng
            int maPhieu = -1;
            String findLoanSql = "SELECT MaPhieu FROM PhieuMuon WHERE Username = ? AND MaSach = ? AND TrangThai = 'DANG_MUON' LIMIT 1";
            try (PreparedStatement psFind = conn.prepareStatement(findLoanSql)) {
                psFind.setString(1, username);
                psFind.setString(2, maSach);
                try (ResultSet rs = psFind.executeQuery()) {
                    if (rs.next()) {
                        maPhieu = rs.getInt("MaPhieu");
                    }
                }
            }

            if (maPhieu == -1) {
                conn.rollback();
                return "Bạn hiện không có phiếu mượn nào đang mở đối với cuốn sách '" + maSach + "'!";
            }

            // 2. Cập nhật phiếu mượn sang 'DA_TRA'
            String updateLoanSql = "UPDATE PhieuMuon SET TrangThai = 'DA_TRA' WHERE MaPhieu = ?";
            try (PreparedStatement psUpdateLoan = conn.prepareStatement(updateLoanSql)) {
                psUpdateLoan.setInt(1, maPhieu);
                psUpdateLoan.executeUpdate();
            }

            // 3. Tăng lại số lượng sách trong kho
            String updateBookSql = "UPDATE Sach SET SoLuong = SoLuong + 1 WHERE MaSach = ?";
            try (PreparedStatement psUpdateBook = conn.prepareStatement(updateBookSql)) {
                psUpdateBook.setString(1, maSach);
                psUpdateBook.executeUpdate();
            }

            conn.commit();
            return null; // Trả sách thành công
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return "Lỗi CSDL khi trả sách: " + e.getMessage();
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Lấy danh sách các mã sách mà người dùng đang mượn.
     */
    public static synchronized List<String> getCurrentlyBorrowedBooks(String username) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT MaSach FROM PhieuMuon WHERE Username = ? AND TrangThai = 'DANG_MUON'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("MaSach"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseHelper] Lỗi getCurrentlyBorrowedBooks: " + e.getMessage());
        }
        return list;
    }

    // =========================================================================
    // CÁC HÀM CRUD CƠ BẢN QUẢN LÝ SÁCH
    // =========================================================================

    public static synchronized List<Book> getAllBooks() {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT MaSach, TenSach, TacGia, NamXB, SoLuong FROM Sach ORDER BY MaSach ASC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Book(
                        rs.getString("MaSach"),
                        rs.getString("TenSach"),
                        rs.getString("TacGia"),
                        rs.getInt("NamXB"),
                        rs.getInt("SoLuong")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseHelper] Lỗi getAllBooks: " + e.getMessage());
        }
        return list;
    }

    public static synchronized List<Book> searchBooks(String keyword) {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT MaSach, TenSach, TacGia, NamXB, SoLuong FROM Sach "
                + "WHERE MaSach LIKE ? OR TenSach LIKE ? ORDER BY MaSach ASC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Book(
                            rs.getString("MaSach"),
                            rs.getString("TenSach"),
                            rs.getString("TacGia"),
                            rs.getInt("NamXB"),
                            rs.getInt("SoLuong")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseHelper] Lỗi searchBooks: " + e.getMessage());
        }
        return list;
    }

    public static synchronized boolean isBookExists(String maSach) {
        String sql = "SELECT 1 FROM Sach WHERE MaSach = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSach);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public static synchronized String addBook(Book book) {
        if (book.getMaSach() == null || book.getMaSach().trim().isEmpty()) return "Mã sách không được để trống!";
        if (book.getTenSach() == null || book.getTenSach().trim().isEmpty()) return "Tên sách không được để trống!";
        if (isBookExists(book.getMaSach().trim())) {
            return "Mã sách '" + book.getMaSach() + "' đã tồn tại trong thư viện!";
        }

        String sql = "INSERT INTO Sach (MaSach, TenSach, TacGia, NamXB, SoLuong) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, book.getMaSach().trim());
            ps.setString(2, book.getTenSach().trim());
            ps.setString(3, book.getTacGia() == null ? "" : book.getTacGia().trim());
            ps.setInt(4, book.getNamXB());
            ps.setInt(5, book.getSoLuong());
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            return "Lỗi CSDL khi thêm sách: " + e.getMessage();
        }
    }

    public static synchronized String updateBook(Book book) {
        if (book.getMaSach() == null || book.getMaSach().trim().isEmpty()) return "Mã sách không được để trống!";
        if (!isBookExists(book.getMaSach().trim())) {
            return "Mã sách '" + book.getMaSach() + "' không tồn tại trong thư viện!";
        }

        String sql = "UPDATE Sach SET TenSach = ?, TacGia = ?, NamXB = ?, SoLuong = ? WHERE MaSach = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, book.getTenSach().trim());
            ps.setString(2, book.getTacGia() == null ? "" : book.getTacGia().trim());
            ps.setInt(3, book.getNamXB());
            ps.setInt(4, book.getSoLuong());
            ps.setString(5, book.getMaSach().trim());
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            return "Lỗi CSDL khi cập nhật sách: " + e.getMessage();
        }
    }

    public static synchronized String deleteBook(String maSach) {
        if (maSach == null || maSach.trim().isEmpty()) return "Mã sách không được để trống!";
        if (!isBookExists(maSach.trim())) {
            return "Mã sách '" + maSach + "' không tồn tại trong thư viện!";
        }

        String sql = "DELETE FROM Sach WHERE MaSach = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, maSach.trim());
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            return "Lỗi CSDL khi xóa sách: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        System.out.println("=== KIỂM TRA DATABASE HELPER & CSDL SQLITE ===");
        initDatabase();
        List<Book> books = getAllBooks();
        System.out.println("Tổng số sách hiện có trong CSDL: " + books.size());
        for (Book b : books) {
            System.out.println(" + " + b);
        }
    }
}
