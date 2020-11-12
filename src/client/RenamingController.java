package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.io.File;
import java.nio.file.Paths;

public class RenamingController {
    private Controller controller;
    private String panel;
    private String oldName;

    @FXML
    TextField fieldName;

    public void setInfo(Controller controller, String panel, String oldName) {
        this.controller = controller;
        this.panel = panel;
        this.oldName = oldName;
    }

    public void btnChangeName(ActionEvent event) {
        if (panel.equals("left")) {
            File file = new File(controller.leftPC.getCurrentPath(), oldName);
            File newFile = new File(controller.leftPC.getCurrentPath(), fieldName.getText());
            //todo сделать проверку на некорректный ввод
            file.renameTo(newFile);
            controller.leftPC.updateLeftList(Paths.get(controller.leftPC.getCurrentPath()));
        }
        if (panel.equals("right")) {
            //todo сделать проверку на некорректный ввод
            controller.send(fieldName.getText());
            controller.rightPC.updateTable(controller.rightPC.pathField.getText());
        }
        controller.closeRenameForm();


    }
}
