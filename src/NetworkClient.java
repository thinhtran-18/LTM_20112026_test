import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lớp NetworkClient quản lý kết nối TCP và UDP của Client.
 * Tích hợp:
 * - Đăng nhập & xác thực tài khoản (Yêu cầu 11).
 * - Mượn & Trả sách (Yêu cầu 12).
 * - Lắng nghe thông báo UDP từ Server (Yêu cầu 13).
 * - Các chức năng CRUD sách cơ bản.
 */
public class NetworkClient {
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private DatagramSocket udpSocket;
    private int udpPort = 0;
    private Thread udpListenerThread;
    private Consumer<String> notificationCallback;

    private String host;
    private int port;
    private String username = "sinhvien";
    private String hoTen = "Sinh Viên";
    private String role = "USER";

    public NetworkClient() {
    }

    /**
     * Thiết lập hàm callback xử lý khi nhận được thông báo UDP từ Server.
     */
    public void setNotificationCallback(Consumer<String> callback) {
        this.notificationCallback = callback;
    }

    /**
     * Yêu cầu nâng cao 11: Đăng nhập và xác thực với Server kèm đăng ký cổng UDP (Yêu cầu 13).
     */
    public synchronized void login(String host, int port, String username, String password) throws IOException {
        this.host = host;
        this.port = port;
        this.username = (username == null || username.trim().isEmpty()) ? "sinhvien" : username.trim();

        // 1. Khởi tạo DatagramSocket trên một cổng ngẫu nhiên khả dụng để nhận UDP thông báo
        try {
            if (this.udpSocket != null && !this.udpSocket.isClosed()) {
                this.udpSocket.close();
            }
            this.udpSocket = new DatagramSocket(0);
            this.udpPort = this.udpSocket.getLocalPort();
            startUdpListener();
        } catch (Exception e) {
            System.err.println("[NetworkClient] Không thể mở cổng UDP: " + e.getMessage());
            this.udpPort = 0;
        }

        // 2. Mở kết nối TCP Socket
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(this.host, this.port), 5000);

