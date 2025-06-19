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
        JButton sendFieldButton = new JButton("保存字段");

        JTextField fieldNameField = new JTextField("字段名");
        JTextField fieldValueField = new JTextField("字段值");

        responseArea = new JTextArea();
        responseArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(responseArea);

        // 布局
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(fieldNameField, BorderLayout.WEST);
        panel.add(fieldValueField, BorderLayout.CENTER);
        panel.add(sendFieldButton, BorderLayout.EAST);

        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(uploadButton, BorderLayout.SOUTH);

        // 按钮点击事件
        sendFieldButton.addActionListener(e -> saveFieldToServer(fieldNameField.getText(), fieldValueField.getText()));
        uploadButton.addActionListener(this::uploadFileToServer);

        // 显示窗口
        frame.setVisible(true);
    }

    /**
     * 向服务端发送字段数据
     */
    private void saveFieldToServer(String fieldName, String fieldValue) {
        String serverAddress = "localhost";
        int port = 8080;

        try (Socket socket = new Socket(serverAddress, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // 发送字段数据到服务端
            out.println("SAVE_FIELD"); // 指令标识
            out.println(fieldName); // 字段名
            out.println(fieldValue); // 字段值

            // 接收服务端响应
            String serverResponse = in.readLine();
            updateResponseArea("服务端响应：" + serverResponse);

        } catch (IOException e) {
            updateResponseArea("发生错误：" + e.getMessage());
        }
    }

    /**
     * 上传文件到服务端
     */
    private void uploadFileToServer(ActionEvent event) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String serverAddress = "localhost";
            int port = 8080;

            try (Socket socket = new Socket(serverAddress, port);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                // 读取文件内容并编码为Base64
                byte[] fileContent = readFileContent(selectedFile);
                String encodedFile = Base64.getEncoder().encodeToString(fileContent);

                // 发送文件名和内容到服务端
                out.println("UPLOAD_FILE"); // 指令标识
                out.println(selectedFile.getName()); // 文件名
                out.println(encodedFile); // 文件内容

                // 接收服务端响应
                String serverResponse = in.readLine();
                updateResponseArea("服务端响应：" + serverResponse);

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