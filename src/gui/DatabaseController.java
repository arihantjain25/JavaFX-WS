package gui;

import autowebservices.database.DB;
import javafx.scene.control.TextArea;

import java.sql.SQLException;

public class DatabaseController {
    public DB db;
    public TextArea jsonschema;


    public void setDatabase(DB db) throws SQLException {
        this.db = db;
        System.out.println(db.getTableNames());
    }

    public void generatePaths() throws SQLException {
        String jschema = jsonschema.getText();
        System.out.println(jschema);
        DatabaseController databaseController = new DatabaseController();
        if (db == null)
            System.out.println("whyyy");
        else
            System.out.println(db.getTableNames());
//        CharStream charStream = CharStreams.fromString(jschema);
//        JSONLexer lexer = new JSONLexer(charStream);
//        CommonTokenStream ts = new CommonTokenStream(lexer);
//        JSONParser parser = new JSONParser(ts);
//        parser.json(db);

    }
}
