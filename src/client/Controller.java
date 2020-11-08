package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

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

    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    private InnerPanelController leftPC;
    private ExternalPanelController rightPC;
    private static Socket socket;


    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void btnConnect(ActionEvent actionEvent) {
        rightPC = (ExternalPanelController) rightPanel.getProperties().get("ctrl");
        try {
            socket = new Socket("localhost", 8189);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            String rootFolder = in.readUTF();
            System.out.println("path after connect: " + rootFolder + ".");
            rightPC.updateTable(rootFolder);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось подключиться к серверу", ButtonType.OK);
            alert.showAndWait();
        }
    }

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

        PanelController srcPC = null, dstPC = null; // можно потом закомментить
        if (leftPC.getSelectedFilename() != null) {
            srcPC = leftPC; // можно потом закомментить
            dstPC = rightPC; // можно потом закомментить
            try {
                out.writeUTF("upload");
                out.flush();
                Path path = Paths.get(rightPC.pathField.getText() + "/" + leftPC.getSelectedFilename());
                System.out.println(path.toString());
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
                System.out.println("It is flush");


                Alert alert = new Alert(Alert.AlertType.INFORMATION, "file is sent", ButtonType.OK);
                alert.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
            }

//            rightPC.updateList();

        }
        if (rightPC.getSelectedFilename() != null) {
            srcPC = rightPC;
            dstPC = leftPC;
        }

//        Path srcPath = Paths.get(srcPC.getCurrentPath(), srcPC.getSelectedFilename());
//        Path dstPath = Paths.get(dstPC.getCurrentPath()).resolve(srcPath.getFileName().toString());

//        try {
//            Files.copy(srcPath, dstPath);
//            dstPC.updateLeftList(Paths.get(dstPC.getCurrentPath()));
//        } catch (IOException e) {
//            Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось скопировать указанный файл", ButtonType.OK);
//            alert.showAndWait();
//        }
    }

    public List<FileInfo> getList(String path) {
        String command = "info" + path;
        System.out.println("command out: "+ command + ".");
        try {
            System.out.println("in block TRY");
            out.writeUTF(command);
            System.out.println("after out.write");
            out.flush();
            System.out.println("out.writeUTF(command) OK");
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


}