package blackjack;

import static blackjack.BlackjackConstants.BACKGROUND2;
import blackjack.Player.Move;
import blackjack.Player.State;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * @author Tanner Lisonbee
 */
public class Client extends Application implements Runnable, BlackjackConstants
{
    //Initializes Variables
    private ObjectInputStream fromServer;
    private ObjectOutputStream toServer;
    private TextField creditsField, betField;
    private Text mainCardArea, message;
    private TextField[] playerFields;
    private Text[] cardArea;
    private Polygon[] turnMarker;
    private String ip;
    private List<Player> players;
    private Player supportedPlayer;
    private int turnN;
    private boolean isThePlayerSeated = false;
    
    //Begins the UI Code
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
        TextField ipInput = new TextField("localhost");
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
        Label errorMessage = new Label("Welcome");
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
        //grid.setPadding(new Insets(10, 10, 10, 10));
        //grid.setVgap(8);
        //grid.setHgap(10);
        //grid.setAlignment(Pos.CENTER);
        
        //add column and row constraints to keep elements in the proper position
        /*
        grid.getColumnConstraints().add(new ColumnConstraints(235));
        grid.getColumnConstraints().add(new ColumnConstraints(75));
        grid.getColumnConstraints().add(new ColumnConstraints(250));
        grid.getColumnConstraints().add(new ColumnConstraints(250));
        grid.getColumnConstraints().add(new ColumnConstraints(345));
        grid.getColumnConstraints().add(new ColumnConstraints(75));
        grid.getRowConstraints().add(new RowConstraints(50));
        grid.getRowConstraints().add(new RowConstraints(650));*/
        
        //current bet label
        Label betLabel = new Label("CURRENT BET: ");
        betLabel.setFont(Font.font("Times New Roman", 32));
        betLabel.setTextFill(Color.web("#FFD000"));
        
        //current bet field
        betField = new TextField();
        //betField.setEditable(false);
        betField.setFont(Font.font("Times New Roman", 24));
        //betField.setPrefHeight(34);
        betField.setPrefWidth(80);
        betField.setLayoutX(235);
        
        //credits available label
        Label creditsLabel = new Label("CREDITS AVAILABLE: ");
        creditsLabel.setFont(Font.font("Times New Roman", 32));
        creditsLabel.setTextFill(Color.web("#FFD000"));
        creditsLabel.setLayoutX(810);        
        //credits available field
        creditsField = new TextField();
        
