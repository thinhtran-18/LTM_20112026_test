import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chương trình Server đa luồng (Multi-threaded Library Server).
 * - Sử dụng ServerSocket lắng nghe tại cổng TCP 9999.
 * - Sử dụng ExecutorService để quản lý pool các thread xử lý nhiều Client đồng thời.
 * - Yêu cầu 11: Đăng nhập và xác thực tài khoản người dùng qua CSDL.
 * - Yêu cầu 12: Nghiệp vụ Mượn sách và Trả sách.
 * - Yêu cầu 13: Gửi thông báo từ Server đến các Client qua UDP Socket.
 * - Yêu cầu 14: Ghi nhật ký (log) hoạt động của Client vào file server_activity.log.
 * - Yêu cầu 15: Kiểm soát tương tranh, nhất quán dữ liệu khi nhiều Client cùng mượn/sửa số lượng sách.
 */
public class LibraryServer {
    public static final int TCP_PORT = 9999;
    public static final String LOG_FILE = "server_activity.log";

    private static final AtomicInteger activeClientsCounter = new AtomicInteger(0);

    // Bảng lưu trữ địa chỉ UDP của các Client đang online để gửi thông báo (Yêu cầu 13)
    public static class ClientUdpEndpoint {
        public InetAddress address;
        public int udpPort;

        public ClientUdpEndpoint(InetAddress address, int udpPort) {
            this.address = address;
            this.udpPort = udpPort;
        }
    }

    private static final Map<String, ClientUdpEndpoint> onlineUdpClients = new ConcurrentHashMap<>();
    private static DatagramSocket udpSocket;

    static {
        try {
            System.setOut(new java.io.PrintStream(
                    new FileOutputStream(java.io.FileDescriptor.out), true, StandardCharsets.UTF_8));
            System.setErr(new java.io.PrintStream(
                    new FileOutputStream(java.io.FileDescriptor.err), true, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    public static String getTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    /**
     * Yêu cầu nâng cao 14: Ghi nhật ký hoạt động của các Client vào file server_activity.log
     */
    public static synchronized void logActivity(String ip, int port, String username, String action, String details) {
        String logEntry = String.format("[%s] [%s:%d] [User: %s] [Action: %s] %s",
                getTimestamp(), ip, port, (username == null ? "Anonymous" : username), action, details);

        // 1. In ra màn hình console Server
        System.out.println(logEntry);

        // 2. Ghi nối tiếp vào file nhật ký server_activity.log (chuẩn UTF-8)
        try (PrintWriter writer = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(new FileOutputStream(LOG_FILE, true), StandardCharsets.UTF_8)))) {
            writer.println(logEntry);
        } catch (IOException e) {
            System.err.println("[Server] Lỗi khi ghi file nhật ký log: " + e.getMessage());
        }
    }

    /**
     * Đăng ký địa chỉ UDP của Client sau khi Client đăng nhập thành công.
     */
    public static void registerUdpClient(String username, InetAddress ip, int udpPort) {
        if (udpPort > 0) {
            onlineUdpClients.put(username, new ClientUdpEndpoint(ip, udpPort));
            logActivity(ip.getHostAddress(), udpPort, username, "UDP_REGISTER", "Đăng ký nhận thông báo UDP tại cổng " + udpPort);
        }
    }

    /**
     * Hủy đăng ký UDP khi Client ngắt kết nối.
     */
    public static void unregisterUdpClient(String username) {
        if (username != null) {
            onlineUdpClients.remove(username);
        }
    }

    /**
     * Yêu cầu nâng cao 13: Gửi thông báo từ Server đến toàn bộ Client đang kết nối qua UDP Socket
     */
    public static void broadcastUdpNotification(String message) {
        if (udpSocket == null || udpSocket.isClosed() || onlineUdpClients.isEmpty()) {
            return;
        }

        new Thread(() -> {
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            for (Map.Entry<String, ClientUdpEndpoint> entry : onlineUdpClients.entrySet()) {
                ClientUdpEndpoint endpoint = entry.getValue();
                try {
                    DatagramPacket packet = new DatagramPacket(data, data.length, endpoint.address, endpoint.udpPort);
                    udpSocket.send(packet);
                } catch (IOException e) {
                    System.err.println("[Server UDP] Lỗi gửi thông báo tới " + entry.getKey() + ": " + e.getMessage());
                }
            }
        }).start();
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   HỆ THỐNG QUẢN LÝ THƯ VIỆN QUA MẠNG - JAVA TCP/UDP SERVER   ");
        System.out.println("===============================================================");

        // 1. Khởi tạo CSDL SQLite
        System.out.println("[" + getTimestamp() + "] [Server] Đang kết nối và khởi tạo CSDL SQLite...");
        DatabaseHelper.initDatabase();

        // 2. Khởi tạo UDP Socket để phục vụ gửi thông báo (Yêu cầu 13)
        try {
            udpSocket = new DatagramSocket();
            System.out.printf("[%s] [Server] UDP Notification Service khởi động tại cổng nội bộ: %d%n",
                    getTimestamp(), udpSocket.getLocalPort());
        } catch (IOException e) {
            System.err.println("[Server] Không thể khởi tạo UDP Socket: " + e.getMessage());
        }

        // 3. Khởi tạo ThreadPool để xử lý đa Client đồng thời
        ExecutorService threadPool = Executors.newCachedThreadPool();

        // 4. Khởi tạo ServerSocket lắng nghe cổng TCP 9999
        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            System.out.printf("[%s] [Server] Server TCP đang lắng nghe tại cổng TCP %d...%n", getTimestamp(), TCP_PORT);
            System.out.println("[" + getTimestamp() + "] [Server] Nhật ký hoạt động được ghi tự động vào file: " + LOG_FILE);
            System.out.println("[" + getTimestamp() + "] [Server] Sẵn sàng phục vụ nhiều Client đồng thời!");
            System.out.println("---------------------------------------------------------------");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                int currentCount = activeClientsCounter.incrementAndGet();

                String clientIp = clientSocket.getInetAddress().getHostAddress();
                int clientPort = clientSocket.getPort();

                logActivity(clientIp, clientPort, "Guest", "CONNECT",
                        String.format("Client mới kết nối! (Tổng Client online: %d)", currentCount));

                // Giao việc cho luồng trong threadPool xử lý
                threadPool.execute(new ClientHandler(clientSocket, activeClientsCounter));
            }
        } catch (IOException e) {
            System.err.printf("[%s] [Server] Lỗi ServerSocket: %s%n", getTimestamp(), e.getMessage());
        } finally {
            threadPool.shutdown();
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
            System.out.println("[" + getTimestamp() + "] [Server] Server đã dừng hoạt động.");
        }
    }
}
