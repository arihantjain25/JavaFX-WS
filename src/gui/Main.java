package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.sql.SQLException;

public class Main extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        Parent root = FXMLLoader.load(getClass().getResource("dbconfig.fxml"));
        stage.setTitle("Plug and Play Web Services");
        Scene primaryScene = new Scene(root);
        stage.setScene(primaryScene);
        Parents.getRootStack().push(root);
        stage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) throws SQLException {
        launch(args);
    }
}
