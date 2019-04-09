package sample;

import autowebservices.database.DB;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class Controller {

    @FXML
    private TextField dburi;

    @FXML
    private TextField dbname;

    @FXML
    private TextField dbpass;

    @FXML
    private TextField dbuser;

    @FXML
    private TextField dbport;

    @FXML
    public DB onButtonClicked() {
        try {
            DB db = new DB("jdbc:postgresql://" + dburi.getText() + ":" + dbport.getText() + "/", dbname.getText(),
                    dbuser.getText(), dbpass.getText());
            System.out.println("Database Connected.");
            return db;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
