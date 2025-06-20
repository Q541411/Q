// Server.txt
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.sql.*;

public class Server {
    // SQLite数据库配置
    private static final String DB_URL = "jdbc:sqlite:server.db";
    private static final String UPLOAD_DIR = "uploaded_files/";

    public static void main(String[] args) {
        // 加载SQLite驱动
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("SQLite驱动加载成功");
        } catch (ClassNotFoundException e) {
            System.err.println("无法加载SQLite驱动: " + e.getMessage());
            return;
        }

        // 初始化数据库
        initializeDatabase();

        // 创建上传目录
        new File(UPLOAD_DIR).mkdirs();

        int port = 8080;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("服务端已启动，等待客户端连接...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端已连接：" + clientSocket.getInetAddress());

                // 启动线程处理客户端请求
                Thread clientThread = new Thread(() -> handleClientRequest(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("服务端异常：" + e.getMessage());
        }
    }

    /**
     * 初始化数据库
     */
    private static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // 创建消息表
            String sql = "CREATE TABLE IF NOT EXISTS messages (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "content TEXT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            stmt.execute(sql);

            // 创建文件表
            sql = "CREATE TABLE IF NOT EXISTS files (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "filename TEXT NOT NULL," +
                    "filepath TEXT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            stmt.execute(sql);

            System.out.println("数据库初始化完成");
        } catch (SQLException e) {
            System.err.println("数据库初始化失败: " + e.getMessage());
        }
    }

    /**
     * 处理客户端请求
     */
    private static void handleClientRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split("\\|", 3);
                if (parts.length < 3) {
                    out.println("ERROR|Invalid message format");
                    continue;
                }

                String type = parts[0];
                String content = parts[2];

                switch (type) {
                    case "TEXT":
                        handleTextMessage(content, out);
                        break;
                    case "FILE":
                        handleFileMessage(parts[1], content, out);
                        break;
                    default:
                        out.println("ERROR|Unknown message type: " + type);
                }
            }
        } catch (IOException e) {
            System.err.println("处理客户端请求时发生异常：" + e.getMessage());
        }
    }

    /**
     * 处理文本消息（Base64解码后存入数据库）
     */
    private static void handleTextMessage(String encodedContent, PrintWriter out) {
        try {
            // Base64解码
            byte[] decodedBytes = Base64.getDecoder().decode(encodedContent);
            String message = new String(decodedBytes, "UTF-8");

            // 存入数据库
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO messages(content) VALUES(?)")) {

                pstmt.setString(1, message);
                pstmt.executeUpdate();
                out.println("SUCCESS|Message saved: " + message);

            } catch (SQLException e) {
                out.println("ERROR|Database error: " + e.getMessage());
            }
        } catch (IllegalArgumentException | UnsupportedEncodingException e) {
            out.println("ERROR|Invalid Base64 content");
        }
    }

    /**
     * 处理文件消息（Base64解码后存入文件系统并记录到数据库）
     */
    private static void handleFileMessage(String filename, String encodedContent, PrintWriter out) {
        try {
            // Base64解码
            byte[] fileContent = Base64.getDecoder().decode(encodedContent);

            // 确保文件名安全
            String safeFilename = filename.replaceAll("[^a-zA-Z0-9\\._-]", "_");

            // 保存文件
            String filePath = UPLOAD_DIR + safeFilename;
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(fileContent);
            }

            // 存入数据库
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO files(filename, filepath) VALUES(?, ?)")) {

                pstmt.setString(1, safeFilename);
                pstmt.setString(2, filePath);
                pstmt.executeUpdate();
                out.println("SUCCESS|File saved: " + safeFilename);

            } catch (SQLException e) {
                out.println("ERROR|Database error: " + e.getMessage());
            }
        } catch (IOException | IllegalArgumentException e) {
            out.println("ERROR|File processing error: " + e.getMessage());
        }
    }
}