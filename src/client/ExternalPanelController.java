package client;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ExternalPanelController extends PanelController {

    Controller controller = new Controller();

    @FXML
    Label label;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);

        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Имя");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumn.setPrefWidth(240);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });
        fileSizeColumn.setPrefWidth(120);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменения");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(120);

        filesTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn, fileDateColumn);
        filesTable.getSortOrder().add(fileTypeColumn);


        filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                if (event.getClickCount() == 2) {
                    Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
//                    System.out.println(filesTable.getSelectionModel().getSelectedItem().getType().getName());
                    if (filesTable.getSelectionModel().getSelectedItem().getType().getName().equals("D")) {
                        updateTable(path.toString());
                    } else {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "It is not folder. The program does not have a file viewer.", ButtonType.OK);
                        alert.showAndWait();
                    }
                }
            }
        });

    }

//    public void updateTable() {
//        Path path = Paths.get(pathField.getText());
//        updateList(path, controller.getList("info" + path.toString()) );
//    }


    public void updateTable(String path) {
        if (label.getText().equals("NA")) {
            setLabel(path);
        }
        pathField.setText(path);
        filesTable.getItems().clear();
//        System.out.println("path in update(): " + path + ".");
        filesTable.getItems().addAll(controller.getList(path));
        filesTable.sort();
    }

    public void setLabel(String path) {
        label.setText(path);
        label.setFont(new Font("Arial", 16));

    }

    @Override
    public void btnPathUpAction(ActionEvent actionEvent) {
        if (!label.getText().equals(pathField.getText())) {
            Path path = Paths.get(pathField.getText()).getParent();
            updateTable(path.toString());
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "You are in the root folder of your account.", ButtonType.OK);
            alert.showAndWait();
        }
    }
}
