package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class CreatorFolderController {
    private Controller controller;

    private String path;

    @FXML
    TextField fieldFolderName;

    public void btnCreateFolder() {
        controller.createFolder(path + "/" + fieldFolderName.getText());
    }

    public void setInfo(String path, Controller controller) {
        this.path = path;
        this.controller = controller;
    }


}
