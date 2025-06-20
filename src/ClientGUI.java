// ClientGUI.txt
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.util.Base64;

public class ClientGUI {
    private JTextArea responseArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }

    public ClientGUI() {
        // 创建主窗口
        JFrame frame = new JFrame("TCP 客户端");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        // 创建组件
        JButton uploadButton = new JButton("上传文件");
        JButton sendTextButton = new JButton("发送文本");

        JTextField inputField = new JTextField();
        inputField.setPreferredSize(new Dimension(300, 30));

        responseArea = new JTextArea();
        responseArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(responseArea);

        // 布局
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendTextButton, BorderLayout.EAST);

        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(uploadButton, BorderLayout.SOUTH);

        // 按钮点击事件
        sendTextButton.addActionListener(e -> sendMessageToServer(inputField.getText()));
        uploadButton.addActionListener(this::uploadFileToServer);

        // 显示窗口
        frame.setVisible(true);
    }

    /**
     * 向服务端发送文本消息（Base64编码）
     */
    private void sendMessageToServer(String message) {
        if (!message.isEmpty()) {
            String serverAddress = "localhost";
            int port = 8080;

            try (Socket socket = new Socket(serverAddress, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                // 编码消息并发送
                String encodedMsg = Base64.getEncoder().encodeToString(message.getBytes());
                out.println("TEXT|" + encodedMsg);
                updateResponseArea("已发送消息: " + message);

            } catch (IOException e) {
                updateResponseArea("发生错误：" + e.getMessage());
            }
        }
    }

    /**
     * 上传文件到服务端（Base64编码）
     */
    private void uploadFileToServer(ActionEvent event) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String serverAddress = "localhost";
            int port = 8080;

            try (Socket socket = new Socket(serverAddress, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                // 读取文件内容并编码为Base64
                byte[] fileContent = readFileContent(selectedFile);
                String encodedFile = Base64.getEncoder().encodeToString(fileContent);

                // 发送文件信息：类型|文件名|Base64内容
                out.println("FILE|" + selectedFile.getName() + "|" + encodedFile);
                updateResponseArea("已上传文件: " + selectedFile.getName());

            } catch (IOException e) {
                updateResponseArea("发生错误：" + e.getMessage());
            }
        }
    }

    /**
     * 读取文件内容为字节数组
     */
    private byte[] readFileContent(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes();
        }
    }

    /**
     * 更新响应区域内容
     */
    private void updateResponseArea(String text) {
        responseArea.append(text + "\n");
    }
}