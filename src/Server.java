
/**
 * GameServer class represents a server for a game.
 * It allows multiple clients to connect and communicate with each other.
 */

import javafx.application.*;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.geometry.*;
import java.io.*;
import java.net.*;
import java.util.*;

// Server class with global attributes
public class Server extends Application {
  private ServerSocket serverSocket;
  private List<ClientHandler> clients = new ArrayList<>();
  private TextArea chatTextArea;
  public static final int SERVERPORT = 12345;

  /**
   * The main method of the GameServer class.
   * It launches the JavaFX application.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) throws Exception {
    launch(args);
  }

  /**
   * The start method of the JavaFX application.
   * It initializes the GUI components and starts the server thread.
   *
   * @param stage the primary stage for the JavaFX application
   */
  public void start(Stage stage) throws Exception {
    stage.setTitle("Pacman Server");

    chatTextArea = new TextArea();
    chatTextArea.setEditable(false);

    VBox root = new VBox();
    root.setSpacing(10);
    root.setPadding(new Insets(10));
    root.getChildren().add(chatTextArea);

    Scene scene = new Scene(root, 300, 300);
    stage.setScene(scene);
    stage.show();

    Thread serverThread = new Thread(this::runServer);
    serverThread.setDaemon(true);
    serverThread.start();
  }

  /**
   * The runServer method starts the server and accepts client connections.
   * It creates a ClientHandler thread for each client.
   */
  private void runServer() {
    try {
      serverSocket = new ServerSocket(SERVERPORT);
      Platform.runLater(() -> chatTextArea.appendText("Server started on PORT " + SERVERPORT + "\n"));

      while (true) {
        Socket clientSocket = serverSocket.accept();

        ClientHandler clientHandler = new ClientHandler(clientSocket);
        clients.add(clientHandler);
        clientHandler.start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * The broadcastMessage method broadcasts a message to all connected clients.
   *
   * @param message the message to broadcast
   */
  private void broadcastMessage(String message) {
    Platform.runLater(() -> chatTextArea.appendText(message + " \n"));

    for (ClientHandler client : clients) {
      client.sendMessage(message);
    }
  }

  /**
   * The removeClient method removes a client from the list of connected clients.
   * It also broadcasts a message to inform other clients about the disconnection.
   *
   * @param client the client to remove
   */
  private void removeClient(ClientHandler client) {
    clients.remove(client);
    broadcastMessage(client.getClientID() + "has disconnected!");
  }

  /**
   * The ClientHandler class represents a thread that handles communication with a
   * client.
   */
  public class ClientHandler extends Thread {
    private Socket clientSocket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String clientId;

    /**
     * Constructor for the ClientHandler class.
     *
     * @param socket the client socket
     */
    public ClientHandler(Socket socket) {
      clientSocket = socket;
      clientId = clients.size() + " ";
    }

    /**
     * Returns the client ID.
     *
     * @return the client ID
     */
    public String getClientID() {
      return clientId;
    }

    /**
     * The run method of the ClientHandler thread.
     * It handles communication with the client, receives messages, and broadcasts
     * them to other clients.
     */
    @Override
    public void run() {
      try {
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

        broadcastMessage("Client" + clientId + "connected");
        String input;
        while ((input = reader.readLine()) != null) {
          broadcastMessage("Client" + clientId + ": " + input);
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {

        try {

          if (reader != null) {
            reader.close();
          }
          if (writer != null) {
            writer.close();
          }
          if (clientSocket != null) {
            clientSocket.close();
          }

        } catch (IOException e) {
          e.printStackTrace();
        }

      }

      removeClient(this);
    }

    /**
     * Sends a message to the client.
     *
     * @param message the message to send
     */
    public void sendMessage(String message) {
      try {
        writer.write(message);
        writer.newLine();
        writer.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }

    }

  }

}
