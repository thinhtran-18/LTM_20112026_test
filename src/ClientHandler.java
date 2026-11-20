import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lớp ClientHandler xử lý các yêu cầu từ Client trên luồng (thread) riêng biệt.
 * Tích hợp toàn bộ nghiệp vụ cơ bản và 5 yêu cầu nâng cao:
 * - YC11: Xác thực tài khoản người dùng và mật khẩu qua CSDL.
 * - YC12: Mượn sách và Trả sách.
 * - YC13: Đăng ký cổng nhận thông báo UDP.
 * - YC14: Ghi nhật ký chi tiết vào file log.
 * - YC15: Đảm bảo kiểm soát tương tranh nhất quán khi mượn/sửa số lượng.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AtomicInteger activeClientsCounter;
    private final String clientIp;
    private final int clientPort;
    private String username = "Khách";
    private String role = "USER";
    private DataInputStream dis;
    private DataOutputStream dos;

    public ClientHandler(Socket socket, AtomicInteger activeClientsCounter) {
        this.socket = socket;
        this.activeClientsCounter = activeClientsCounter;
        this.clientIp = socket.getInetAddress().getHostAddress();
        this.clientPort = socket.getPort();
    }

    @Override
    public void run() {
        try {
            dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            while (!socket.isClosed()) {
                String command;
                try {
                    command = dis.readUTF();
                } catch (EOFException | SocketException e) {
                    break;
                }

                switch (command) {
                    // Yêu cầu nâng cao 11: Đăng nhập & Xác thực qua CSDL
                    case "LOGIN": {
                        String user = dis.readUTF();
                        String pass = dis.readUTF();
                        int clientUdpPort = dis.readInt();

                        UserAccount acc = DatabaseHelper.authenticateUser(user, pass);
                        if (acc != null) {
                            this.username = acc.getUsername();
                            this.role = acc.getRole();

                            // Đăng ký nhận thông báo UDP (Yêu cầu 13)
                            LibraryServer.registerUdpClient(this.username, socket.getInetAddress(), clientUdpPort);

                            dos.writeUTF("SUCCESS");
                            dos.writeUTF(acc.getHoTen());
                            dos.writeUTF(acc.getRole());
                            dos.flush();

                            LibraryServer.logActivity(clientIp, clientPort, username, "LOGIN_SUCCESS",
                                    "Đăng nhập thành công với vai trò: " + acc.getRole());
                            LibraryServer.broadcastUdpNotification("Người dùng [" + acc.getHoTen() + "] vừa kết nối vào thư viện!");
                        } else {
                            dos.writeUTF("ERROR");
                            dos.writeUTF("Tên đăng nhập hoặc mật khẩu không chính xác!");
                            dos.flush();

                            LibraryServer.logActivity(clientIp, clientPort, user, "LOGIN_FAILED",
                                    "Đăng nhập thất bại do sai tài khoản hoặc mật khẩu.");
                        }
                        break;
                    }

                    // Tương thích ngược nếu Client cũ gọi CONNECT
                    case "CONNECT": {
                        this.username = dis.readUTF();
                        dos.writeUTF("SUCCESS");
                        dos.writeUTF("Chào mừng " + username + " đã kết nối thành công đến Server!");
                        dos.flush();
                        LibraryServer.logActivity(clientIp, clientPort, username, "CONNECT", "Kết nối chế độ thường.");
                        break;
                    }

                    // Yêu cầu nâng cao 12 & 15: Mượn sách (Có kiểm soát tương tranh)
                    case "BORROW": {
                        String maSach = dis.readUTF();
                        String err = DatabaseHelper.borrowBook(this.username, maSach);
                        if (err == null) {
                            dos.writeUTF("SUCCESS");
                            dos.writeUTF("Mượn cuốn sách [" + maSach + "] thành công!");
                            dos.flush();

                            LibraryServer.logActivity(clientIp, clientPort, username, "BORROW_BOOK", "Mượn thành công sách: " + maSach);
                            LibraryServer.broadcastUdpNotification("Sách [" + maSach + "] vừa được mượn bởi " + username + "!");
                        } else {
                            dos.writeUTF("ERROR");
                            dos.writeUTF(err);
                            dos.flush();

                            LibraryServer.logActivity(clientIp, clientPort, username, "BORROW_FAILED", "Mượn " + maSach + " thất bại: " + err);
                        }
                        break;
                    }

                    // Yêu cầu nâng cao 12: Trả sách
                    case "RETURN": {
                        String maSach = dis.readUTF();
                        String err = DatabaseHelper.returnBook(this.username, maSach);
                        if (err == null) {
                            dos.writeUTF("SUCCESS");
                            dos.writeUTF("Trả cuốn sách [" + maSach + "] thành công!");
                            dos.flush();

                            LibraryServer.logActivity(clientIp, clientPort, username, "RETURN_BOOK", "Trả thành công sách: " + maSach);
                            LibraryServer.broadcastUdpNotification("Sách [" + maSach + "] đã được hoàn trả lại kho bởi " + username + "!");
                        } else {
                            dos.writeUTF("ERROR");
                            dos.writeUTF(err);
                            dos.flush();

                            LibraryServer.logActivity(clientIp, clientPort, username, "RETURN_FAILED", "Trả " + maSach + " thất bại: " + err);
                        }
                        break;
                    }

                    // Lấy danh sách các mã sách mà người dùng này đang mượn
                    case "GET_MY_BORROWED": {
                        List<String> list = DatabaseHelper.getCurrentlyBorrowedBooks(this.username);
                        dos.writeUTF("SUCCESS");
                        dos.writeInt(list.size());
                        for (String ms : list) {
                            dos.writeUTF(ms);
                        }
                        dos.flush();
                        break;
                    }

                    case "GET_ALL": {
                        List<Book> books = DatabaseHelper.getAllBooks();
                        dos.writeUTF("SUCCESS");
                        dos.writeInt(books.size());
                        for (Book b : books) {
                            dos.writeUTF(b.getMaSach());
                            dos.writeUTF(b.getTenSach());
                            dos.writeUTF(b.getTacGia());
                            dos.writeInt(b.getNamXB());
                            dos.writeInt(b.getSoLuong());
                        }
                        dos.flush();
                        LibraryServer.logActivity(clientIp, clientPort, username, "GET_ALL", "Lấy toàn bộ " + books.size() + " cuốn sách.");
                        break;
                    }

                    case "SEARCH": {
                        String keyword = dis.readUTF();
                        List<Book> books = DatabaseHelper.searchBooks(keyword);
                        dos.writeUTF("SUCCESS");
                        dos.writeInt(books.size());
                        for (Book b : books) {
                            dos.writeUTF(b.getMaSach());
                            dos.writeUTF(b.getTenSach());
                            dos.writeUTF(b.getTacGia());
                            dos.writeInt(b.getNamXB());
                            dos.writeInt(b.getSoLuong());
                        }
                        dos.flush();
                        LibraryServer.logActivity(clientIp, clientPort, username, "SEARCH", "Tìm kiếm từ khóa: '" + keyword + "'");
                        break;
                    }

                    case "ADD": {
                        String maSach = dis.readUTF();
                        String tenSach = dis.readUTF();
                        String tacGia = dis.readUTF();
                        int namXB = dis.readInt();
                        int soLuong = dis.readInt();

                        Book book = new Book(maSach, tenSach, tacGia, namXB, soLuong);
                        String error = DatabaseHelper.addBook(book);

                        if (error == null) {
                            dos.writeUTF("SUCCESS");
                            dos.writeUTF("Thêm sách mới [" + maSach + "] thành công!");
                            LibraryServer.logActivity(clientIp, clientPort, username, "ADD_BOOK", "Thêm sách mới: " + maSach);
                            LibraryServer.broadcastUdpNotification("Sách mới [" + maSach + " - " + tenSach + "] vừa được bổ sung vào thư viện!");
                        } else {
                            dos.writeUTF("ERROR");
                            dos.writeUTF(error);
                            LibraryServer.logActivity(clientIp, clientPort, username, "ADD_FAILED", "Thêm sách " + maSach + " lỗi: " + error);
                        }
                        dos.flush();
                        break;
                    }

                    case "UPDATE": {
                        String maSach = dis.readUTF();
                        String tenSach = dis.readUTF();
                        String tacGia = dis.readUTF();
                        int namXB = dis.readInt();
                        int soLuong = dis.readInt();

                        Book book = new Book(maSach, tenSach, tacGia, namXB, soLuong);
                        String error = DatabaseHelper.updateBook(book);

                        if (error == null) {
                            dos.writeUTF("SUCCESS");
                            dos.writeUTF("Cập nhật sách [" + maSach + "] thành công!");
                            LibraryServer.logActivity(clientIp, clientPort, username, "UPDATE_BOOK", "Cập nhật sách: " + maSach);
                            LibraryServer.broadcastUdpNotification("Thông tin cuốn sách [" + maSach + "] vừa được cập nhật!");
                        } else {
                            dos.writeUTF("ERROR");
                            dos.writeUTF(error);
                            LibraryServer.logActivity(clientIp, clientPort, username, "UPDATE_FAILED", "Sửa sách " + maSach + " lỗi: " + error);
                        }
                        dos.flush();
                        break;
                    }

                    case "DELETE": {
                        String maSach = dis.readUTF();
                        String error = DatabaseHelper.deleteBook(maSach);

                        if (error == null) {
                            dos.writeUTF("SUCCESS");
                            dos.writeUTF("Xóa sách [" + maSach + "] thành công!");
                            LibraryServer.logActivity(clientIp, clientPort, username, "DELETE_BOOK", "Xóa sách: " + maSach);
                            LibraryServer.broadcastUdpNotification("Sách [" + maSach + "] vừa bị xóa khỏi hệ thống!");
                        } else {
                            dos.writeUTF("ERROR");
                            dos.writeUTF(error);
                            LibraryServer.logActivity(clientIp, clientPort, username, "DELETE_FAILED", "Xóa sách " + maSach + " lỗi: " + error);
                        }
                        dos.flush();
                        break;
                    }

                    case "QUIT": {
                        dos.writeUTF("SUCCESS");
                        dos.writeUTF("Tạm biệt!");
                        dos.flush();
                        return;
                    }

                    default: {
                        dos.writeUTF("ERROR");
                        dos.writeUTF("Lệnh không được hỗ trợ: " + command);
                        dos.flush();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            // Lỗi đường truyền
        } finally {
            LibraryServer.unregisterUdpClient(this.username);
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
            int remaining = activeClientsCounter.decrementAndGet();
            LibraryServer.logActivity(clientIp, clientPort, username, "DISCONNECT",
                    String.format("Client ngắt kết nối. (Số Client online còn lại: %d)", remaining));
        }
    }
}
