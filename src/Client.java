// Client.txt
import java.io.*;
import java.net.Socket;
import java.util.Base64;

public class Client {
    private static volatile boolean running = true;

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 8080;

        try (Socket socket = new Socket(serverAddress, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("已连接到服务端，输入消息发送：");

            // 启动工作线程：接收服务端响应
            Thread responseThread = new Thread(() -> {
                try {
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        System.out.println("\n[服务端响应]：" + serverResponse);
                        System.out.print("请输入消息：");
                    }
                } catch (IOException e) {
                    if (running) {
                        System.err.println("工作线程异常：" + e.getMessage());
                    }
                }
            }, "Response-Thread");
            responseThread.start();

            // 主线程：处理用户输入
            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                if ("sendfile".equalsIgnoreCase(userInput)) {
                    sendFile(out);
                } else {
                    // 普通消息Base64编码
                    String encodedMsg = Base64.getEncoder().encodeToString(userInput.getBytes());
                    out.println("TEXT|" + encodedMsg);
                }

                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("客户端异常：" + e.getMessage());
        } finally {
            running = false;
        }
    }

    /**
     * 发送文件到服务端（使用Base64编码）
     */
    private static void sendFile(PrintWriter out) throws IOException {
        System.out.print("请输入文件路径：");
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String filePath = stdIn.readLine();

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("文件不存在或路径无效！");
            return;
        }

        try (InputStream fileIn = new FileInputStream(file)) {
            byte[] fileBytes = fileIn.readAllBytes();
            String encodedFile = Base64.getEncoder().encodeToString(fileBytes);

            // 发送文件信息：类型|文件名|Base64内容
            out.println("FILE|" + file.getName() + "|" + encodedFile);
            System.out.println("文件发送完成：" + file.getName());
        }
    }
}