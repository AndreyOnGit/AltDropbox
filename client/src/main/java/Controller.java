import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
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
import java.util.stream.Stream;

public class Controller implements Initializable {

    public Controller() {
    }

    public Controller(Stage stage) {
        this.stage = stage;
    }

    @FXML
    VBox leftPanel, rightPanel;

    private Stage stage;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    protected static PanelController leftPC;
    protected static ExternalPanelController rightPC;
    private static Socket socket;
    private static Stage logIn, newName, newFolder, newUser;
    private static String panel = "none";
    private boolean isDark = false;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        rightPC = (ExternalPanelController) rightPanel.getProperties().get("ctrl");
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
                throwAlert(Alert.AlertType.ERROR, "Authorization failed. The data file may have been damaged.");
                return;
            }
            completeAuth();
        }
    }

    public void btnConnect(ActionEvent actionEvent) {
        connect();
        throwAuthForm();
    }

    public void btnDisconnect(ActionEvent actionEvent) {
        disconnect();
        File file = new File("./authFile.txt");
        file.delete();
    }

    public void btnExit(ActionEvent actionEvent) {
        try {
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("Something happened in method btmExitAction.");
        }
        Platform.exit();
    }

    public void btnCopy(ActionEvent actionEvent) {
        try {
            copy();
        } catch (NullPointerException e) {
            throwAlert(Alert.AlertType.WARNING, "You are not authorized. To work with a " +
                    "file (folder) on the server you need to be authorized.");
            e.printStackTrace();
            return;
        }
    }

    public void btnMove(ActionEvent actionEvent) {
        try {
            copy();
        } catch (NullPointerException e) {
            throwAlert(Alert.AlertType.WARNING, "You are not authorized. To work with a " +
                    "file (folder) on the server you need to be authorized.");
            return;
        }
        btnDelete(actionEvent);
    }

    public void btnDelete(ActionEvent actionEvent) {
        leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
        //удаление у клиента
        if (leftPC.getSelectedFilename() != null) {
            File file = new File(leftPC.pathField.getText() + "/" + leftPC.getSelectedFilename());
            file.delete();
            leftPC.updateLeftList(Paths.get(leftPC.pathField.getText()));
        }
        //удаление на сервере
        try {
            if (rightPC.getSelectedFilename() != null) {
                send("delete");
                send(rightPC.pathField.getText() + "/" + rightPC.getSelectedFilename());
                rightPC.updateTable(rightPC.pathField.getText());
            }
        } catch (NullPointerException e) {
            throwAlert(Alert.AlertType.WARNING, "You are not authorized. To work with a " +
                    "file (folder) on the server you need to be authorized.");
            return;
        }
    }

    public void btnRename(ActionEvent actionEvent) {
        try {
            if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
                throwAlert(Alert.AlertType.ERROR, "No file selected.");
                return;
            }
        } catch (NullPointerException e) {
            throwAlert(Alert.AlertType.WARNING, "No file selected. You are not authorized. " +
                    "To work with a file (folder) on the server you need to be authorized.");
            return;
        }
        if (leftPC.getSelectedFilename() != null) {
            throwRenameForm("left", leftPC.getSelectedFilename());
        }
        if (rightPC.getSelectedFilename() != null) {
            throwRenameForm("right", rightPC.getSelectedFilename());
        }
    }

    public void btnCreate(ActionEvent actionEvent) {
        if (!leftPC.filesTable.isFocused() && !rightPC.filesTable.isFocused()) {
            throwAlert(Alert.AlertType.ERROR, "No file table (right or left) selected.");
            return;
        }
        String path = "";
        if (rightPC.filesTable.isFocused()) {
            if (rightPC.label.getText().equals("NA")) {
                throwAlert(Alert.AlertType.WARNING, "You are not authorized. To create a folder on the server "
                        + "you need to be authorized.");
                return;
            }
            path = rightPC.pathField.getText();
            panel = "right";
        }
        if (leftPC.filesTable.isFocused()) {
            path = leftPC.pathField.getText();
            panel = "left";
        }
        throwCreateForm(path);
    }

    public void btnSignIn(ActionEvent actionEvent) {
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

    public void btnLight(ActionEvent e) {
        isDark = false;
        stage.getScene().getStylesheets().remove("dark-theme.css");
    }

    public void btnDark(ActionEvent e) {
        isDark = true;
        stage.getScene().getStylesheets().add("dark-theme.css");
    }

    public void btnAbout(ActionEvent e) {
        throwAlert(Alert.AlertType.NONE, "This application was made as part of the Java Network Storage "
                + "Development course.\n" +
                "This program is presented by geekbrains.ru.\n" +
                "Course instructor: Mikhail Levin.\n" +
                "Student and developer: Andrey Enyutin.");
    }

    public void connect() {
        try {
            socket = new Socket("localhost", 8189);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            throwAlert(Alert.AlertType.ERROR, "Failed to connect to server");
        }
    }

    public void disconnect() {
        try {
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            System.out.println("Something happened in method btnDisconnect.");
        }
        rightPC.setLabel("NA");
        rightPC.pathField.clear();
        rightPC.filesTable.getItems().clear();
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

    public void copy() throws NullPointerException {
        if (rightPC.getSelectedFilename() == null && leftPC.getSelectedFilename() == null) {
            throwAlert(Alert.AlertType.ERROR, "No file selected.");
            return;
        }
        //копирования с клиента на сервер
        if (leftPC.getSelectedFilename() != null) {
            try {
                PathArray.arrayList.clear();
                Path path = Paths.get(leftPC.getCurrentPath() + "/" + leftPC.getSelectedFilename());
                Files.walk(path)
                        .filter(Files::isRegularFile)
                        .forEach(PathArray::makePathArray);
                for (int i = 0; i < PathArray.arrayList.size(); i++) {

                    String target = PathArray.arrayList.get(i).toString().replace(leftPC.getCurrentPath(), rightPC.pathField.getText());
                    upload(PathArray.arrayList.get(i).toString(), target);
                    in.readUTF();
                }
                rightPC.updateTable(rightPC.pathField.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //копирования с сервера на клиента
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
                byte[] buffer = new byte[1024];
                for (int i = 0; i < (size + 1023) / 1024; i++) {
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

    public void upload(String source, String target) throws IOException {
        send("upload");
        send(target);
        File file = new File(source);
        long length = file.length();
        out.writeLong(length);
        FileInputStream fileBytes = new FileInputStream(file);
        int read = 0;
        byte[] buffer = new byte[1024];
        while ((read = fileBytes.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        fileBytes.close();
        out.flush();
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

    public void send(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException | NullPointerException e) {
            throwAlert(Alert.AlertType.ERROR, "Communication with the server has failed. " +
                    "You may not be logged in.");
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

    public void completeAuth() {
        rightPC = (ExternalPanelController) rightPanel.getProperties().get("ctrl");
        leftPC = (PanelController) leftPanel.getProperties().get("ctrl");
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
        Scene sc = new Scene(root);
        if (isDark) sc.getStylesheets().add("dark-theme.css");
        logIn.setScene(sc);
        logIn.setTitle("Log in");
        logIn.show();
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
        Scene sc = new Scene(root, 300, 50);
        if (isDark) sc.getStylesheets().add("dark-theme.css");
        newName.setScene(sc);
        newName.setTitle("Rename file: " + oldName);
        newName.show();
    }

    public void closeRenameForm() {
        newName.close();
    }

    public void throwCreateForm(String path) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("newFolder.fxml"));
        try {
            Parent root = loader.load();
            CreatorFolderController creatorFolderController = loader.getController();
            creatorFolderController.setInfo(path, this);
            newFolder = new Stage();
            Scene sc = new Scene(root, 300, 50);
            if (isDark) sc.getStylesheets().add("dark-theme.css");
            newFolder.setScene(sc);
            newFolder.setTitle("Creating new folder");
            newFolder.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void throwAlert(Alert.AlertType alertType, String content) {
        Alert alert = new Alert(alertType, content, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

}