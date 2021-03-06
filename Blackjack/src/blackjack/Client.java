package blackjack;

import blackjack.Player.Move;
import blackjack.Player.State;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * @author Tanner Lisonbee
 */
public class Client extends Application implements Runnable, BlackjackConstants
{
    private ObjectInputStream fromServer;
    private ObjectOutputStream toServer;
    private TextField creditsField, betField;
    private Text mainCardArea, message, dealerHand;
    private TextField[] playerFields;
    private Text[] cardArea;
    private Polygon[] turnMarker;
    private String ip;
    private List<Player> players;
    private Player supportedPlayer;
    private int turnN;
    
    @Override
    public void start(Stage primaryStage)
    {
        StackPane root = new StackPane();
        GridPane grid = new GridPane();
        Pane pane = new Pane();
        pane.setMouseTransparent(true);

        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);

        //Label for title
        Label gameLabel = new Label("BLACKJACK");
        gameLabel.setFont(Font.font("Times New Roman", 36));
        gameLabel.setPadding(new Insets(10 ,0, 0, 50));

        grid.getRowConstraints().add(new RowConstraints(50));
        grid.getColumnConstraints().add(new ColumnConstraints(70));
        grid.getColumnConstraints().add(new ColumnConstraints(210));

        //IP Address
        Label ipLabel = new Label("IP Address: ");
        TextField ipInput = new TextField();
        ipInput.setText("localhost");
        GridPane.setConstraints(ipLabel, 0, 1);
        GridPane.setConstraints(ipInput, 1, 1);

        //Username
        Label usernameLabel = new Label("Username: ");
        TextField usernameInput = new TextField();
        GridPane.setConstraints(usernameLabel, 0, 2);
        GridPane.setConstraints(usernameInput, 1, 2);

        //Login button
        Button btn = new Button("Enter Game");
        GridPane.setConstraints(btn, 1, 6);
        
        //Error message label 
        Label errorMessage = new Label("Welcome.");
        GridPane.setConstraints(errorMessage, 1, 7);

        grid.getChildren().addAll(ipLabel, ipInput,
                                  usernameLabel, usernameInput,
                                  btn,
                                  errorMessage);
        pane.getChildren().add(gameLabel);
        root.getChildren().addAll(grid, pane);

        Scene scene = new Scene(root, 300, 200);
        primaryStage.setTitle("Blackjack");
        primaryStage.setScene(scene);
        primaryStage.setResizable(ALLOW_RESIZE);
        primaryStage.show();

        //ensure client terminates when the window is closed
        primaryStage.setOnCloseRequest((WindowEvent event) -> 
        {
            Platform.exit();
            System.exit(0);
        });
        
        btn.setOnAction((ActionEvent event) ->
        {
            //------------------------------------------------------------------
            //REMOVE FROM FINAL GAME
            if (ipInput.getText().equalsIgnoreCase("debug"))
            {
                buildGUI(primaryStage);
            }
            //------------------------------------------------------------------
            boolean isConnected = connectToServer(ipInput.getText());
            if (isConnected == false)
                errorMessage.setText("Failed to connect to host...");
            else if (usernameInput.getText().isEmpty())
                errorMessage.setText("You must enter a name...");
            else
            {
                ip = ipInput.getText();
                supportedPlayer = new Player(usernameInput.getText());
                buildGUI(primaryStage);
            }
        });
        
