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



//        CharStream charStream = CharStreams.fromString("{\n" +
//                "  \"vernacular_name\": \"VernacularName\",\n" +
//                "  \"source\": \"uid\",\n" +
//                "  \"users\": {\n" +
//                "    \"rank_id\": \"RankId\"\n" +
//                "  },\n" +
//                "  \"count\": {\n" +
//                "    \"url\": \"traitid\"\n" +
//                "  }\n" +
//                "}");
//        String jdbcUrl = "jdbc:postgresql://localhost:5432/";
//        String dbName = "symbiota2";
//        String username = "postgres";
//        String password = "password";
////        String dbName = "imdb";
////        String password = ".namjklsd.";
//        DB db = new DB(jdbcUrl, dbName, username, password);
//        JSONLexer lexer = new JSONLexer(charStream);
//        CommonTokenStream ts = new CommonTokenStream(lexer);
//        JSONParser parser = new JSONParser(ts);
//        parser.json(db);
    }
}
