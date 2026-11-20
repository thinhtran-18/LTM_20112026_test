import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Giao diện chính Quản lý sách JavaFX (Hình 3 đề bài).
 * Tích hợp đầy đủ các chức năng cơ bản & các yêu cầu nâng cao:
 * - YC11: Hiển thị tên & vai trò người dùng (ADMIN / USER).
 * - YC12: Chức năng MƯỢN SÁCH & TRẢ SÁCH.
 * - YC13: Thanh thông báo nhận tin thời gian thực qua UDP Socket.
 * - YC15: Bảo toàn tính nhất quán dữ liệu khi mượn/sửa sách đồng thời.
 */
public class BookFrame {
    private static NetworkClient sharedClient;

    public static void showWithClient(NetworkClient client) {
        sharedClient = client;
        Platform.runLater(() -> {
            Stage stage = new Stage();
            new BookApp().start(stage);
        });
    }

    public static class BookApp extends Application {
        private NetworkClient networkClient;
        private TextField txtMaSach;
        private TextField txtTenSach;
        private TextField txtTacGia;
        private TextField txtNamXB;
        private TextField txtSoLuong;

        private Button btnThem;
        private Button btnSua;
        private Button btnXoa;
        private Button btnLamMoi;
        private Button btnTimKiem;

        // Nút nâng cao: Mượn & Trả sách (Yêu cầu 12)
        private Button btnMuonSach;
        private Button btnTraSach;

        // Thanh hiển thị thông báo UDP từ Server (Yêu cầu 13)
        private Label lblUdpNotification;

        private TableView<Book> tblBooks;
        private final ObservableList<Book> bookList = FXCollections.observableArrayList();

        @Override
        @SuppressWarnings("unchecked")
        public void start(Stage primaryStage) {
            this.networkClient = sharedClient;

            // Nếu chạy trực tiếp BookFrame mà chưa qua ConnectFrame
            if (this.networkClient == null || !this.networkClient.isConnected()) {
                try {
                    this.networkClient = new NetworkClient();
                    this.networkClient.login("127.0.0.1", 9999, "sinhvien", "123456");
                    sharedClient = this.networkClient;
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.WARNING, "Chưa kết nối Server",
                            "Chưa thể kết nối tới Server cổng 9999!\nChuyển sang màn hình Đăng nhập...");
                    new ConnectFrame.ConnectApp().start(primaryStage);
                    return;
                }
            }

            String hoTen = networkClient.getHoTen();
            String role = networkClient.getRole();
            primaryStage.setTitle("HỆ THỐNG QUẢN LÝ THƯ VIỆN - " + hoTen + " [" + role + "]");
            primaryStage.setMinWidth(960);
            primaryStage.setMinHeight(680);

            primaryStage.setOnCloseRequest(e -> {
                if (networkClient != null) {
                    networkClient.disconnect();
                }
            });

            VBox root = new VBox(10);
            root.setStyle("-fx-background-color: #f1f5f9;");

            // 1. HEADER (Xanh sẫm #1b4965)
            HBox headerBox = new HBox(16);
            headerBox.setAlignment(Pos.CENTER_LEFT);
            headerBox.setPadding(new Insets(8, 20, 8, 20));
            headerBox.setStyle("-fx-background-color: #1b4965;");

            VBox titleCol = new VBox(2);
            titleCol.setAlignment(Pos.CENTER_LEFT);

            Label lblTitle = new Label("HỆ THỐNG QUẢN LÝ THƯ VIỆN QUA MẠNG");
            lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
            lblTitle.setTextFill(Color.WHITE);

            Label lblUserBadge = new Label("Xin chào: " + hoTen + " (" + networkClient.getUsername() + " - " + role + ")");
            lblUserBadge.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
            lblUserBadge.setTextFill(Color.web("#a5f3fc"));

            titleCol.getChildren().addAll(lblTitle, lblUserBadge);

            Region headerSpacer = new Region();
            HBox.setHgrow(headerSpacer, Priority.ALWAYS);

