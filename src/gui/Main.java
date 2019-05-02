package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class Main extends Application {
    private static Stage primaryStage;

//    @Override
//    public void start(Stage stage) throws Exception {
//        primaryStage = stage;
//        Parent root = FXMLLoader.load(getClass().getResource("dbconfig.fxml"));
//        stage.setTitle("Plug and Play Web Services");
//        Scene primaryScene = new Scene(root);
//        stage.setScene(primaryScene);
//        Parents.getRootStack().push(root);
//        stage.show();
//    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hello World");
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 300, 250);

        File file = new File("C:\\Users\\Arihant Jain\\IdeaProjects\\JavaFX-WS\\images\\0.png");
        Image image = new Image(file.toURI().toString());
        ImageView iv = new ImageView(image);

        root.getChildren().add(iv);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) throws SQLException, IOException {
        launch(args);
    }
}
