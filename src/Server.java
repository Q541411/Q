import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.sql.*;

public class Server {
    public static void main(String[] args) {
        int port = 8080;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("服务端已启动，等待客户端连接...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleClientRequest(clientSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理客户端请求
     */
    private static void handleClientRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String command = in.readLine(); // 读取指令标识

            if ("UPLOAD_FILE".equals(command)) {
                String fileName = in.readLine(); // 读取文件名
                String encodedFile = in.readLine(); // 读取Base64编码的文件内容

                // 接收并保存文件
                receiveFile(fileName, encodedFile);

                // 响应客户端
                out.println("文件上传成功！");
            } else if ("SAVE_FIELD".equals(command)) {
                String fieldName = in.readLine(); // 读取字段名
                String fieldValue = in.readLine(); // 读取字段值

                // 将字段数据存储到SQLite数据库
                saveToDatabase(fieldName, fieldValue);

                // 响应客户端
                out.println("字段数据保存成功！");
            } else {
                out.println("未知指令！");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收并保存文件
     */
    private static void receiveFile(String fileName, String encodedFile) throws IOException {
        // 对接收到的 Base64 数据进行解码
        byte[] decodedFile = Base64.getDecoder().decode(encodedFile);

        // 将解码后的数据写入文件
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            fos.write(decodedFile);
        }
    }

    /**
     * 将字段数据存储到 SQLite 数据库
     */
    private static void saveToDatabase(String fieldName, String fieldValue) {
        String url = "jdbc:sqlite:database.db";
        String sql = "INSERT INTO fields (name, value) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 创建表（如果不存在）
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS fields (name TEXT, value TEXT)");
            }

            // 插入数据
            pstmt.setString(1, fieldName);
            pstmt.setString(2, fieldValue);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}