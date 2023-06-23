import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 
 * The GameClient class represents a client application for a game.
 * 
 * It extends the Application class from the JavaFX framework.
 * 
 * The client connects to a server and allows the user to play a Pacman game and
 * interact with other players through a chat.
 */
public class Client extends Application {

    private final static String PACMAN_IMAGE = "pacmanA.png";
    private final static String MAZE_IMAGE = "background.jpg";
    private final static int PACMAN_SIZE = 40;
    private final static int MOVEMENT_SPEED = 5;

    private ImageView pacmanView;
    private Pane gamePane;
    private Pane chatPane;
    private Scene scene;
    private boolean isMovingUp;
    private boolean isMovingDown;
    private boolean isMovingLeft;
    private boolean isMovingRight;
    private PixelReader mazPixelReader;
    private MediaPlayer mediaPlayer;
    private Socket clientSocket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private TextArea chatTextArea;
    private TextField chatTextField;
    private Button sendButton;
    private boolean isConnected = false;

    /**
     * 
     * The main entry point for the GameClient application.
     * 
     * @param args command-line arguments
     */
    public static void main(String[] args) throws Exception {
        launch(args);
    }

    /**
     * 
     * Initializes and starts the GameClient application.
     * 
     * @param stage the primary stage for the application
     */
    @Override
    public void start(Stage stage) {
        stage.setTitle("Pacman");
        showInstructions();

        Image backgroundImage = new Image(MAZE_IMAGE);
        ImageView backgroundImageView = new ImageView(backgroundImage);

        gamePane = new Pane();
        gamePane.getChildren().add(backgroundImageView);

        pacmanView = new ImageView(new Image(PACMAN_IMAGE));
        pacmanView.setFitWidth(PACMAN_SIZE);
        pacmanView.setFitHeight(PACMAN_SIZE);

        double initialX = 50;
        double initialY = 615;

        pacmanView.setLayoutX(initialX);
        pacmanView.setLayoutY(initialY);

        gamePane.getChildren().add(pacmanView);

        chatPane = new Pane();
        Image characterImage = new Image("ghostA.png");
        RandomCharacter randomCharacter = new RandomCharacter(characterImage, backgroundImage.getWidth(),
                backgroundImage.getHeight());

        Image ghostBImage = new Image("ghostB.png");
        RandomCharacter randomCharacterB = new RandomCharacter(ghostBImage, backgroundImage.getWidth(),
                backgroundImage.getHeight());
        randomCharacterB.setFitWidth(PACMAN_SIZE);
        randomCharacterB.setFitHeight(PACMAN_SIZE);
        gamePane.getChildren().add(randomCharacterB);
        Image ghostCImage = new Image("ghostC.png");
        RandomCharacter randomCharacterC = new RandomCharacter(ghostCImage, backgroundImage.getWidth(),
                backgroundImage.getHeight());
        randomCharacterC.setFitHeight(PACMAN_SIZE);
        randomCharacterC.setFitWidth(PACMAN_SIZE);
        gamePane.getChildren().add(randomCharacterC);
        Image ghostDImage = new Image("ghostD.png");
        RandomCharacter randomCharacterD = new RandomCharacter(ghostDImage, backgroundImage.getWidth(),
                backgroundImage.getHeight());
        randomCharacterD.setFitHeight(PACMAN_SIZE);
        randomCharacterD.setFitWidth(PACMAN_SIZE);
        gamePane.getChildren().add(randomCharacterD);
        gamePane.getChildren().add(randomCharacter);
        randomCharacter.setFitHeight(PACMAN_SIZE);
        randomCharacter.setFitWidth(PACMAN_SIZE);
        chatTextArea = new TextArea();
        chatTextArea.setEditable(false);
        chatTextField = new TextField();
        chatTextField.setPromptText("Type your message here: ");
        sendButton = new Button("Send");
        sendButton.setOnAction(event -> sendMessage());

        VBox chatBox = new VBox(10);
        chatBox.setPadding(new Insets(10));
        chatBox.getChildren().addAll(chatTextArea, chatTextField, sendButton);
        chatPane.getChildren().add(chatBox);

        VBox root = new VBox(gamePane, chatPane);
        root.setPrefWidth(backgroundImage.getWidth());

        scene = new Scene(root);
        stage.setScene(scene);
        playMusic("music.mp3");

        stage.show();
        root.setStyle("-fx-background-color: #BDB76B;");
        mazPixelReader = backgroundImage.getPixelReader();
        generatePacDots(backgroundImage);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleArrowKeyPressed);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, this::handleArrowKeyReleased);

        /**
         * The game loop controls the game's frame rate and updates the game state.
         */
        AnimationTimer gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            /**
             * The handle method is called on each frame update.
             *
             * @param now The timestamp of the current frame.
             */

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 16_000_000) {
                    movePacman();
                    randomCharacter.moveRandomly();
                    randomCharacterB.moveRandomly();
                    randomCharacterC.moveRandomly();
                    randomCharacterD.moveRandomly();
                    lastUpdate = now;
                }
            }

        };
        gameLoop.start();

        // Create a client socket and set up input/output streams
        try {
            clientSocket = new Socket("localHost", 12345);
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            // Start a separate thread for listening to incoming messages
            Thread messageListener = new Thread(this::runMessageListener);
            messageListener.setDaemon(true);
            messageListener.start();

            isConnected = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the arrow key press events.
     *
     * @param event The KeyEvent representing the key press.
     */
    private void handleArrowKeyPressed(KeyEvent event) {
        KeyCode keyCode = event.getCode();
        switch (keyCode) {
            case UP:
                isMovingUp = true;
                break;

            case DOWN:
                isMovingDown = true;
                break;

            case LEFT:
                isMovingLeft = true;
                break;

            case RIGHT:
                isMovingRight = true;
                break;
        }
    }

    /**
     * Handles the arrow key release events.
     *
     * @param event The KeyEvent representing the key release.
     */
    private void handleArrowKeyReleased(KeyEvent event) {
        KeyCode keyCode = event.getCode();
        switch (keyCode) {
            case UP:
                isMovingUp = false;
                break;

            case DOWN:
                isMovingDown = false;
                break;

            case LEFT:
                isMovingLeft = false;
                break;

            case RIGHT:
                isMovingRight = false;
                break;
        }
    }

    /**
     * Moves the Pacman character based on the arrow key inputs and handles
     * collisions.
     */
    private void movePacman() {
        double layoutX = pacmanView.getLayoutX();
        double layoutY = pacmanView.getLayoutY();

        if (isMovingUp) {
            layoutY -= MOVEMENT_SPEED;
            pacmanView.setRotate(-90);
        } else if (isMovingDown) {
            layoutY += MOVEMENT_SPEED;
            pacmanView.setRotate(90);
        }

        if (isMovingLeft) {
            layoutX -= MOVEMENT_SPEED;
            pacmanView.setRotate(180);
        } else if (isMovingRight) {
            layoutX += MOVEMENT_SPEED;
            pacmanView.setRotate(0);
        }

        if (checkCollision(layoutX, layoutY, PACMAN_SIZE)) {
            // Collision detected with wall, stop movement
            return;
        }

        if (checkCollisionWithGhost(layoutX, layoutY, PACMAN_SIZE)) {
            showGameOverAlert();
            resetGame();
            return;
        }
        // Ensure Pacman stays within the game boundaries
        layoutX = Math.max(0, Math.min(layoutX, scene.getWidth() - PACMAN_SIZE));
        layoutY = Math.max(0, Math.min(layoutY, scene.getHeight() - PACMAN_SIZE));
        // Update Pacman's position
        pacmanView.setLayoutX(layoutX);
        pacmanView.setLayoutY(layoutY);
        // Check for collision with Pac-Dots
        checkPacDotCollision();
    }

    /**
     * Checks if there is a collision between Pacman and any ghost character.
     *
     * @param x    The x-coordinate of Pacman's position
     * @param y    The y-coordinate of Pacman's position
     * @param size The size of Pacman
     * @return True if collision with a ghost character occurs, false otherwise
     */

    private boolean checkCollisionWithGhost(double x, double y, double size) {
        List<RandomCharacter> ghostCharacters = new ArrayList<>();
        for (Node node : gamePane.getChildren()) {
            if (node instanceof RandomCharacter) {
                ghostCharacters.add((RandomCharacter) node);
            }
        }
        for (RandomCharacter ghostCharacter : ghostCharacters) {
            double ghostX = ghostCharacter.getLayoutX();
            double ghostY = ghostCharacter.getLayoutY();

            if (Math.abs(x - ghostX) < size && Math.abs(y - ghostY) < size) {
                return true;
            }
        }
        return false;
    }

    /**
     * Plays the specified music file.
     *
     * @param musicFile The path to the music file
     */
    private void playMusic(String musicFile) {
        try {
            Media sound = new Media(new File(musicFile).toURI().toString());
            mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows a game over alert dialog and handles user choices.
     */
    public void showGameOverAlert() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("GAME OVER");
            alert.setHeaderText(null);
            alert.setContentText("The game is over. Play again?");
            ButtonType playAgainButton = new ButtonType("Play Again");
            ButtonType exitButton = new ButtonType("Exit");
            alert.getButtonTypes().setAll(playAgainButton, exitButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == playAgainButton) {
                    resetGame();
                } else if (result.get() == exitButton) {
                    System.exit(0);
                }
            }
        });
    }

    /**
     * Resets the game by repositioning Pacman, removing Pac-Dots, and regenerating
     * the maze.
     */
    private void resetGame() {
        pacmanView.setLayoutX(50);
        pacmanView.setLayoutY(600);

        gamePane.getChildren().removeIf(node -> node instanceof Circle);
        generatePacDots(new Image(MAZE_IMAGE));
    }

    /**
     * Checks if there is a collision with the maze walls at the specified position.
     *
     * @param x    The x-coordinate of the position
     * @param y    The y-coordinate of the position
     * @param size The size of the object
     * @return True if collision with a wall occurs, false otherwise
     */
    private boolean checkCollision(double x, double y, double size) {
        for (double i = x; i < x + size; i++) {
            for (double j = y; j < y + size; j++) {
                int pixelColor = mazPixelReader.getArgb((int) i, (int) j);
                if (pixelColor == 0xFF000000) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Sends a chat message to the server if connected.
     */
    private void sendMessage() {
        if (isConnected) {
            String message = chatTextField.getText().trim();
            if (!message.isEmpty()) {
                try {
                    writer.write(message);
                    writer.newLine();
                    writer.flush();
                    chatTextField.clear();
                } catch (IOException e) {

                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * Listens for incoming messages from the server and updates the chat text area.
     */
    private void runMessageListener() {
        while (true) {
            try {
                String message = reader.readLine();
                if (message != null) {
                    chatTextArea.appendText(message + " \n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Displays the game instructions in an alert dialog.
     */
    private void showInstructions() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("GAME INSTRUCTIONS:");
        alert.setHeaderText(null);
        alert.setContentText("Welcome to Pacman!\n\n" +
                "Navigate Pacman using the arrow keys.\n" +
                "Collect all the dots in the maze to win.\n" +
                "Avoid contact with the ghosts, or you lose.\n" +
                "Have fun and enjoy the game!");

        ButtonType closeButton = new ButtonType("Close");
        alert.getButtonTypes().setAll(closeButton);

        alert.showAndWait();
    }

    /**
     * Generates Pac-Dots on the game pane based on the background image.
     *
     * @param backgroundImage The background image of the maze
     */
    private void generatePacDots(Image backgroundImage) {
        double width = backgroundImage.getWidth();
        double height = backgroundImage.getHeight();

        double cellWidth = PACMAN_SIZE;
        double cellHeight = PACMAN_SIZE;

        double dotRadius = PACMAN_SIZE / 8;

        int stepSize = 2;

        for (double x = 0; x < width; x += cellWidth * stepSize) {
            for (double y = 0; y < height; y += cellHeight * stepSize) {
                boolean isValidPosition = checkValidPosition(x, y, cellWidth, cellHeight);
                if (isValidPosition) {
                    Circle pacDot = new Circle(x + cellWidth / 2, y + cellHeight / 2, dotRadius, Color.BLUE);
                    gamePane.getChildren().add(pacDot);
                }
            }
        }
    }

    /**
     * Checks if the specified position is valid for placing a Pac-Dot.
     *
     * @param x          The x-coordinate of the position
     * @param y          The y-coordinate of the position
     * @param cellWidth  The width of a cell in the maze
     * @param cellHeight The height of a cell in the maze
     * @return True if the position is valid, false otherwise
     */

    private boolean checkValidPosition(double x, double y, double cellWidth, double cellHeight) {
        for (double i = x; i < x + cellWidth; i++) {
            for (double j = y; j < y + cellHeight; j++) {
                int pixelColor = mazPixelReader.getArgb((int) i, (int) j);
                if (pixelColor == 0xFF000000) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks for collision between Pacman and Pac-Dots, and handles the collision.
     */
    private void checkPacDotCollision() {
        double pacmanCenterX = pacmanView.getLayoutX() + PACMAN_SIZE / 2;
        double pacmanCenterY = pacmanView.getLayoutY() + PACMAN_SIZE / 2;

        for (Node node : gamePane.getChildren()) {
            if (node instanceof Circle) {
                Circle pacDot = (Circle) node;
                double pacDotCenterX = pacDot.getCenterX();
                double pacDotCenterY = pacDot.getCenterY();

                if (Math.abs(pacmanCenterX - pacDotCenterX) < PACMAN_SIZE / 2
                        && Math.abs(pacmanCenterY - pacDotCenterY) < PACMAN_SIZE / 2) {
                    gamePane.getChildren().remove(pacDot);
                    playPacDotSound();
                    break;
                }
            }
        }
        if (gamePane.getChildren().stream().filter(node -> node instanceof Circle).count() == 0) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("CONGRATULATIONS!");
            alert.setHeaderText(null);
            alert.setContentText("Congratulations! You have won!");
            alert.setOnHidden(event -> System.exit(0));
            alert.show();
        }
    }

    /**
     * Plays the sound effect for collecting a Pac-Dot.
     */
    private void playPacDotSound() {
        String soundFile = "sound.mp3";
        AudioClip sound = new AudioClip(new File(soundFile).toURI().toString());
        sound.play();
    }

    /**
     * A class representing a random character(ghosts) in the game.
     */

    class RandomCharacter extends ImageView {
        private static final int MOVEMENT_SPEED = 5;

        private Random random;
        private double mazeWidth;
        private double mazeHeight;
        private double direction;

        /**
         * Constructs a RandomCharacter object with the specified image and maze
         * dimensions.
         *
         * @param image      The image representing the random character
         * @param mazeWidth  The width of the maze
         * @param mazeHeight The height of the maze
         */

        public RandomCharacter(Image image, double mazeWidth, double mazeHeight) {
            super(image);
            random = new Random();
            this.mazeWidth = mazeWidth;
            this.mazeHeight = mazeHeight;
            direction = random.nextDouble() * 360;
            startRandomMovement();
        }

        /**
         * Moves the random character in a random direction within the maze.
         */

        public void moveRandomly() {
            double layoutX = getLayoutX();
            double layoutY = getLayoutY();

            layoutX += MOVEMENT_SPEED * Math.cos(Math.toRadians(direction));
            layoutY += MOVEMENT_SPEED * Math.sin(Math.toRadians(direction));

            if (layoutX < 0 || layoutX > mazeWidth - getImage().getWidth()) {
                direction = 180 - direction;
            }
            if (layoutY < 0 || layoutY > mazeHeight - getImage().getHeight()) {
                direction = 360 - direction;
            }
            setLayoutX(layoutX);
            setLayoutY(layoutY);
        }

        /**
         * Starts the random movement of the character by generating a random direction.
         */

        private void startRandomMovement() {
            direction = random.nextDouble() * 360;
        }

    }
}
