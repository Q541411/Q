import java.io.*;
import java.net.Socket;

public class Client {
    private static volatile boolean running = true; // 控制线程运行状态

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
                        System.out.print("请输入消息："); // 提示用户继续输入
                    }
                } catch (IOException e) {
                    if (running) { // 只有在非正常关闭时打印异常
                        System.err.println("工作线程异常：" + e.getMessage());
                    }
                }
            }, "Response-Thread"); // 设置线程名称
            responseThread.start();

            // 主线程：处理用户输入
            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                if ("sendfile".equalsIgnoreCase(userInput)) {
                    // 文件传输命令
                    sendFile(socket);
                } else {
                    out.println(userInput); // 发送普通消息到服务端
                }

                if ("exit".equalsIgnoreCase(userInput)) {
                    break; // 输入 "exit" 退出
                }
            }

        } catch (IOException e) {
            System.err.println("客户端异常：" + e.getMessage());
        } finally {
            running = false; // 停止工作线程
        }
    }

    /**
     * 发送文件到服务端
     */
    private static void sendFile(Socket socket) throws IOException {
        System.out.print("请输入文件路径：");
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String filePath = stdIn.readLine();

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("文件不存在或路径无效！");
            return;
        }

        // 发送文件元信息（文件名和文件大小）
        OutputStream out = socket.getOutputStream();
        String fileInfo = "FILE|" + file.getName() + "|" + file.length();
        out.write((fileInfo + "\n").getBytes()); // 发送文件信息并换行
        out.flush();

        // 发送文件内容
        try (InputStream fileIn = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }

        System.out.println("文件发送完成：" + file.getName());
    }
}