            Button btnLogoutTop = new Button("🚪 ĐĂNG XUẤT");
            btnLogoutTop.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            btnLogoutTop.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6px; -fx-padding: 7px 16px; -fx-cursor: hand;");
            btnLogoutTop.setOnMouseEntered(e -> btnLogoutTop.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-background-radius: 6px; -fx-padding: 7px 16px; -fx-cursor: hand;"));
            btnLogoutTop.setOnMouseExited(e -> btnLogoutTop.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6px; -fx-padding: 7px 16px; -fx-cursor: hand;"));
            btnLogoutTop.setOnAction(e -> doLogout(primaryStage));

            headerBox.getChildren().addAll(titleCol, headerSpacer, btnLogoutTop);

            // 2. THANH THÔNG BÁO UDP TỪ SERVER (Yêu cầu nâng cao 13)
            HBox notifBar = new HBox(8);
            notifBar.setAlignment(Pos.CENTER_LEFT);
            notifBar.setPadding(new Insets(6, 16, 6, 16));
            notifBar.setStyle("-fx-background-color: #e0e7ff; -fx-border-color: #c7d2fe; -fx-border-radius: 6px; -fx-background-radius: 6px;");
            VBox.setMargin(notifBar, new Insets(0, 14, 0, 14));

            lblUdpNotification = new Label("🔔 Thông báo Server (UDP): Sẵn sàng nhận thông báo thời gian thực.");
            lblUdpNotification.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            lblUdpNotification.setTextFill(Color.web("#3730a3"));
            notifBar.getChildren().add(lblUdpNotification);

            // Lắng nghe thông báo UDP từ Server: cập nhật banner và tự động tải lại bảng sách
            networkClient.setNotificationCallback(msg -> Platform.runLater(() -> {
                lblUdpNotification.setText("🔔 [UDP Broadcast] " + msg);
                loadAllBooksSilently();
            }));

            // 3. THẺ QUẢN LÝ SÁCH & CÁC NÚT THAO TÁC
            VBox cardPanel = new VBox(10);
            cardPanel.setPadding(new Insets(12, 18, 14, 18));
            cardPanel.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8px; "
                    + "-fx-border-color: #cbd5e1; -fx-border-radius: 8px; -fx-border-width: 1px;");
            VBox.setMargin(cardPanel, new Insets(0, 14, 0, 14));

            Label lblSubTitle = new Label("QUẢN LÝ SÁCH & DỊCH VỤ THƯ VIỆN");
            lblSubTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
            lblSubTitle.setTextFill(Color.web("#1b4965"));

            // Hàng 5 ô nhập liệu
            HBox formGrid = new HBox(12);
            formGrid.setAlignment(Pos.CENTER_LEFT);

            txtMaSach = createTextField("Mã sách (Sxxx)");
            txtTenSach = createTextField("Tên sách");
            txtTacGia = createTextField("Tác giả");
            txtNamXB = createTextField("Năm XB");
            txtSoLuong = createTextField("Số lượng");

            formGrid.getChildren().addAll(
                    createFieldBox("Mã sách", txtMaSach, 110),
                    createFieldBox("Tên sách", txtTenSach, 260),
                    createFieldBox("Tác giả", txtTacGia, 200),
                    createFieldBox("Năm XB", txtNamXB, 100),
                    createFieldBox("Số lượng", txtSoLuong, 100)
            );

            // Hàng các nút bấm chức năng (Cơ bản + Nâng cao mượn/trả sách)
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER);
            buttonBox.setPadding(new Insets(8, 0, 2, 0));

            btnThem = createStyledButton("THÊM", "#059669", "#047857");        // Xanh lá (Emerald)
            btnSua = createStyledButton("SỬA", "#2563eb", "#1d4ed8");          // Xanh dương (Royal Blue)
            btnXoa = createStyledButton("XÓA", "#dc2626", "#b91c1c");          // Đỏ (Crimson Red)
            btnLamMoi = createStyledButton("LÀM MỚI", "#64748b", "#475569");   // Xám ghi (Slate Gray)
            btnTimKiem = createStyledButton("TÌM KIẾM", "#d97706", "#b45309"); // Cam hổ phách (Amber)

            // Nút nâng cao: Mượn & Trả sách (Yêu cầu 12)
            btnMuonSach = createStyledButton("📖 MƯỢN SÁCH", "#4f46e5", "#4338ca"); // Tím Indigo
            btnTraSach = createStyledButton("↩ TRẢ SÁCH", "#0d9488", "#0f766e");    // Xanh Teal

