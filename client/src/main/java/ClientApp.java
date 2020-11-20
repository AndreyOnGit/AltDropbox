import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {


    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        Controller controller = new Controller( primaryStage );
        fxmlLoader.setController( controller );
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("File Net Manager");
        primaryStage.setScene(new Scene(root, 1000, 500));
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