        creditsField.setEditable(false);
        creditsField.setFont(Font.font("Times New Roman", 24));
        //creditsField.setPrefHeight(34);
        creditsField.setPrefWidth(80);
        creditsField.setLayoutX(1155);
        
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
        });
        
         playerFields = new TextField[4];
        
        Button btnChair1 = new Button("Chair 1");
        Button btnChair2 = new Button("Chair 2");
        Button btnChair3 = new Button("Chair 3");
        Button btnChair4 = new Button("Chair 4");
        Button btnChair5 = new Button("Chair 5");
        
        
        
        
        btnChair1.setLayoutX(605);
        btnChair1.setLayoutY(535);
        btnChair1.setFont(Font.font("Times New Roman", 16));
        btnChair1.setOnAction((ActionEvent event) -> 
        {
            //Places the selected Avatar
            root.getChildren().remove(btnChair1);
            root.getChildren().remove(btnChair2);
            root.getChildren().remove(btnChair3);
            root.getChildren().remove(btnChair4);
            root.getChildren().remove(btnChair5);
            String avatar = display("Avatar Selection Screen", "");
            Image image = new Image(avatar);
            ImageView player1 = new ImageView();
            ImageView boxPlacement = new ImageView();
            player1.setImage(image);
            player1.setX(500);
            player1.setY(500);
            root.getChildren().add(player1);
            
            //Box filled with info
            Image box = new Image("images/BoxMan.png");
            boxPlacement.setImage(box);
            boxPlacement.setX(605);
            boxPlacement.setY(540);
            root.getChildren().add(boxPlacement);
            
           });

        
        btnChair2.setLayoutX(80);
        btnChair2.setLayoutY(400);
        btnChair2.setFont(Font.font("Times New Roman", 16));
        btnChair2.setOnAction((ActionEvent event) -> 
        {
            //Places the selected Avatar
            root.getChildren().remove(btnChair1);
            root.getChildren().remove(btnChair2);
            root.getChildren().remove(btnChair3);
            root.getChildren().remove(btnChair4);
            root.getChildren().remove(btnChair5);
            String avatar = display("Avatar Selection Screen", "");
            ImageView boxPlacement2 = new ImageView();
            Image image = new Image(avatar);
            ImageView player2 = new ImageView();
            player2.setImage(image);
            player2.setLayoutX(80);
            player2.setLayoutY(400);
            root.getChildren().add(player2);
            
            //Box filled with info
            Image box = new Image("images/BoxMan.png");
            boxPlacement2.setImage(box);
            boxPlacement2.setX(182);
            boxPlacement2.setY(450);
            root.getChildren().add(boxPlacement2);
            
        });
        
        btnChair3.setLayoutX(80);
        btnChair3.setLayoutY(190);
        btnChair3.setFont(Font.font("Times New Roman", 16));
        btnChair3.setOnAction((ActionEvent event) -> 
        {
            root.getChildren().remove(btnChair1);
            root.getChildren().remove(btnChair2);
            root.getChildren().remove(btnChair3);
            root.getChildren().remove(btnChair4);
            root.getChildren().remove(btnChair5);
            String avatar = display("Avatar Selection Screen", "");
            ImageView boxPlacement3 = new ImageView();
            Image image = new Image(avatar);
            ImageView player3 = new ImageView();
            player3.setLayoutX(80);
            player3.setLayoutY(100);
            player3.setImage(image);
            root.getChildren().add(player3);
            
            //Box filled with info
            Image box = new Image("images/BoxMan.png");
            boxPlacement3.setImage(box);
            boxPlacement3.setX(183);
            boxPlacement3.setY(100);
            root.getChildren().add(boxPlacement3);
        });
        
        btnChair4.setLayoutX(1155);
        btnChair4.setLayoutY(190);
        btnChair4.setFont(Font.font("Times New Roman", 16));
        btnChair4.setOnAction((ActionEvent event) -> 
        {
            root.getChildren().remove(btnChair1);
            root.getChildren().remove(btnChair2);
            root.getChildren().remove(btnChair3);
            root.getChildren().remove(btnChair4);
            root.getChildren().remove(btnChair5);
            String avatar = display("Avatar Selection Screen", "");
            ImageView boxPlacement4 = new ImageView();
            Image image = new Image(avatar);
            ImageView player4 = new ImageView();
            player4.setLayoutX(1120);
            player4.setLayoutY(100);
            player4.setImage(image);
            root.getChildren().add(player4);
            
            //Box filled with info
            Image box = new Image("images/BoxMan.png");
            boxPlacement4.setImage(box);
            boxPlacement4.setX(979);
            boxPlacement4.setY(100);
            root.getChildren().add(boxPlacement4);
        });
        
        btnChair5.setLayoutX(1155);
        btnChair5.setLayoutY(400);
        btnChair5.setFont(Font.font("Times New Roman", 16));
        btnChair5.setOnAction((ActionEvent event) -> 
        {
            root.getChildren().remove(btnChair1);
            root.getChildren().remove(btnChair2);
            root.getChildren().remove(btnChair3);
            root.getChildren().remove(btnChair4);
            root.getChildren().remove(btnChair5);
            String avatar = display("Avatar Selection Screen", "");
            ImageView boxPlacement5 = new ImageView();
            Image image = new Image(avatar);
            ImageView player5 = new ImageView();
            player5.setLayoutX(1120);
            player5.setLayoutY(400);
            player5.setImage(image);
            root.getChildren().add(player5);
            
            //Box filled with info
            Image box = new Image("images/BoxMan.png");
            boxPlacement5.setImage(box);
            boxPlacement5.setX(979);
            boxPlacement5.setY(460);
            root.getChildren().add(boxPlacement5);
        });
        
        //player 1
        TextField player1Field = new TextField();
        player1Field.setLayoutX(157);
        player1Field.setLayoutY(550);
        player1Field.setEditable(false);
        player1Field.setPrefWidth(96);
        player1Field.setFont(Font.font("Times New Roman"));
        
        //player 2
        TextField player2Field = new TextField();
        player2Field.setLayoutX(276);
        player2Field.setLayoutY(550);
        player2Field.setEditable(false);
        player2Field.setPrefWidth(96);
        player2Field.setFont(Font.font("Times New Roman"));
        
        //player 3
        TextField player3Field = new TextField();
        player3Field.setLayoutX(908);
        player3Field.setLayoutY(550);
        player3Field.setEditable(false);
        player3Field.setPrefWidth(96);
        player3Field.setFont(Font.font("Times New Roman"));
        
        //player 4
        TextField player4Field = new TextField();
        player4Field.setLayoutX(1026);
        player4Field.setLayoutY(550);
        player4Field.setEditable(false);
        player4Field.setPrefWidth(96);
        player4Field.setFont(Font.font("Times New Roman"));
        
        //card area for player1
        Text cardText1 = new Text();
        cardText1.setLayoutX(170);
        cardText1.setLayoutY(490);
        cardText1.setFont(Font.font("Times New Roman", 51));
        cardText1.setFill(Color.web("#FFD000"));
        cardText1.setWrappingWidth(70);
        
        //card area for player2
        Text cardText2 = new Text();
        cardText2.setLayoutX(289);
        cardText2.setLayoutY(490);
        cardText2.setFont(Font.font("Times New Roman", 51));
        cardText2.setFill(Color.web("#FFD000"));
        cardText2.setWrappingWidth(70);
        
        //card area for player3
        Text cardText3 = new Text();
        cardText3.setLayoutX(920);
        cardText3.setLayoutY(490);
        cardText3.setFont(Font.font("Times New Roman", 51));
        cardText3.setFill(Color.web("#FFD000"));
        cardText3.setWrappingWidth(70);
        
        //card area for player4
        Text cardText4 = new Text();
        cardText4.setLayoutX(1039);
        cardText4.setLayoutY(490);
        cardText4.setFont(Font.font("Times New Roman", 51));
        cardText4.setFill(Color.web("#FFD000"));
        cardText4.setWrappingWidth(70);
        
        //main card area
        mainCardArea = new Text();
        mainCardArea.setLayoutX(582);
        mainCardArea.setLayoutY(494);
        mainCardArea.setFont(Font.font("Times New Roman", 88));
        mainCardArea.setText("\uD83C\uDCA0");
        mainCardArea.setFill(Color.web("#FFD000"));
        mainCardArea.setWrappingWidth(117);
        
        message = new Text();
        message.setLayoutX(10);
        message.setLayoutY(700);
        message.setFont(Font.font("Times New Roman", 16));
        message.setFill(Color.WHITE);
        
        //current turn indicator
        turnMarker = new Polygon[5];
        for (int i = 0; i < 5; i++)
        {
            turnMarker[i] = new Polygon();
            turnMarker[i].setFill(Color.web("#FFD000"));
            turnMarker[i].setVisible(false);
        }
        
        turnMarker[2].getPoints().addAll(new Double[]{186.0, 390.0, 224.0, 390.0, 205.0, 420.0});
        turnMarker[1].getPoints().addAll(new Double[]{306.0, 390.0, 344.0, 390.0, 325.0, 420.0});
        turnMarker[0].getPoints().addAll(new Double[]{620.0, 356.0, 658.0, 356.0, 639.0, 386.0});
        turnMarker[3].getPoints().addAll(new Double[]{936.0, 390.0, 974.0, 390.0, 955.0, 420.0});
        turnMarker[4].getPoints().addAll(new Double[]{1055.0, 390.0, 1093.0, 390.0, 1074.0, 420.0});
        
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
        /*
        fieldPane.getChildren().addAll(btnStay, btnHit);
        grid.getChildren().addAll(betLabel, creditsLabel, betField, creditsField);
        fieldPane.getChildren().addAll(player1Field, player2Field, player3Field, player4Field, mainCardArea);
        //fieldPane.getChildren().addAll(turnMarker[0], turnMarker[1], turnMarker[2], turnMarker[3], turnMarker[4]);*/
        root.getChildren().addAll(btnStay, btnHit, betLabel, creditsLabel, betField, creditsField, btnChair1, btnChair2, btnChair3, btnChair4, btnChair5,
                player1Field, player2Field, player3Field, player4Field, mainCardArea,
                cardArea[0], cardArea[1], cardArea[2], cardArea[3],message, 
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
        
        toServer.writeObject(supportedPlayer);
        toServer.flush();
        
        //System.out.println("cHit");
        //Object play = fromServer.readObject();
        //Player pl = (Player) play;
        //players.set(0, pl);
        //System.out.println("cHit2");
        
        //String credits = "";
        
        //credits += players.get(0).getCredits();
        //creditsField.setText((credits));
    }
    
    public void cStay() throws IOException, ClassNotFoundException
    {
        supportedPlayer.setMove(Move.STAY);
        if (betField.getText().isEmpty())
            betField.setText("0");
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
        
        //new thread for connecting to server
        //new Thread(this).start();
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
                while (true)
                {
                    if(supportedPlayer.getSecondHand().size() <= 0)
                    {
                        supportedPlayer.addCardSecondHand(HandleSession.deck.draw());
                        supportedPlayer.addCardSecondHand(HandleSession.deck.draw());
                    }
                    Player object = (Player)fromServer.readObject();
                    
                    boolean isSet = false;
                    
                    for (int i = 0; i < players.size(); i++)
                        if (object.getID() == supportedPlayer.getID())
                        {
                            System.out.println("same object1");
                            System.out.println("Hand size: "+supportedPlayer.getSecondHand().size());
                            
                            isSet = true;
                            supportedPlayer = object;
                            System.out.println("state:" + supportedPlayer.getState());
                        }
                        else if (players.get(i).getID() == object.getID())
                        {
                            isSet = true;
                            players.set(i, object);
                        }
                               
                    if (isSet == false)
                        if (object.getID() == supportedPlayer.getID())
                        {
                            System.out.println("same object2");
                            System.out.println("Hand size: "+supportedPlayer.getSecondHand().size());
                            supportedPlayer = object;
                            System.out.println("state:" + supportedPlayer.getState());
                        }
                        else
                            players.add(object);
                    //System.out.println(players.size());
                    
                    updateFields();
                    
                    if(players.size() <=0){
                        System.out.println("on");
                        supportedPlayer.setState(State.ON);
                    }
                }
            }
            catch (IOException | ClassNotFoundException e)
            {
                System.err.println(e);
            }
            catch (IndexOutOfBoundsException e) {}
        }).start();
    }
    
    public void updateFields()
    {
        String cardCode = "";
        for (int i = 0; i < players.size(); i++)
        {
            cardCode = "";
            playerFields[i].setText(players.get(i).getName());
            if (players.get(i).getState() == State.ON)
                turnMarker[i].setVisible(true);
            else
                turnMarker[i].setVisible(false);
            for (Card c : players.get(i).getSecondHand()) {
                cardCode += c.getUnicode();
            }
            cardArea[i].setText(cardCode);
        }
        //turnMarker[0].setVisible(true);
        if (supportedPlayer.getState() == State.ON)
            turnMarker[0].setVisible(true);
        else
            turnMarker[0].setVisible(false);
        
        creditsField.setText(supportedPlayer.getCredits() + "");
        for (Card c : supportedPlayer.getSecondHand()) {
            cardCode += c.getUnicode();
        }
        mainCardArea.setText(cardCode);
        message.setText(supportedPlayer.getMessage());
        
        System.out.println("List of names:");
        for (Player p : players) {
            System.out.print(p.getName() + " - ");
        }
        System.out.println();
    }
 static String avatar;
    
        public static String display(String title, String message){
        Stage window = new Stage();
        window.setTitle(title);
        String Avatar;
        StackPane root = new StackPane();
        GridPane grid = new GridPane();
        Pane buttonPane = new Pane();
        
        //set and configure background
        grid.setStyle("-fx-background-image: url(" + BACKGROUND2 + "); \n" +
                      "-fx-background-position: center center; \n" +
                      "-fx-background-repeat: stretch; \n" + 
                      "-fx-background-size: 1280 720;");
        //grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        //grid.setHgap(10);
        grid.setAlignment(Pos.CENTER);
        
        //add column and row constraints to keep elements in the proper position
        grid.getColumnConstraints().add(new ColumnConstraints(235));
        grid.getColumnConstraints().add(new ColumnConstraints(75));
        grid.getColumnConstraints().add(new ColumnConstraints(250));
        grid.getColumnConstraints().add(new ColumnConstraints(250));
        grid.getColumnConstraints().add(new ColumnConstraints(345));
        grid.getColumnConstraints().add(new ColumnConstraints(75));
        grid.getRowConstraints().add(new RowConstraints(50));
        grid.getRowConstraints().add(new RowConstraints(650));
        avatar = "";
        window.initModality(Modality.APPLICATION_MODAL);
        
        Label label = new Label();
        label.setText(message);
        
        Button avatarOne = new Button("Confirm");
        avatarOne.setLayoutX(120);
        avatarOne.setLayoutY(250);
        avatarOne.setOnAction(e->{
            avatar = "images/PlayerMan.png";
            window.close();
        });
        
        Button avatarTwo =  new Button("Confirm");
        avatarTwo.setLayoutX(560);
        avatarTwo.setLayoutY(250);
        avatarTwo.setOnAction(e->{
            avatar = "images/TrashMan.png";
            window.close();
            });
        Button avatarThree = new Button("Confirm");
        avatarThree.setLayoutX(1005);
        avatarThree.setLayoutY(250);
        avatarThree.setOnAction(e->{
            avatar = "images/Banana.png";
            window.close();
            });
        Button avatarFour = new Button("Confirm");
        avatarFour.setLayoutX(325);
        avatarFour.setLayoutY(620);
        avatarFour.setOnAction(e->{
            avatar = "images/MoneyMan.png";
            window.close();
            });
        Button avatarFive = new Button("Confirm");
        avatarFive.setLayoutX(830);
        avatarFive.setLayoutY(620);
        avatarFive.setOnAction(e->{
            avatar = "images/MonopolyCar.png";
            window.close();
        });

        buttonPane.getChildren().addAll(label, avatarOne, avatarTwo, avatarThree, avatarFour, avatarFive);
        root.getChildren().addAll(grid, buttonPane);

        
        Scene scene = new Scene(root);
        window.setScene(scene);
        window.showAndWait();
        
        return avatar;
        }
        
    public static void main(String[] args)
    {
        launch(args);
    }
}