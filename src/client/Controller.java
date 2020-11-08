package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Controller {
    @FXML
    VBox leftPanel, rightPanel;

    @FXML
    TextField logField, passField;


    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    private InnerPanelController leftPC;
    private ExternalPanelController rightPC;
    private static Socket socket;
    private Stage logIn;


    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void btnConnect(ActionEvent actionEvent) {
        rightPC = (ExternalPanelController) rightPanel.getProperties().get("ctrl");
        try {
            throwForm();

            socket = new Socket("localhost", 8189);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

//            while (true){
//            if (in.readUTF().equals("authOK")) break;
//            }

//            String rootFolder = in.readUTF();
//            rightPC.updateTable(rootFolder);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to connect to server", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void throwForm() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("form.fxml"));
            logIn = new Stage();
            logIn.setTitle("Log in");
            logIn.setScene(new Scene(root, 200, 100));
            logIn.show();
        } catch (IOException e) {
            System.out.println("Something is wrong");
        }
    }


    public void authBtnAction() {
//        rightPC = (ExternalPanelController) rightPanel.getProperties().get("ctrl");
        String log = logField.getText();
        String pass = passField.getText();
        String rootFolder = null;
        try {
            out.writeUTF("auth " + log + " " + pass);
            out.flush();
            System.out.println(" out.writeUTF: " + "auth " + log + " " + pass);
            rootFolder = in.readUTF();
            System.out.println("rootFolder: " + rootFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        rightPC.updateTable(rootFolder);
        logIn.close();
    }


    public List<FileInfo> getList(String path) {
        String command = "info" + path;
        try {
            out.writeUTF(command);
            out.flush();
        } catch (IOException | NullPointerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Some communication problems.", ButtonType.OK);
            alert.showAndWait();
        }
        List<FileInfo> list;
        try {
            list = (List<FileInfo>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            list = new ArrayList<>();
            list.add(new FileInfo("NA", FileInfo.FileType.FILE, 0L, LocalDateTime.MIN));
        }
        return list;
    }

    //todo заменить кнопку на кнопку регистрации
    public void btnCheckConnect(ActionEvent actionEvent) {
        try {
            out.writeUTF("Hello");
            out.flush();
            String answer = in.readUTF();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, answer, ButtonType.OK);
            alert.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Со связью проблема", ButtonType.OK);
            alert.showAndWait();
        }
    }


    public void copyBtnAction(ActionEvent actionEvent) {
        leftPC = (InnerPanelController) leftPanel.getProperties().get("ctrl");

        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        //копирования с клиента на сервер
        if (leftPC.getSelectedFilename() != null) {
            try {
                out.writeUTF("upload");
                out.flush();
                Path path = Paths.get(rightPC.pathField.getText() + "/" + leftPC.getSelectedFilename());
//                System.out.println(path.toString());
                out.writeUTF(path.toString());
                out.flush();
                File file = new File(leftPC.getCurrentPath(), leftPC.getSelectedFilename());
                long length = file.length();
                out.writeLong(length);
                FileInputStream fileBytes = new FileInputStream(file);
                int read = 0;
                byte[] buffer = new byte[256];
                while ((read = fileBytes.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                fileBytes.close();
                out.flush();
//                System.out.println("before readUTF");
//                String msg = in.readUTF();
//                System.out.println(msg);
//                System.out.println("After readUTF");
                Alert alert = new Alert(Alert.AlertType.INFORMATION, in.readUTF(), ButtonType.OK);
                alert.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
            }
            rightPC.updateTable(rightPC.pathField.getText());
        }

        //копирования с сервера на клиента
        //todo отработать случай уже существования файла
        //todo отработать случай копирования директории
        if (rightPC.getSelectedFilename() != null) {
//            System.out.println("in needed IF block");
            try {
                out.writeUTF("download");
//                System.out.println("out.write command: OK");
                out.flush();
                out.writeUTF(rightPC.pathField.getText() + "/" + rightPC.getSelectedFilename());
//                System.out.println("out.write path: " + rightPC.pathField.getText() + "/" + rightPC.getSelectedFilename());
                out.flush();
                File file = new File(leftPC.pathField.getText() + "/" + rightPC.getSelectedFilename());
//                System.out.println("filename: " + leftPC.pathField.getText() + "/" +  rightPC.getSelectedFilename());
                if (!file.exists()) {
                    file.createNewFile();
                }
                long size = in.readLong();
//                System.out.println("size: " + size);
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buffer = new byte[256];
                for (int i = 0; i < (size + 255) / 256; i++) {
                    int read = in.read(buffer);
                    fos.write(buffer, 0, read);
                }
                fos.close();
//                System.out.println("fos.close()");
            } catch (Exception e) {
                e.printStackTrace();
            }
//            System.out.println("path for update: " + Paths.get(leftPC.pathField.getText()));
            leftPC.updateLeftList(Paths.get(leftPC.pathField.getText()));
        }

    }

    // todo надо учесть работу с папками
    public void moveBtnAction(ActionEvent actionEvent) {
//        leftPC  = (InnerPanelController) leftPanel.getProperties().get("ctrl");
//
//        //перемещение с клиента на сервер
//        if (leftPC.getSelectedFilename() != null) {
//            copyBtnAction(actionEvent);
//            File file = new File(leftPC.pathField.getText() + "/" + leftPC.getSelectedFilename());
//            file.delete();
//            leftPC.updateLeftList(Paths.get(leftPC.pathField.getText()));
//        }
//        //перемещение с сервера на клиент
//        if (rightPC.getSelectedFilename() != null) {
//            copyBtnAction(actionEvent);
//            try {
//                out.writeUTF("delete");
//                out.flush();
//                out.writeUTF(rightPC.pathField.getText() + "/" + rightPC.getSelectedFilename());
//                out.flush();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            rightPC.updateTable(rightPC.pathField.getText());
//        }
        copyBtnAction(actionEvent);
        deleteBtnAction(actionEvent);
    }

    // todo надо учесть работу с папками
    public void deleteBtnAction(ActionEvent actionEvent) {
        leftPC = (InnerPanelController) leftPanel.getProperties().get("ctrl");
        //удаление у клиента
        if (leftPC.getSelectedFilename() != null) {
            File file = new File(leftPC.pathField.getText() + "/" + leftPC.getSelectedFilename());
            file.delete();
            leftPC.updateLeftList(Paths.get(leftPC.pathField.getText()));
        }
        //удаление на сервере
        if (rightPC.getSelectedFilename() != null) {
            try {
                out.writeUTF("delete");
                out.flush();
                out.writeUTF(rightPC.pathField.getText() + "/" + rightPC.getSelectedFilename());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            rightPC.updateTable(rightPC.pathField.getText());
        }
    }

}