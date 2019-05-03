package gui;

import autowebservices.database.DB;
import autowebservices.datapull.SQLPull;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

import javafx.stage.Stage;
import autowebservices.grammar.JSONLexer;
import autowebservices.grammar.JSONParser;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.json.JSONArray;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
    File filesJpg[];
    Image images[];
    ImageView imageViews[];
    BufferedImage bufferedImage[];
    TitledPane titledPanes[];

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

    public void loadApplication() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = fxmlLoader.load();
        Stage stage = Main.getPrimaryStage();
        Scene scene = stage.getScene();
        scene.setRoot(root);
        Parents.getRootStack().push(root);
        stage.show();
    }

    public void generatePaths() throws SQLException, IOException {
        db = establishConnection();
        String jschema = jsonschema.getText();
        FileWriter fileWriter = new FileWriter("schema.json");
        fileWriter.write(jschema);
        fileWriter.close();
        CharStream charStream = CharStreams.fromString(jschema);
        JSONLexer lexer = new JSONLexer(charStream);
        CommonTokenStream ts = new CommonTokenStream(lexer);
        JSONParser parser = new JSONParser(ts);
        parser.json(db);
        generateImages();
    }

    public void generateImages() throws IOException {
        ProcessBuilder builderlinux = new ProcessBuilder("python3", "/home/arihant/IdeaProjects/JavaFX-WS/creategraphimages.py");
//        ProcessBuilder builderwin = new ProcessBuilder("python", "creategraphimages.py");
        Process p = builderlinux.start();
        try {
            p.waitFor();
        } catch (InterruptedException ignored) { }
        openDirectoryChooser();
    }

    private void openDirectoryChooser() {
        File selectedDirectory = new File("/home/arihant/IdeaProjects/JavaFX-WS/images");
        FilenameFilter filterJpg = (dir, name) -> name.toLowerCase().endsWith(".png");
        filesJpg = selectedDirectory.listFiles(filterJpg);
        openTitledPane();
    }

    private void openTitledPane() {
        int numOfJpg = filesJpg.length;
        images = new Image[numOfJpg];
        bufferedImage = new BufferedImage[numOfJpg];
        imageViews = new ImageView[numOfJpg];
        titledPanes = new TitledPane[numOfJpg];
        for (int i = 0; i < numOfJpg; i++) {
            try {
                File file = filesJpg[i];
                bufferedImage[i] = ImageIO.read(file);
                images[i] = SwingFXUtils.toFXImage(bufferedImage[i], null);
                imageViews[i] = new ImageView();
                imageViews[i].setImage(images[i]);
                imageViews[i].setFitWidth(500);
                imageViews[i].setPreserveRatio(true);
                imageViews[i].setSmooth(true);
                imageViews[i].setCache(true);
                titledPanes[i] = new TitledPane(String.valueOf(i), imageViews[i]);
            } catch (IOException ignored) {
            }
        }
        Accordion accordion = new Accordion();
        accordion.getPanes().addAll(titledPanes);
        Stage titledPaneStage = new Stage();
        titledPaneStage.setTitle("TitledPane");
        Scene scene = new Scene(new Group(), 500, 500);
        Group root = (Group) scene.getRoot();
        root.getChildren().add(accordion);
        titledPaneStage.setScene(scene);
        titledPaneStage.show();
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
        String filePath = "schema.json";
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
