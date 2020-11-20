import javafx.fxml.FXML;
import javafx.scene.control.TextField;

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
