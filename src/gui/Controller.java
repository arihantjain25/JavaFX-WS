package gui;

import autowebservices.database.DB;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class Controller {
    public TextField dburi;
    public TextField dbname;
    public TextField dbpass;
    public TextField dbuser;
    public TextField dbport;
    public AnchorPane anchorPane;
    public Button connectdb;
    public DB db;

    public void connectDatabase() throws IOException {
            this.db = new DB("jdbc:postgresql://" + dburi.getText() + ":" + dbport.getText() + "/", dbname.getText(),
                    dbuser.getText(), dbpass.getText());
            loadApplication();
    }

    public void loadApplication() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = fxmlLoader.load();
        Stage stage = Main.getPrimaryStage();
        Scene scene = stage.getScene();
        scene.setRoot(root);
        Parents.getRootStack().push(root);
        stage.show();
    }
}
