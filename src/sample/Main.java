package sample;

import autowebservices.database.DB;
import autowebservices.grammar.JSONLexer;
import autowebservices.grammar.JSONParser;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.util.Optional;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Plug and Play Web Services");
        primaryStage.setScene(new Scene(root, 690, 481));
        primaryStage.show();
//        try {
//            DB db;
//            String jdbcUrl = "jdbc:postgresql://localhost:5432/";
//            String dbName = "symbiota2";
//            String username = "postgres";
//            String password = "password";
////        String dbName = "imdb";
////        String password = ".namjklsd.";
//            db = new DB(jdbcUrl, dbName, username, password);
//            FXMLLoader fxmlLoader = new FXMLLoader();
//            Controller controller = fxmlLoader.getController();
//            DB db1 = controller.onButtonClicked();
//            CharStream charStream = CharStreams.fromFileName("test/Test3.json");
//            JSONLexer lexer = new JSONLexer(charStream);
//            CommonTokenStream ts = new CommonTokenStream(lexer);
//            JSONParser parser = new JSONParser(ts);
//            parser.json(db);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
