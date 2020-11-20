import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ExternalPanelController extends PanelController {

    Controller controller = new Controller();
    String rootFolder;

    @FXML
    Label label;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);

        TableColumn<FileInfo, String> filenameColumn = new TableColumn<>("Name");
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumn.setPrefWidth(240);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Size");
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
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Date of change");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(120);

        filesTable.getColumns().addAll(fileTypeColumn, filenameColumn, fileSizeColumn, fileDateColumn);
        filesTable.getSortOrder().add(fileTypeColumn);


        filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                if (event.getClickCount() == 2) {
                    try {
                        Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
                        if (filesTable.getSelectionModel().getSelectedItem().getType().getName().equals("D")) {
                            updateTable(path.toString());
                        } else {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION, "It is not folder. The program does not have a file viewer.", ButtonType.OK);
                            alert.showAndWait();
                        }
                    } catch (NullPointerException e) {
                        return;
                    }
                }
            }
        });

    }

    public void updateTable(String path) {
        if (label.getText().equals("NA")) {
            setLabel(path.replace("server\\", "USER: ") + " ");
            rootFolder = path;
        }
        pathField.setText(path);
        filesTable.getItems().clear();
        filesTable.getItems().addAll(controller.getList(path));
        filesTable.sort();
    }

    public void setLabel(String path) {
        label.setText(path);
        label.setFont(new Font("Arial", 16));

    }

    @Override
    public void btnPathUpAction(ActionEvent actionEvent) {
        if (!rootFolder.equals(pathField.getText())) {
            Path path = Paths.get(pathField.getText()).getParent();
            updateTable(path.toString());
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "You are in the root folder of your account.", ButtonType.OK);
            alert.showAndWait();
        }
    }
}
