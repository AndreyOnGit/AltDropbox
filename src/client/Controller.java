package client;

import com.sun.org.apache.xpath.internal.operations.Mod;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
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
    TextField logField, passField, fieldName;

    @FXML
    Button btmAuth;

    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    private static InnerPanelController leftPC;
    private static ExternalPanelController rightPC;
    private static Socket socket;
    private static Stage logIn, newName, newFolder;
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

    public void btnConnect(ActionEvent actionEvent) {
        rightPC = (ExternalPanelController) rightPanel.getProperties().get("ctrl");
        try {
            throwFormAuth();
            socket = new Socket("localhost", 8189);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to connect to server", ButtonType.OK);
            alert.showAndWait();
        }
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
    }

    public void throwFormAuth() {
        logIn = new Stage();
        logIn.setTitle("Log in");
        try {
            logIn.setScene(new Scene(FXMLLoader.load(getClass().getResource("form.fxml")), 200, 100));
        } catch (IOException e) {
            System.out.println("Something is wrong");
        }
        logIn.show();
    }

    public void authBtnAction() {
        String log = logField.getText();
        String pass = passField.getText();
        String rootFolder = null;
        try {
            out.writeUTF("auth " + log + " " + pass);
            out.flush();
            String answer = in.readUTF();
            if (answer.equals("No log")) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Your login has not been found.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            if (answer.equals("No pass")) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Your password is not correct.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            rootFolder = in.readUTF();

        } catch (IOException e) {
            System.out.println("Something happend in method authBtnAction.");
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

    public void btnRenameAction(ActionEvent actionEvent) {
        //todo сделать выброс сообщения о наличие файла с тем же именем в папке
        //todo сделать проверку на расширение, переименование без изменения расширения
        leftPC = (InnerPanelController) leftPanel.getProperties().get("ctrl");
        if (leftPC.getSelectedFilename() == null && rightPC.getSelectedFilename() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "No file selected.", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        //файл у клиента
        if (leftPC.getSelectedFilename() != null) {
            File file = new File(leftPC.getCurrentPath(), leftPC.getSelectedFilename());
            panel = "left";
            try {
                throwFormRename(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //файл на сервере
        if (rightPC.getSelectedFilename() != null) {
            try {
                out.writeUTF("rename");
                out.flush();
                out.writeUTF(rightPC.pathField.getText() + "/" + rightPC.getSelectedFilename());
                out.flush();
                File file = new File(rightPC.getSelectedFilename());
                panel = "right";
                throwFormRename(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void throwFormRename(File file) throws IOException {
        newName = new Stage();
        newName.setTitle("Rename file: " + file.getName());
        newName.setScene(new Scene(FXMLLoader.load(getClass().getResource("newName.fxml")), 300, 50));
        newName.initModality(Modality.WINDOW_MODAL);
        newName.show();
        //todo сделать ввод исходного имени файла в TextField
    }

    public void btnChangeNameAction(ActionEvent actionEvent) {
        if (panel.equals("left")) {
            File file = new File(leftPC.getCurrentPath(), newName.getTitle().replace("Rename file: ", ""));
            File newFile = new File(leftPC.getCurrentPath(), fieldName.getText());
            //todo сделать проверку на некорректный ввод
            file.renameTo(newFile);
            newName.close();
            leftPC.updateLeftList(Paths.get(leftPC.getCurrentPath()));
        }
        if (panel.equals("right")) {
            //todo сделать проверку на некорректный ввод
            try {
                out.writeUTF(fieldName.getText());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            newName.close();
            rightPC.updateTable(rightPC.pathField.getText());
        }
        panel = "";
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
            try {
                out.writeUTF("make");
                out.flush();
                out.writeUTF(path);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            rightPC.updateTable(rightPC.pathField.getText());
        }
        panel = "";
    }

}