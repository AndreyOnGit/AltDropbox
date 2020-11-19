import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;


public class SingingInController {
    Controller controller;

    @FXML
    TextField logField, passField;


    public void setInfo (Controller controller){
        this.controller = controller;
    }

    public void btnSingIn(ActionEvent event){
        controller.disconnect();
        controller.connect();
        controller.send("singIn " + logField.getText() + " " + passField.getText());
        String answer = controller.get();
        if (answer.equals("occupied")) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "An account with this login already exists", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        if (answer.equals("OK")){
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Registration completed successfully", ButtonType.OK);
            alert.showAndWait();
            controller.completeAuth();
        }
        if (answer.equals("repeat")) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Something went wrong. Repeat", ButtonType.OK);
            alert.showAndWait();
            return;
        }
    }
}