        this.dis = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
        this.dos = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));

        // 3. Gửi lệnh LOGIN kèm thông tin xác thực và cổng UDP
        dos.writeUTF("LOGIN");
        dos.writeUTF(this.username);
        dos.writeUTF(password == null ? "" : password);
        dos.writeInt(this.udpPort);
        dos.flush();

        String status = dis.readUTF();
        if ("SUCCESS".equals(status)) {
            this.hoTen = dis.readUTF();
            this.role = dis.readUTF();
        } else {
            String errorMsg = dis.readUTF();
            disconnect();
            throw new IOException(errorMsg);
        }
    }

    /**
     * Phương thức kết nối tương thích ngược (nếu không dùng mật khẩu).
     */
    public synchronized void connect(String host, int port, String username) throws IOException {
        login(host, port, username, "123456");
    }

    /**
     * Yêu cầu nâng cao 13: Luồng nền liên tục lắng nghe các gói tin thông báo UDP từ Server.
     */
    private void startUdpListener() {
        if (udpSocket == null) return;

        udpListenerThread = new Thread(() -> {
            byte[] buffer = new byte[2048];
            while (udpSocket != null && !udpSocket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                    System.out.println("[Client UDP Notification] " + msg);
                    if (notificationCallback != null) {
                        notificationCallback.accept(msg);
                    }
                } catch (IOException e) {
                    break;
                }
            }
        });
        udpListenerThread.setDaemon(true);
        udpListenerThread.start();
    }

    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Yêu cầu nâng cao 12 & 15: Mượn sách
     */
    public synchronized String borrowBook(String maSach) throws IOException {
        ensureConnected();
        dos.writeUTF("BORROW");
        dos.writeUTF(maSach == null ? "" : maSach.trim());
        dos.flush();

        String status = dis.readUTF();
        String message = dis.readUTF();
        return "SUCCESS".equals(status) ? null : message;
    }

    /**
     * Yêu cầu nâng cao 12: Trả sách
     */
    public synchronized String returnBook(String maSach) throws IOException {
        ensureConnected();
        dos.writeUTF("RETURN");
        dos.writeUTF(maSach == null ? "" : maSach.trim());
        dos.flush();

        String status = dis.readUTF();
        String message = dis.readUTF();
        return "SUCCESS".equals(status) ? null : message;
    }

    /**
     * Lấy danh sách các mã sách mà tài khoản này đang mượn
     */
    public synchronized List<String> getMyBorrowedBooks() throws IOException {
        ensureConnected();
        dos.writeUTF("GET_MY_BORROWED");
        dos.flush();

        String status = dis.readUTF();
        if ("SUCCESS".equals(status)) {
            int count = dis.readInt();
            List<String> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                list.add(dis.readUTF());
            }
            return list;
        }
        return new ArrayList<>();
    }

    public synchronized List<Book> getAllBooks() throws IOException {
        ensureConnected();
        dos.writeUTF("GET_ALL");
        dos.flush();

        String status = dis.readUTF();
        if ("SUCCESS".equals(status)) {
            int count = dis.readInt();
            List<Book> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String ma = dis.readUTF();
                String ten = dis.readUTF();
                String tg = dis.readUTF();
                int nam = dis.readInt();
                int sl = dis.readInt();
                list.add(new Book(ma, ten, tg, nam, sl));
            }
            return list;
        } else {
            throw new IOException(dis.readUTF());
        }
    }

    public synchronized List<Book> searchBooks(String keyword) throws IOException {
        ensureConnected();
        dos.writeUTF("SEARCH");
        dos.writeUTF(keyword == null ? "" : keyword.trim());
        dos.flush();

        String status = dis.readUTF();
        if ("SUCCESS".equals(status)) {
            int count = dis.readInt();
            List<Book> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String ma = dis.readUTF();
                String ten = dis.readUTF();
                String tg = dis.readUTF();
                int nam = dis.readInt();
                int sl = dis.readInt();
                list.add(new Book(ma, ten, tg, nam, sl));
            }
            return list;
        } else {
            throw new IOException(dis.readUTF());
        }
    }

    public synchronized String addBook(Book book) throws IOException {
        ensureConnected();
        dos.writeUTF("ADD");
        dos.writeUTF(book.getMaSach());
        dos.writeUTF(book.getTenSach());
        dos.writeUTF(book.getTacGia());
        dos.writeInt(book.getNamXB());
        dos.writeInt(book.getSoLuong());
        dos.flush();

        String status = dis.readUTF();
        String message = dis.readUTF();
        return "SUCCESS".equals(status) ? null : message;
    }

    public synchronized String updateBook(Book book) throws IOException {
        ensureConnected();
        dos.writeUTF("UPDATE");
        dos.writeUTF(book.getMaSach());
        dos.writeUTF(book.getTenSach());
        dos.writeUTF(book.getTacGia());
        dos.writeInt(book.getNamXB());
        dos.writeInt(book.getSoLuong());
        dos.flush();

        String status = dis.readUTF();
        String message = dis.readUTF();
        return "SUCCESS".equals(status) ? null : message;
    }

    public synchronized String deleteBook(String maSach) throws IOException {
        ensureConnected();
        dos.writeUTF("DELETE");
        dos.writeUTF(maSach);
        dos.flush();

        String status = dis.readUTF();
        String message = dis.readUTF();
        return "SUCCESS".equals(status) ? null : message;
    }

    public synchronized void disconnect() {
        try {
            if (isConnected()) {
                dos.writeUTF("QUIT");
                dos.flush();
            }
        } catch (Exception ignored) {
        } finally {
            try { if (dis != null) dis.close(); } catch (Exception ignored) {}
            try { if (dos != null) dos.close(); } catch (Exception ignored) {}
            try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception ignored) {}
            try { if (udpSocket != null && !udpSocket.isClosed()) udpSocket.close(); } catch (Exception ignored) {}
            socket = null;
            udpSocket = null;
        }
    }

    private void ensureConnected() throws IOException {
        if (!isConnected()) {
            throw new IOException("Chưa kết nối đến Server hoặc kết nối đã bị ngắt!");
        }
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public String getHoTen() { return hoTen; }
    public String getRole() { return role; }
    public int getUdpPort() { return udpPort; }
}
