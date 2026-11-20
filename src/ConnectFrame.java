import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Giao diện kết nối / đăng nhập JavaFX (Hình 2 đề thi + Yêu cầu nâng cao 11).
 * Hỗ trợ xác thực tài khoản & mật khẩu qua Server, chọn nhanh tài khoản để test nhiều Client.
 */
public class ConnectFrame {

    public static void show() {
        Platform.runLater(() -> {
            Stage stage = new Stage();
            new ConnectApp().start(stage);
        });
    }

    public static class ConnectApp extends Application {
        private TextField txtIp;
        private TextField txtPort;
        private TextField txtUsername;
        private PasswordField txtPassword;
        private Button btnConnect;

        @Override
        public void start(Stage primaryStage) {
            primaryStage.setTitle("Kết nối hệ thống thư viện");
            primaryStage.setResizable(false);

            VBox root = new VBox();
            root.setStyle("-fx-background-color: #f8fafc;");
            root.setPrefWidth(450);
            root.setPrefHeight(510);

            // 1. Thanh tiêu đề Header xanh đậm (#1b4965)
            VBox headerBox = new VBox();
            headerBox.setAlignment(Pos.CENTER);
            headerBox.setPrefHeight(60);
            headerBox.setStyle("-fx-background-color: #1b4965;");

            Label lblTitle = new Label("KẾT NỐI HỆ THỐNG THƯ VIỆN");
            lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
            lblTitle.setTextFill(Color.WHITE);
            headerBox.getChildren().add(lblTitle);

            // 2. Thẻ Form nhập liệu
            VBox formCard = new VBox(10);
            formCard.setPadding(new Insets(18, 30, 20, 30));
            formCard.setAlignment(Pos.CENTER_LEFT);

            // Địa chỉ IP Server
            Label lblIp = new Label("Địa chỉ IP Server");
            lblIp.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            lblIp.setTextFill(Color.web("#334155"));
            txtIp = new TextField("127.0.0.1");
            styleInput(txtIp);

            // Cổng kết nối
            Label lblPort = new Label("Cổng kết nối (TCP)");
            lblPort.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            lblPort.setTextFill(Color.web("#334155"));
            txtPort = new TextField("9999");
            styleInput(txtPort);

            // Tên đăng nhập (Yêu cầu 11)
            Label lblUser = new Label("Tên đăng nhập");
            lblUser.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            lblUser.setTextFill(Color.web("#334155"));
            txtUsername = new TextField("sinhvien");
            styleInput(txtUsername);

            // Hàng nút chọn nhanh tài khoản test (Cực kỳ tiện lợi khi test 2 Client song song)
            HBox quickAccountBox = new HBox(6);
            quickAccountBox.setAlignment(Pos.CENTER_LEFT);
            Label lblQuick = new Label("Chọn nhanh:");
            lblQuick.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
            lblQuick.setTextFill(Color.web("#64748b"));

            Button btnAccSinhvien = createQuickAccBtn("sinhvien", "123456", "sinhvien");
            Button btnAccSinhvien1 = createQuickAccBtn("sinhvien1", "123456", "sinhvien1");
            Button btnAccSinhvien2 = createQuickAccBtn("sinhvien2", "123456", "sinhvien2");
            Button btnAccAdmin = createQuickAccBtn("admin", "admin123", "admin");

            quickAccountBox.getChildren().addAll(lblQuick, btnAccSinhvien, btnAccSinhvien1, btnAccSinhvien2, btnAccAdmin);

            // Mật khẩu (Yêu cầu 11)
            Label lblPass = new Label("Mật khẩu");
            lblPass.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            lblPass.setTextFill(Color.web("#334155"));
            txtPassword = new PasswordField();
            txtPassword.setText("123456");
            styleInput(txtPassword);

            // Nút KẾT NỐI & ĐĂNG NHẬP
            btnConnect = new Button("KẾT NỐI & ĐĂNG NHẬP");
            btnConnect.setMaxWidth(Double.MAX_VALUE);
            btnConnect.setStyle("-fx-background-color: #2563eb; -fx-text-fill: #ffffff; -fx-font-weight: bold; "
                    + "-fx-font-size: 13px; -fx-padding: 10px 0; -fx-background-radius: 6px; -fx-cursor: hand;");

            btnConnect.setOnMouseEntered(e -> btnConnect.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: #ffffff; "
                    + "-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10px 0; -fx-background-radius: 6px; -fx-cursor: hand;"));
            btnConnect.setOnMouseExited(e -> btnConnect.setStyle("-fx-background-color: #2563eb; -fx-text-fill: #ffffff; "
                    + "-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10px 0; -fx-background-radius: 6px; -fx-cursor: hand;"));

            VBox.setMargin(btnConnect, new Insets(10, 0, 0, 0));

            // Phím tắt Enter
            btnConnect.setOnAction(e -> doConnect(primaryStage));
            txtPassword.setOnAction(e -> doConnect(primaryStage));
            txtUsername.setOnAction(e -> doConnect(primaryStage));
            txtPort.setOnAction(e -> doConnect(primaryStage));
            txtIp.setOnAction(e -> doConnect(primaryStage));

            formCard.getChildren().addAll(lblIp, txtIp, lblPort, txtPort, lblUser, txtUsername, quickAccountBox, lblPass, txtPassword, btnConnect);
            VBox.setVgrow(formCard, Priority.ALWAYS);

            root.getChildren().addAll(headerBox, formCard);

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.show();
        }

        private Button createQuickAccBtn(String user, String pass, String label) {
            Button btn = new Button(label);
            btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            btn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #1e293b; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #cbd5e1; -fx-text-fill: #0f172a; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-cursor: hand;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #1e293b; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-cursor: hand;"));
            btn.setOnAction(e -> {
                txtUsername.setText(user);
                txtPassword.setText(pass);
            });
            return btn;
        }

        private void styleInput(TextField tf) {
            tf.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cbd5e1; -fx-border-radius: 6px; "
                    + "-fx-background-radius: 6px; -fx-padding: 7px 10px; -fx-font-size: 13px;");
        }

        private void doConnect(Stage currentStage) {
            String ip = txtIp.getText().trim();
            String portStr = txtPort.getText().trim();
            String username = txtUsername.getText().trim();
            String password = txtPassword.getText().trim();

            if (ip.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập địa chỉ IP Server!");
                txtIp.requestFocus();
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portStr);
                if (port < 1 || port > 65535) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Cổng kết nối phải là số nguyên từ 1 đến 65535!");
                txtPort.requestFocus();
                return;
            }

            if (username.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập tên đăng nhập!");
                txtUsername.requestFocus();
                return;
            }

            btnConnect.setDisable(true);
            btnConnect.setText("ĐANG XÁC THỰC...");

            new Thread(() -> {
                try {
                    NetworkClient client = new NetworkClient();
                    // Đăng nhập và xác thực tài khoản qua Server (Yêu cầu nâng cao 11)
                    client.login(ip, port, username, password);

                    Platform.runLater(() -> {
                        currentStage.close();
                        BookFrame.showWithClient(client);
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        btnConnect.setDisable(false);
                        btnConnect.setText("KẾT NỐI & ĐĂNG NHẬP");
                        showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại", ex.getMessage());
                    });
                }
            }).start();
        }

        private void showAlert(Alert.AlertType type, String title, String content) {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        Application.launch(ConnectApp.class, args);
    }
}
