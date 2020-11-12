package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    VBox leftPanel, rightPanel;

    @FXML
    TextField logField, passField, fieldName;

    @FXML
    Button btmAuth;

    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    protected static InnerPanelController leftPC;
    protected static ExternalPanelController rightPC;
    private static Socket socket;
    private static Stage logIn, newName, newFolder, newUser;
    private static String panel = "none";


    public void btnExitAction(ActionEvent actionEvent) {
        try {
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("Something happend in method btmExitAction.");
        }
        Platform.exit();
    }

    public void connect() {
        try {
            socket = new Socket("localhost", 8189);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to connect to server", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnConnect(ActionEvent actionEvent) {
        connect();
        throwAuthForm();
    }

    public void btnDisconnect(ActionEvent actionEvent) {
        try {
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("Something happend in method btnDisconnect.");
        }
        rightPC.setLabel("NA");
        rightPC.pathField.clear();
        rightPC.filesTable.getItems().clear();
        File file = new File("./authFile.txt");
        file.delete();
    }

    public void throwAuthForm() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("authForm.fxml"));
        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        AuthController authController = loader.getController();
        authController.setInfo(this);
        logIn = new Stage();
        logIn.setScene(new Scene(root));
        logIn.setTitle("Log in");
        logIn.show();
    }

    public List<FileInfo> getList(String path) {
        String command = "info" + path;
        send(command);
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

    public void copyBtnAction(ActionEvent actionEvent) {
        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ни один файл не был выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        //копирования с клиента на сервер
        if (leftPC.getSelectedFilename() != null) {
            try {
                send("upload");
                Path path = Paths.get(rightPC.pathField.getText() + "/" + leftPC.getSelectedFilename());
                send(path.toString());
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
            try {
                send("download");
                send(rightPC.pathField.getText() + "/" + rightPC.getSelectedFilename());
                File file = new File(leftPC.pathField.getText() + "/" + rightPC.getSelectedFilename());
                if (!file.exists()) {
                    file.createNewFile();
                }
                long size = in.readLong();
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buffer = new byte[256];
                for (int i = 0; i < (size + 255) / 256; i++) {
                    int read = in.read(buffer);
                    fos.write(buffer, 0, read);
                }
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            leftPC.updateLeftList(Paths.get(leftPC.pathField.getText()));
        }
    }

    // todo надо учесть работу с папками
    public void moveBtnAction(ActionEvent actionEvent) {
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
            send("delete");
            send(rightPC.pathField.getText() + "/" + rightPC.getSelectedFilename());
            rightPC.updateTable(rightPC.pathField.getText());
        }
    }

    public void btnRenameAction(ActionEvent actionEvent) {
        //todo сделать выброс сообщения о наличие файла с тем же именем в папке
        //todo сделать проверку на расширение, переименование без изменения расширения
        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No file selected.", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        if (leftPC.getSelectedFilename() != null) {
            throwRenameForm("left", leftPC.getSelectedFilename());
        }
        if (rightPC.getSelectedFilename() != null) {
            send("rename");
            send(rightPC.pathField.getText() + "/" + rightPC.getSelectedFilename());
            throwRenameForm("right", rightPC.getSelectedFilename());
        }
    }

    public void closeRenameForm() {
        newName.close();
    }

    private void throwRenameForm(String panel, String oldName) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("newName.fxml"));
        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        RenamingController renamingController = loader.getController();
        renamingController.setInfo(this, panel, oldName);
        newName = new Stage();
        newName.setScene(new Scene(root));
        newName.setTitle("Rename file: " + oldName);
        newName.show();
    }


    public void btnCreateFolderAction(ActionEvent actionEvent) {
        leftPC = (InnerPanelController) leftPanel.getProperties().get("ctrl");
        rightPC = (ExternalPanelController) rightPanel.getProperties().get("ctrl");
        if (!leftPC.filesTable.isFocused() && !rightPC.filesTable.isFocused()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No file table (right or left) selected.", ButtonType.OK);
            alert.showAndWait();
            System.out.println("!leftPC.filesTable.isManaged() && !rightPC.filesTable.isManaged()");
            return;
        }
        String path = "";
        if (rightPC.filesTable.isFocused()) {
            path = rightPC.pathField.getText();
            panel = "right";
        }
        if (leftPC.filesTable.isFocused()) {
            path = leftPC.pathField.getText();
            panel = "left";
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("newFolder.fxml"));
        try {
            Parent root = loader.load();
            CreatorFolderController creatorFolderController = loader.getController();
            creatorFolderController.setInfo(path, this);
            newFolder = new Stage();
            newFolder.setScene(new Scene(root));
            newFolder.setTitle("Creat new folder");
            newFolder.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createFolder(String path) {
        newFolder.close();
        if (panel.equals("left")) {
            File file = new File(path);
            file.mkdir();
            leftPC.updateLeftList(Paths.get(leftPC.getCurrentPath()));
        }
        if (panel.equals("right")) {
            send("make");
            send(path);
            rightPC.updateTable(rightPC.pathField.getText());
        }
        panel = "";
    }

    public void btnSignIn() {
        connect();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("singignIn.fxml"));
        try {
            Parent root = loader.load();
            SingingInController singingInController = loader.getController();
            singingInController.setInfo(this);
            newUser = new Stage();
            newUser.setScene(new Scene(root));
            newUser.setTitle("Sing in");
            newUser.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String get() {
        try {
            return in.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void compliteAuth() {
        rightPC = (ExternalPanelController) rightPanel.getProperties().get("ctrl");
        leftPC = (InnerPanelController) leftPanel.getProperties().get("ctrl");
        String rootFolder = "";
        try {
            rootFolder = in.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }
        rightPC.updateTable(rootFolder);
        try {
            newUser.close();
        } catch (RuntimeException e) {
        }
        try {
            logIn.close();
        } catch (RuntimeException e) {
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        File file = new File("./authFile.txt");
        if (file.exists()) {
            connect();
            String logAndPass = "";
            try {
                logAndPass = new String(Files.readAllBytes(Paths.get(file.getPath())));
                System.out.println("logAndPass: " + logAndPass);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String logPass[] = logAndPass.split(" ");
            String log = logPass[0];
            String pass = logPass[1];
            send("auth " + log + " " + pass);
            String answer = get();
            if (!answer.equals("OK")) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Authorization failed. The data file may have been damaged.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            compliteAuth();
        }
    }
}