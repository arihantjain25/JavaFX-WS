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
            return new DB("jdbc:postgresql://" + dburi.getText() + ":" + dbport.getText() + "/", dbname.getText(),
                    dbuser.getText(), dbpass.getText());
    }
}