        usernameInput.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode().equals(KeyCode.ENTER))
                btn.fire();
        });
        
        ipInput.setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode().equals(KeyCode.ENTER))
                btn.fire();
        });
    }
    
    public void buildGUI(Stage primaryStage)
    {
        //StackPane root = new StackPane();
        //GridPane grid = new GridPane();
        Pane root = new Pane();
        
        //set and configure background
        root.setStyle("-fx-background-image: url(" + BACKGROUND + "); \n" +
                      "-fx-background-position: center center; \n" +
                      "-fx-background-repeat: stretch; \n" + 
                      "-fx-background-size: 1280 720;");
        
        //current bet label
        Label betLabel = new Label("CURRENT BET: ");
        betLabel.setFont(Font.font("Times New Roman", 32));
        betLabel.setTextFill(Color.web("#FFD000"));
        betLabel.setLayoutX(15);
        betLabel.setLayoutY(15);
        
        //current bet field
        betField = new TextField();
        betField.setFont(Font.font("Times New Roman", 24));
        betField.setPrefWidth(80);
        betField.setLayoutX(250);
        betField.setLayoutY(15);
        
        //credits available label
        Label creditsLabel = new Label("CREDITS AVAILABLE: ");
        creditsLabel.setFont(Font.font("Times New Roman", 32));
        creditsLabel.setTextFill(Color.web("#FFD000"));
        creditsLabel.setLayoutX(825);   
        creditsLabel.setLayoutY(15);
        
        //credits field
        creditsField = new TextField();
        creditsField.setEditable(false);
        creditsField.setFont(Font.font("Times New Roman", 24));
        creditsField.setPrefWidth(80);
        creditsField.setLayoutX(1170);
        creditsField.setLayoutY(15);
        
        //stay button
        Button btnStay = new Button("STAY");
        btnStay.setLayoutX(434);
        btnStay.setLayoutY(467);
        btnStay.setFont(Font.font("Times New Roman", 16));
        btnStay.setOnAction((ActionEvent event) -> 
        {
            System.out.println("STAY");
            try {  
                if(supportedPlayer.getState() == State.ON)
                    cStay();
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
            updateFields();
        });
        
        //hit button
        Button btnHit = new Button("HIT");
        btnHit.setLayoutX(789);
        btnHit.setLayoutY(467);
        btnHit.setFont(Font.font("Times New Roman", 16));
        btnHit.setOnAction((ActionEvent event) -> 
        {
           //if(supportedPlayer.getState() == State.)
            System.out.println("HIT");
            try {  
                System.out.println("testing");
                if(supportedPlayer.getState() == State.ON)
                    cHit();
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
            updateFields();
        });
        
        //player 1
        TextField player1Field = new TextField();
        player1Field.setLayoutX(157);
        player1Field.setLayoutY(403);
        player1Field.setEditable(false);
        player1Field.setPrefWidth(96);
        player1Field.setFont(Font.font("Times New Roman"));
        
        //player 2
        TextField player2Field = new TextField();
        player2Field.setLayoutX(276);
        player2Field.setLayoutY(403);
        player2Field.setEditable(false);
        player2Field.setPrefWidth(96);
        player2Field.setFont(Font.font("Times New Roman"));
        
        //player 3
        TextField player3Field = new TextField();
        player3Field.setLayoutX(908);
        player3Field.setLayoutY(403);
        player3Field.setEditable(false);
        player3Field.setPrefWidth(96);
        player3Field.setFont(Font.font("Times New Roman"));
        
        //player 4
        TextField player4Field = new TextField();
        player4Field.setLayoutX(1026);
        player4Field.setLayoutY(403);
        player4Field.setEditable(false);
        player4Field.setPrefWidth(96);
        player4Field.setFont(Font.font("Times New Roman"));
        
        //card area for player1
        Text cardText1 = new Text();
        cardText1.setLayoutX(170);
        cardText1.setLayoutY(480);
        cardText1.setFont(Font.font("Times New Roman", 51));
        cardText1.setFill(Color.web("#FFD000"));
        cardText1.setWrappingWidth(70);
        
        //card area for player2
        Text cardText2 = new Text();
        cardText2.setLayoutX(289);
        cardText2.setLayoutY(480);
        cardText2.setFont(Font.font("Times New Roman", 51));
        cardText2.setFill(Color.web("#FFD000"));
        cardText2.setWrappingWidth(70);
        
        //card area for player3
        Text cardText3 = new Text();
        cardText3.setLayoutX(920);
        cardText3.setLayoutY(480);
        cardText3.setFont(Font.font("Times New Roman", 51));
        cardText3.setFill(Color.web("#FFD000"));
        cardText3.setWrappingWidth(70);
        
        //card area for player4
        Text cardText4 = new Text();
        cardText4.setLayoutX(1039);
        cardText4.setLayoutY(480);
        cardText4.setFont(Font.font("Times New Roman", 51));
        cardText4.setFill(Color.web("#FFD000"));
        cardText4.setWrappingWidth(70);
        
        //main card area
        mainCardArea = new Text();
        mainCardArea.setLayoutX(582);
        mainCardArea.setLayoutY(478);
        mainCardArea.setFont(Font.font("Times New Roman", 88));
        mainCardArea.setText("\uD83C\uDCA0");
        mainCardArea.setFill(Color.web("#FFD000"));
        mainCardArea.setWrappingWidth(117);
        
        //message text
        message = new Text();
        message.setLayoutX(10);
        message.setLayoutY(700);
        message.setFont(Font.font("Times New Roman", 16));
        message.setFill(Color.WHITE);
        
        Text dealer = new Text("Dealer Hand: ");
        dealer.setLayoutX(560);
        dealer.setLayoutY(160);
        dealer.setFont(Font.font("Times New Roman", 24));
        dealer.setFill(Color.WHITE);
        
        //the dealer's hand value
        dealerHand = new Text();
        dealerHand.setLayoutX(695);
        dealerHand.setLayoutY(160);
        dealerHand.setFont(Font.font("Times New Roman", 24));
        dealerHand.setFill(Color.WHITE);
        
        //current turn indicator
        turnMarker = new Polygon[5];
        for (int i = 0; i < 5; i++)
        {
            turnMarker[i] = new Polygon();
            turnMarker[i].setFill(Color.web("#FFD000"));
            turnMarker[i].setVisible(false);
        }
        
        turnMarker[2].getPoints().addAll(new Double[]{186.0, 360.0, 224.0, 360.0, 205.0, 390.0});
        turnMarker[1].getPoints().addAll(new Double[]{306.0, 360.0, 344.0, 360.0, 325.0, 390.0});
        turnMarker[0].getPoints().addAll(new Double[]{620.0, 356.0, 658.0, 356.0, 639.0, 386.0});
        turnMarker[3].getPoints().addAll(new Double[]{936.0, 360.0, 974.0, 360.0, 955.0, 390.0});
        turnMarker[4].getPoints().addAll(new Double[]{1055.0, 360.0, 1093.0, 360.0, 1074.0, 390.0});
        
        playerFields = new TextField[4];
        playerFields[0] = player1Field;
        playerFields[1] = player2Field;
        playerFields[2] = player3Field;
        playerFields[3] = player4Field;
        
        cardArea = new Text[4];
        cardArea[0] = cardText1;
        cardArea[1] = cardText2;
        cardArea[2] = cardText3;
        cardArea[3] = cardText4;
        
        root.getChildren().addAll(btnStay, btnHit, betLabel, creditsLabel, betField, creditsField, 
                player1Field, player2Field, player3Field, player4Field, mainCardArea,
                cardArea[0], cardArea[1], cardArea[2], cardArea[3], message, dealer, dealerHand, 
                turnMarker[0], turnMarker[1], turnMarker[2], turnMarker[3], turnMarker[4]);
        
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setTitle("Blackjack");
        primaryStage.setScene(scene);
        primaryStage.setResizable(ALLOW_RESIZE);
        primaryStage.show();
        
        run();
    }
    
    public void cHit() throws IOException, ClassNotFoundException
    {
        supportedPlayer.setMove(Move.HIT);
        if (betField.getText().isEmpty())
            betField.setText("0");
        supportedPlayer.setBet(Integer.parseInt(betField.getText()));
        supportedPlayer.setTimeStamp(System.currentTimeMillis());
        toServer.writeObject(supportedPlayer);
        toServer.flush();
    }
    
    public void cStay() throws IOException, ClassNotFoundException
    {
        supportedPlayer.setMove(Move.STAY);
        if (betField.getText().isEmpty())
            betField.setText("0");
        supportedPlayer.setTimeStamp(System.currentTimeMillis());
        toServer.writeObject(supportedPlayer);
        toServer.flush();
    }
    
    private boolean connectToServer(String ip)
    {
        Socket socket = null;
        try
        {
            //new socket on port 8000, looking on local network for server
            socket = new Socket(ip, 8000);
            toServer = new ObjectOutputStream(socket.getOutputStream());
            toServer.flush();
            fromServer = new ObjectInputStream(socket.getInputStream());
        }
        catch (IOException e)
        {
            System.err.println(e);
        }
        
        if (socket == null)
            return false;
        return socket.isConnected();
    }
    
    @Override
    public void run() 
    {
        players = new LinkedList<>();
        new Thread(() ->
        {
            try
            {
                toServer.writeObject(supportedPlayer);
                toServer.flush();
                while (true) {
                    if (supportedPlayer.getSecondHand().size() <= 0) {
                        supportedPlayer.addCardSecondHand(HandleSession.deck.draw());
                        supportedPlayer.addCardSecondHand(HandleSession.deck.draw());
                    }
                    Player object = (Player) fromServer.readObject();

                    boolean isSet = false;
                    for (int i = 0; i < players.size(); i++) {
                        if (object.getID() == supportedPlayer.getID()) {

                            //System.out.println("same object1");
                            //System.out.println("Hand size: "+ supportedPlayer.getSecondHand().size());
                            isSet = true;
                            supportedPlayer = object;
                        } else if (players.get(i).getID() == object.getID()) {
                            isSet = true;
                            players.set(i, object);
                        }
                    }
                    if (isSet == false) {
                        if (object.getID() == supportedPlayer.getID()) {
                            supportedPlayer = object;
                        } else {
                            players.add(object);
                        }
                    }
                    updateFields();
                    System.out.println("The time on the client is " + System.currentTimeMillis());
                }
            }
            catch (IOException | ClassNotFoundException e)
            {
                System.err.println(e);
            }
            catch (IndexOutOfBoundsException e) {}
        }).start();
    }
    
    public void updateFields() {
        try {
            String cardCode = "";
            for (int i = 0; i < players.size(); i++) {
                cardCode = "";
                playerFields[i].setText(players.get(i).getName());
                if (players.get(i).getState() == State.ON) {
                    turnMarker[i].setVisible(true);
                } else {
                    turnMarker[i].setVisible(false);
                }
                for (Card c : players.get(i).getSecondHand()) {
                    cardCode += c.getUnicode();
                }
                cardArea[i].setText(cardCode);
            }
            //turnMarker[0].setVisible(true);
            if (supportedPlayer.getState() == State.ON) {
                turnMarker[0].setVisible(true);
            } else {
                turnMarker[0].setVisible(false);
            }

            cardCode = "";
            creditsField.setText(supportedPlayer.getCredits() + "");
            for (Card c : supportedPlayer.getSecondHand()) {
                cardCode += c.getUnicode();
            }

            mainCardArea.setText(cardCode);
            message.setText(supportedPlayer.getMessage());
            dealerHand.setText(supportedPlayer.getDealerValue() + "");
        } catch (ConcurrentModificationException e) {
            System.out.println("Update fields is the root of all of our issues");
        }
    }
    
    public static void main(String[] args)
    {
        launch(args);
    }
}
