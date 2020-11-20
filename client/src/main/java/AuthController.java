import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class AuthController {
    private Controller controller;

    @FXML
    TextField logField, passField;

    @FXML
    CheckBox checkBox;

    public void btnAuth(ActionEvent event) {
        String log = logField.getText();
        String pass = passField.getText();
        String rootFolder = null;
        controller.send("auth " + log + " " + pass);
        String answer = controller.get();
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
        if (checkBox.isSelected()) {
            File file = new File("./authFile.txt");
            try (FileWriter writer = new FileWriter(file, false)) {
                writer.write(log + " " + pass);
                writer.flush();
            } catch (IOException ex) {
            }
        }
        controller.completeAuth();
    }

    public void setInfo(Controller controller) {
        this.controller = controller;
    }
}