            // Nút Đăng xuất trên thanh công cụ
            Button btnDangXuatToolbar = createStyledButton("🚪 ĐĂNG XUẤT", "#ef4444", "#dc2626"); // Đỏ tươi
            btnDangXuatToolbar.setOnAction(e -> doLogout(primaryStage));

            buttonBox.getChildren().addAll(btnThem, btnSua, btnXoa, btnLamMoi, btnTimKiem, btnMuonSach, btnTraSach, btnDangXuatToolbar);

            cardPanel.getChildren().addAll(lblSubTitle, formGrid, buttonBox);

            // 4. BẢNG DỮ LIỆU SÁCH (TableView)
            tblBooks = new TableView<>();
            tblBooks.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            tblBooks.setStyle("-fx-font-size: 13px; -fx-selection-bar: #dbeafe; -fx-selection-bar-non-focused: #eff6ff;");
            VBox.setVgrow(tblBooks, Priority.ALWAYS);
            VBox.setMargin(tblBooks, new Insets(0, 14, 14, 14));

            // Cột STT
            TableColumn<Book, Void> colStt = new TableColumn<>("STT");
            colStt.setPrefWidth(55);
            colStt.setStyle("-fx-alignment: CENTER;");
            colStt.setCellFactory(col -> new TableCell<Book, Void>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() >= getTableView().getItems().size()) {
                        setText(null);
                    } else {
                        setText(String.valueOf(getIndex() + 1));
                    }
                }
            });

            // Cột Mã sách
            TableColumn<Book, String> colMa = new TableColumn<>("Mã sách");
            colMa.setPrefWidth(100);
            colMa.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
            colMa.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMaSach()));

            // Cột Tên sách
            TableColumn<Book, String> colTen = new TableColumn<>("Tên sách");
            colTen.setPrefWidth(260);
            colTen.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTenSach()));

            // Cột Tác giả
            TableColumn<Book, String> colTacGia = new TableColumn<>("Tác giả");
            colTacGia.setPrefWidth(190);
            colTacGia.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTacGia()));

            // Cột Năm XB
            TableColumn<Book, Integer> colNamXB = new TableColumn<>("Năm XB");
            colNamXB.setPrefWidth(90);
            colNamXB.setStyle("-fx-alignment: CENTER;");
            colNamXB.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getNamXB()).asObject());

            // Cột Số lượng
            TableColumn<Book, Integer> colSoLuong = new TableColumn<>("Số lượng");
            colSoLuong.setPrefWidth(90);
            colSoLuong.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
            colSoLuong.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getSoLuong()).asObject());

            tblBooks.getColumns().addAll(colStt, colMa, colTen, colTacGia, colNamXB, colSoLuong);
            tblBooks.setItems(bookList);

            // Bắt sự kiện chọn dòng trên bảng -> đổ dữ liệu lên form
            tblBooks.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    txtMaSach.setText(newVal.getMaSach());
                    txtTenSach.setText(newVal.getTenSach());
                    txtTacGia.setText(newVal.getTacGia());
                    txtNamXB.setText(String.valueOf(newVal.getNamXB()));
                    txtSoLuong.setText(String.valueOf(newVal.getSoLuong()));
                }
            });

            // Gắn sự kiện các nút
            btnThem.setOnAction(e -> doThem());
            btnSua.setOnAction(e -> doSua());
            btnXoa.setOnAction(e -> doXoa());
            btnLamMoi.setOnAction(e -> doLamMoi());
            btnTimKiem.setOnAction(e -> doTimKiem());
            btnMuonSach.setOnAction(e -> doMuonSach());
            btnTraSach.setOnAction(e -> doTraSach());

            root.getChildren().addAll(headerBox, notifBar, cardPanel, tblBooks);

            Scene scene = new Scene(root, 960, 680);
            primaryStage.setScene(scene);
            primaryStage.show();

            loadAllBooks();
        }

        private VBox createFieldBox(String labelText, TextField tf, double prefWidth) {
            VBox box = new VBox(4);
            Label lbl = new Label(labelText);
            lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            lbl.setTextFill(Color.web("#334155"));
            tf.setPrefWidth(prefWidth);
            box.getChildren().addAll(lbl, tf);
            return box;
        }

        private TextField createTextField(String prompt) {
            TextField tf = new TextField();
            tf.setPromptText(prompt);
            tf.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cbd5e1; -fx-border-radius: 6px; "
                    + "-fx-background-radius: 6px; -fx-padding: 7px 10px; -fx-font-size: 13px;");
            return tf;
        }

        private Button createStyledButton(String text, String bgColor, String hoverColor) {
            Button btn = new Button(text);
            btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            btn.setPrefHeight(34);
            btn.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: #ffffff; -fx-font-weight: bold; "
                    + "-fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6px 14px;");

            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + hoverColor + "; -fx-text-fill: #ffffff; "
                    + "-fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6px 14px;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: #ffffff; "
                    + "-fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6px 14px;"));
            return btn;
        }

        private void loadAllBooks() {
            if (networkClient == null || !networkClient.isConnected()) return;

            new Thread(() -> {
                try {
                    List<Book> list = networkClient.getAllBooks();
                    Platform.runLater(() -> bookList.setAll(list));
                } catch (IOException e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối",
                            "Mất kết nối đến Server:\n" + e.getMessage()));
                }
            }).start();
        }

        private void loadAllBooksSilently() {
            if (networkClient == null || !networkClient.isConnected()) return;
            new Thread(() -> {
                try {
                    List<Book> list = networkClient.getAllBooks();
                    Platform.runLater(() -> bookList.setAll(list));
                } catch (IOException ignored) {}
            }).start();
        }

        // =====================================================================
        // YÊU CẦU NÂNG CAO 12 & 15: MƯỢN SÁCH & TRẢ SÁCH
        // =====================================================================

        private void doMuonSach() {
            String maSach = txtMaSach.getText().trim();
            if (maSach.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng chọn cuốn sách cần mượn từ danh sách!");
                txtMaSach.requestFocus();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Xác nhận mượn sách");
            confirm.setHeaderText(null);
            confirm.setContentText("Bạn có muốn mượn cuốn sách [" + maSach + "] không?");
            Optional<ButtonType> res = confirm.showAndWait();

            if (res.isPresent() && res.get() == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        String err = networkClient.borrowBook(maSach);
                        Platform.runLater(() -> {
                            if (err == null) {
                                showAlert(Alert.AlertType.INFORMATION, "Mượn sách thành công",
                                        "Đã ghi nhận mượn cuốn sách [" + maSach + "]. Số lượng kho đã được cập nhật!");
                                loadAllBooks();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi mượn sách", err);
                            }
                        });
                    } catch (IOException ex) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi mạng", ex.getMessage()));
                    }
                }).start();
            }
        }

        private void doTraSach() {
            String maSach = txtMaSach.getText().trim();
            if (maSach.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng chọn cuốn sách muốn trả từ danh sách!");
                txtMaSach.requestFocus();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Xác nhận trả sách");
            confirm.setHeaderText(null);
            confirm.setContentText("Bạn có chắc chắn muốn trả cuốn sách [" + maSach + "] về thư viện?");
            Optional<ButtonType> res = confirm.showAndWait();

            if (res.isPresent() && res.get() == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        String err = networkClient.returnBook(maSach);
                        Platform.runLater(() -> {
                            if (err == null) {
                                showAlert(Alert.AlertType.INFORMATION, "Trả sách thành công",
                                        "Đã hoàn trả cuốn sách [" + maSach + "] về thư viện. Số lượng kho đã được cộng lại!");
                                loadAllBooks();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi trả sách", err);
                            }
                        });
                    } catch (IOException ex) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi mạng", ex.getMessage()));
                    }
                }).start();
            }
        }

        // =====================================================================
        // CÁC CHỨC NĂNG CƠ BẢN (THÊM, SỬA, XÓA, LÀM MỚI, TÌM KIẾM)
        // =====================================================================

        private void doThem() {
            Book b = validateAndGetBook();
            if (b == null) return;

            new Thread(() -> {
                try {
                    String err = networkClient.addBook(b);
                    Platform.runLater(() -> {
                        if (err == null) {
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Thêm sách mới [" + b.getMaSach() + "] thành công!");
                            clearFields();
                            loadAllBooks();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Lỗi thêm sách", err);
                        }
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", e.getMessage()));
                }
            }).start();
        }

        private void doSua() {
            Book b = validateAndGetBook();
            if (b == null) return;

            new Thread(() -> {
                try {
                    String err = networkClient.updateBook(b);
                    Platform.runLater(() -> {
                        if (err == null) {
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Cập nhật sách [" + b.getMaSach() + "] thành công!");
                            loadAllBooks();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Lỗi cập nhật sách", err);
                        }
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", e.getMessage()));
                }
            }).start();
        }

        private void doXoa() {
            String ma = txtMaSach.getText().trim();
            if (ma.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng chọn hoặc nhập mã sách cần xóa!");
                txtMaSach.requestFocus();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Xác nhận xóa");
            confirm.setHeaderText(null);
            confirm.setContentText("Bạn có chắc chắn muốn xóa cuốn sách có mã: " + ma + " không?");
            Optional<ButtonType> result = confirm.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        String err = networkClient.deleteBook(ma);
                        Platform.runLater(() -> {
                            if (err == null) {
                                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Xóa sách [" + ma + "] thành công!");
                                clearFields();
                                loadAllBooks();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi xóa sách", err);
                            }
                        });
                    } catch (IOException e) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", e.getMessage()));
                    }
                }).start();
            }
        }

        private void doLamMoi() {
            clearFields();
            loadAllBooks();
        }

        private void doTimKiem() {
            String keyword = txtMaSach.getText().trim();
            if (keyword.isEmpty()) {
                keyword = txtTenSach.getText().trim();
            }

            if (keyword.isEmpty()) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Tìm kiếm sách");
                dialog.setHeaderText(null);
                dialog.setContentText("Nhập mã sách hoặc tên sách cần tìm:");
                Optional<String> res = dialog.showAndWait();
                if (!res.isPresent()) return;
                keyword = res.get().trim();
            }

            final String searchKey = keyword;
            new Thread(() -> {
                try {
                    List<Book> list = networkClient.searchBooks(searchKey);
                    Platform.runLater(() -> {
                        bookList.setAll(list);
                        if (list.isEmpty()) {
                            showAlert(Alert.AlertType.INFORMATION, "Kết quả tìm kiếm",
                                    "Không tìm thấy cuốn sách nào khớp với từ khóa: '" + searchKey + "'!");
                        }
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", e.getMessage()));
                }
            }).start();
        }

        private void clearFields() {
            txtMaSach.clear();
            txtTenSach.clear();
            txtTacGia.clear();
            txtNamXB.clear();
            txtSoLuong.clear();
            tblBooks.getSelectionModel().clearSelection();
            txtMaSach.requestFocus();
        }

        private Book validateAndGetBook() {
            String ma = txtMaSach.getText().trim();
            String ten = txtTenSach.getText().trim();
            String tg = txtTacGia.getText().trim();
            String namStr = txtNamXB.getText().trim();
            String slStr = txtSoLuong.getText().trim();

            if (ma.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập mã sách!");
                txtMaSach.requestFocus();
                return null;
            }
            if (ten.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập tên sách!");
                txtTenSach.requestFocus();
                return null;
            }
            if (tg.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập tác giả!");
                txtTacGia.requestFocus();
                return null;
            }

            int nam;
            try {
                nam = Integer.parseInt(namStr);
                if (nam < 0 || nam > 2100) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Năm xuất bản phải là số nguyên hợp lệ!");
                txtNamXB.requestFocus();
                return null;
            }

            int sl;
            try {
                sl = Integer.parseInt(slStr);
                if (sl < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Số lượng phải là số nguyên không âm (>= 0)!");
                txtSoLuong.requestFocus();
                return null;
            }

            return new Book(ma, ten, tg, nam, sl);
        }

        private void doLogout(Stage primaryStage) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Xác nhận đăng xuất");
            confirm.setHeaderText(null);
            confirm.setContentText("Bạn có chắc chắn muốn đăng xuất tài khoản '" + (networkClient != null ? networkClient.getUsername() : "") + "'?");
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                if (networkClient != null) {
                    networkClient.disconnect();
                }
                sharedClient = null;
                primaryStage.close();
                ConnectFrame.show();
            }
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
        Application.launch(BookApp.class, args);
    }
}
