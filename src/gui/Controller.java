package gui;

import autowebservices.database.DB;
import autowebservices.datapull.SQLPull;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import autowebservices.grammar.JSONLexer;
import autowebservices.grammar.JSONParser;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.json.JSONArray;

import java.io.*;
import java.sql.SQLException;
import java.util.Scanner;

public class Controller {
    public TextField dburi;
    public TextField dbname;
    public PasswordField dbpass;
    public TextField dbuser;
    public TextField dbport;
    public AnchorPane anchorPane;
    public Button connectdb;
    public TextArea jsonschema;
    public DB db;

    public void connectDatabase() throws IOException, SQLException {
        FileWriter fileWriter = new FileWriter("dbinfo.txt");
        String url = "jdbc:postgresql://" + dburi.getText() + ":" + dbport.getText() + "/" + "!" + dbname.getText() + "!" + dbuser.getText() + "!" + dbpass.getText();
        fileWriter.write(url);
        fileWriter.close();
        db = establishConnection();
        loadApplication();
    }

    public DB establishConnection() throws FileNotFoundException, SQLException {
        File file = new File("dbinfo.txt");
        Scanner sc = new Scanner(file);
        String[] dbinfo = sc.nextLine().split("!");
        return new DB(dbinfo[0], dbinfo[1], dbinfo[2], dbinfo[3]);
    }

    public void loadApplication() throws IOException, SQLException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = fxmlLoader.load();
        Stage stage = Main.getPrimaryStage();
        Scene scene = stage.getScene();
        scene.setRoot(root);
        Parents.getRootStack().push(root);
        stage.show();
    }

    public void generatePaths() throws SQLException, FileNotFoundException {
        db = establishConnection();
        String jschema = jsonschema.getText();
        CharStream charStream = CharStreams.fromString(jschema);
        JSONLexer lexer = new JSONLexer(charStream);
        CommonTokenStream ts = new CommonTokenStream(lexer);
        JSONParser parser = new JSONParser(ts);
        parser.json(db);
        selectInput();
    }

    public void generateImages() {

    }

    public void selectInput() throws FileNotFoundException, SQLException {
        op("SELECT DISTINCT taxavernaculars.\"VernacularName\", users.\"uid\", taxa.\"RankId\", tmtraits.\"traitid\"\n" +
                "FROM taxa \n" +
                "LEFT JOIN taxavernaculars ON taxavernaculars.\"TID\" = taxa.\"TID\"\n" +
                "LEFT JOIN tmtraittaxalink ON taxa.\"TID\" = tmtraittaxalink.\"tid\"\n" +
                "LEFT JOIN users ON taxa.\"modifiedUid\" = users.\"uid\"\n" +
                "LEFT JOIN tmtraits ON tmtraittaxalink.\"traitid\" = tmtraits.\"traitid\"\n" +
                "ORDER BY taxavernaculars.\"VernacularName\", users.\"uid\", taxa.\"RankId\", tmtraits.\"traitid\"");
    }

    public void op(String query) throws SQLException, FileNotFoundException {
        String filePath = "test/Test3.json";
        db = establishConnection();
        SQLPull sqlPull = new SQLPull();
        JSONArray jsonArray = sqlPull.convertToJSON(db.executeQuery(query));
        String[] fillArray = new String[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            String str = jsonArray.get(i).toString();
            if (jsonArray.get(i).toString().equals("{}")) {
                fillArray[i] = "null";
            } else {
                fillArray[i] = str.split("\":")[1].replace("}", "");
            }
        }
        sqlPull.fillNested(filePath, fillArray, sqlPull.getCountForValues(filePath));
    }
}
