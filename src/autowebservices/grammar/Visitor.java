/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autowebservices.grammar;

import autowebservices.database.DB;
import autowebservices.database.ForeignKey;
import autowebservices.datapull.SQLPull;
import autowebservices.joingraph.Graph;
import autowebservices.joingraph.Path;
import autowebservices.tree.PatternTree;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.BorderPane;
import org.json.JSONArray;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
// import autowebservices.database.ForeignKey;
// import java.util.Set;

/**
 * @author Curtis Dyreson
 */
public class Visitor {

    private static PatternTree tree;
    private static DB db;
    private static Stack<PatternTree> treeStack;
    private static Graph joinGraph;

    public static void enterJson(DB db) throws IOException {

        // Initialize the database and tree
        treeStack = new Stack();
//        String jdbcUrl = "jdbc:postgresql://localhost:5432/";
//        String dbName = "symbiota2";
//        String username = "postgres";
//        String password = "password";
////        String dbName = "imdb";
////        String password = ".namjklsd.";
//        db = new DB(jdbcUrl, dbName, username, password);
        Visitor.db = db;
        tree = new PatternTree(db);
        joinGraph = new Graph(db);
    }


    public static void exitJson() throws IllegalAccessException, IOException {
//        Map<String, Map<String, List<Path>>> mapMap = new HashMap<>();
        Set<ForeignKey> treePaths = tree.computeTreePaths(joinGraph, null);
//        List<String> listTables = tree.listTables();
//        List<String> listColumns = tree.listColumns();
//        String query = sqlPull.generateQuery(joinGraph, treePaths,
//                listColumns.toString(), listTables);
//        System.out.println(query);
    }

    public void op(String query) throws SQLException {
        String filePath = "test/Test3.json";
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


    public static void enterArray() {
//            System.out.println("entering array");
        PatternTree child = new PatternTree(db, tree, PatternTree.arrayType);
        treeStack.push(tree);
        tree = child;
    }

    public static void exitArray() {
        tree = treeStack.pop();
    }

    public static void enterObj() {
//            System.out.println("descending a level");
        treeStack.push(tree);
        PatternTree child = new PatternTree(db, tree, PatternTree.objType);
        treeStack.push(tree);
        tree = child;
    }

    public static void exitObj() {
//            System.out.println("going up a level");
        tree = treeStack.pop();
    }

    public static void enterPair(String key) {
        // System.out.println($STRING.text + " " + $value.text);
//            System.out.println("comp: enterPair " + key);
        key = key.replace("\"", "");
        tree = treeStack.peek();
        PatternTree child = new PatternTree(db, tree, key, PatternTree.pairType);
        tree.addChild(child);
        tree = child;
        treeStack.push(child);
    }

    public static void parsedString(String value) {
        // System.out.println($STRING.text + " " + $value.text);
//            System.out.println("comp: parsedString " + value);
        value = value.replace("\"", "");
        tree = treeStack.pop();
        tree.buildPotentialLabels(value);
    }

    public static void exitPair() {
//            System.out.println("comp: exitPair ");
        tree = treeStack.peek();
    }
}
