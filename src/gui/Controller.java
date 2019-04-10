package gui;

import autowebservices.database.DB;
import autowebservices.grammar.JSONLexer;
import autowebservices.grammar.JSONParser;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import java.io.IOException;
import java.sql.SQLException;

public class Controller {
    public TextField dburi;
    public TextField dbname;
    public PasswordField dbpass;
    public TextField dbuser;
    public TextField dbport;
    public AnchorPane anchorPane;
    public Button connectdb;
    public DB db;

    public void connectDatabase() throws IOException, SQLException {
            db = new DB("jdbc:postgresql://" + dburi.getText() + ":" + dbport.getText() + "/", dbname.getText(),
                    dbuser.getText(), dbpass.getText());
            loadApplication();
    }

    public void loadApplication() throws IOException, SQLException {
        DatabaseController databaseController = new DatabaseController();
        databaseController.setDatabase(db);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = fxmlLoader.load();
        Stage stage = Main.getPrimaryStage();
        Scene scene = stage.getScene();
        scene.setRoot(root);
        Parents.getRootStack().push(root);
        stage.show();
    }

    public void reconfigDatabase() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("dbconfig.fxml"));
        Parent root = fxmlLoader.load();
        Stage stage = Main.getPrimaryStage();
        Scene scene = stage.getScene();
        scene.setRoot(root);
        Parents.getRootStack().push(root);
        stage.show();
    }




}
