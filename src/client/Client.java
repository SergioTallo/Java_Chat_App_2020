package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import security.RSAKeyHandling;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Client extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("clientgui.fxml"));
        primaryStage.setTitle("Client");
        primaryStage.setScene(new Scene(root, 750, 750));
        primaryStage.show();

        //To close the program with the x on the window
        primaryStage.setOnCloseRequest(windowEvent -> eventClose());
    }

    @FXML
    private TextField txt_host;
    @FXML
    private TextField txt_port;
    @FXML
    private TextArea txt_log;
    @FXML
    private TextField txt_password;
    @FXML
    private TextField txt_login;
    @FXML
    private TextField txt_logged_in;
    @FXML
    private ListView<String> txt_online;

    static PrivateKey ownPrivateKey;
    static PublicKey ownPublicKey;
    static PublicKey serverPublicKey;
    static Socket client;
    static String userName;

    private String logged;
    private String host;
    private int port;

    /**
     *
     * @brief Button connect. Take the host address and port from text fields and connect to server.
     * When connected creates a thread that remains in "listen" mode, listening all the incoming
     * messages from the server and acting in consequence
     *
     */
    public void eventConnect() {

        //Method to get all the Data written by the user
        getDataToConnect();

        if (logged.equals("Not logged")) {

            //Method managing the conection to the server
            client = ClientConnectionsMethods.connectToServer(host, port);

            //First client and server exchange the public keys for encrypted communication
            assert client != null;
            RSAKeyHandling.sendKeys(ownPublicKey, client);
            serverPublicKey = RSAKeyHandling.receiveKeys(client);

            //If the connection is succesfull
            if (client != null) {

                //Thread started and remains in "Listen Mode"
                new Client.Listen().start();

                //Get Login Username from text field and send to the server
                userName = txt_login.getText();
                ReceiveAndSendMethods.sendMessageServer(userName);

                //Get Password as Hashcode from text field and send to the server
                ReceiveAndSendMethods.sendMessageServer(Integer.toString(txt_password.getText().hashCode()));

            } else {
                log("Client: No conection possible to: " + host + ":" + port);
            }
        } else {
            log("You are already logged");
        }
    }

    /**
     *
     * @brief Method to get all the data write by the user
     *
     */
    public void getDataToConnect() {

        //Get Host, username and login info from text fields
        logged = txt_logged_in.getText();
        host = txt_host.getText();

        //Get the port number from text field
        try {
            port = Integer.parseInt(txt_port.getText());
        } catch (NumberFormatException e) {
            log("Client: Wrong Port entry");
        }

        //Shows who is online
        txt_online.setItems(ClientLists.onlineUsers);

    }

    /**
     *
     * @brief New Thread to manage the Listen Mode
     *
     */
    private class Listen extends Thread {

        @Override
        public void run() {
            boolean breakWhile = false;
            String input;

            //Keeps running till he receive a disconnect message from the server (triggered by this client)
            //or an exception (server closed unexpectedly). After reading pass the message to the correct method
            //to manage it, and returns to listen again.
            while (client != null && !breakWhile) {

                //Open stream and remains listening till a message comes. Than read it.
                input = readMessage();

                if (input == null){
                    break;
                }

                if (input.startsWith("$$Client&&Message##")) { //it means is a message originally coming from another client
                    try {
                        messageFromUser(input);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (input.startsWith("$$Server&&Message##")) { //it means is a message with orders and info from the server
                    breakWhile = messageFromServer(input);
                }
            }

            //The communication is closed. Therefore socket must be closed. Status changed to Not logged
            //and list of online users cleared.
            try {
                assert client != null;
                client.close();

                //This Platform.runLater is to avoid Not on FX application thread Exception
                //Clear users online list
                Platform.runLater(() -> ClientLists.onlineUsers.clear());
                txt_logged_in.setText("Not logged");

            } catch (UnsupportedOperationException | IOException e) {
                log("Connection with the server broke unexpectedly");
                e.printStackTrace();
            }
        }

        /**
         *
         * @brief Method to read from the stream and to store the input in a string
         *
         * @return String String coming from the server
         *
         */
        public String readMessage() {
            String input;
            try {
                //Read from the stream, store in an object
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());
                Object inputObject = in.readObject();

                //Decrypt this object
                byte[] byteString = RSAKeyHandling.rsaDecrypt((byte[]) inputObject, ownPrivateKey);
                assert byteString != null;

                //Convert the bytes in a readable String and return it
                input = new String(byteString, StandardCharsets.UTF_8);
                return input;

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     *
     * @brief Method managing the server incoming specific messages. They start with the code $$Server&&Message##
     * and have different meanings.
     *
     * @param input Incoming message
     *
     * @return true if the client have to close the communication with the server
     *
     *
     */
    private boolean messageFromServer(String input) {

        //Manage the different server specific messages:
        //OptionClientDisconnect: The server want to close the communication
        //LogInCorrect: Login process was successful
        //LogInIncorrect: Login process was not successful
        //clear + username: Remove username from the online users list
        //newlog + username_ Add username to the online users list

        if (input.startsWith("$$Server&&Message##OptionClientDisconnect")) {
            log("Connection with " + client.getInetAddress() + " closed");
            return true;
        } else if (input.startsWith("$$Server&&Message##LogInCorrect")) {
            log("Client: Succesfully connected to: " + client.getInetAddress() + ":" + port);
            txt_logged_in.setText("logged as " + txt_login.getText());
            return false;
        } else if (input.startsWith("$$Server&&Message##LogInIncorrect")) {
            log("Server: Incorrect Password for " + txt_login.getText());
            return true;
        } else if (input.startsWith("$$Server&&Message##clear")) {
            //It comes withe the format "$$Server&&Message##clear,user" and have to be split
            String[] token = input.split(",");
            int field = 0;

            for (String userName : token) {
                //Field 0 contains $$Server&&Message##clear and we don't need to store that
                if (field != 0) {

                    //This Platform.runLater is to avoid Not on FX application thread Exception
                    Platform.runLater(() -> ClientLists.onlineUsers.remove(userName));

                }
                field += 1;
            }
            return false;
        } else if (input.startsWith("$$Server&&Message##newlog")) {
            //It comes withe the format "$$Server&&Message##newlog,user,user,user" and have to be split
            String[] token = input.split(",");
            int field = 0;

            for (String userName : token) {
                //Field 0 contains $$Server&&Message##newlog and we don't need to store that
                if (field != 0) {

                    //The other fields have the name of an user who is online
                    boolean check = ClientLists.searchonlineUsers(userName);
                    if (!check) {

                        //This Platform.runLater is to avoid Not on FX application thread Exception
                        Platform.runLater(() -> ClientLists.onlineUsers.add(userName));

                    }
                    return false;
                }
                field += 1;
            }
        }
        return false;
    }

    /**
     *
     * @brief Method managing an incoming message that initally comes from another client
     *
     * @param input String with the message
     *
     * @throws IOException
     *
     */
    public void messageFromUser(String input) throws IOException {

        //First the string has to be splitted into user and message
        String[] token = input.split(";#&");
        String incomingUser = null;
        String message = null;
        int field = 0;

        for (String inputMessage : token) {
            if (field == 1) {
                incomingUser = inputMessage;
            } else if (field == 2) {
                message = inputMessage;
            }
            field += 1;
        }

        assert message != null;

        //If the message started with this code %%&&incomingfile&&%% means that a file is coming
        if (message.startsWith("%%&&incomingfile&&%%")){
            receivingFile(input);

        } else {
            //If not, is a text message
            receivingMessage(incomingUser, message);
        }
    }

    /**
     *
     * @brief Method managing the incoming message if its a text message
     *
     * @param incomingUser User who sends the message
     *
     * @param message String with the message
     *
     */
    private void receivingMessage(String incomingUser, String message) {

        //Check if there is already a chat window open with that user
        boolean exists = ClientLists.searchChatUser(incomingUser);

        if (exists) {
            //If yes, write the message in the chat area of the chat window
            ClientLists.sendMessageToChat(incomingUser, message);
        } else {
            //If not, open a new chat window with that user
            openChat(incomingUser);
        }
    }

    /**
     *
     * @brief Method managing an incoming file
     *
     * @param input String from another user
     *
     */
    private void receivingFile(String input) {

        String fileName = null;

        //This messages come with this format: "%%&&incomingfile&&%%filename" and have to be split into tokens
        //to manage the information
        String[] token2 = input.split("&&%%");

        int field2 = 0;

        for (String nameBuffer : token2) {

            if (field2 == 1) { //field 0 means the token is: "%%&&incomingfile we don't need that
                fileName = nameBuffer;
            }
            field2 += 1;
        }

        try {
            //Read from the stream, store in an object
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            Object inputObject = in.readObject();

            //Decrypt this object
            byte[] byteString = RSAKeyHandling.rsaDecrypt((byte[]) inputObject, ownPrivateKey);
            assert byteString != null;

            assert fileName != null;
            //Write the content of the byte array in the disk as a file
            FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(byteString);

        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }


    /**
     *
     * @brief Button to open a new chat window with an user
     *
     */
    public void eventOpenChat() {

        //Select from the online list
        String chatUser = String.valueOf(txt_online.getSelectionModel().getSelectedItem());

        //Opens only if one user is selected
        if (txt_online.getSelectionModel().getSelectedItem() != null){
            //Open a new chat window
            openChat(chatUser);
        }
    }

    /**
     *
     * @brief Method that manage the opening of a new chat Window with another user
     *
     * @param chatUser Name of the user to chat with
     *
     */
    public void openChat(String chatUser) {

        //Search in the online list if this chat is already open
        boolean exist = ClientLists.searchChatUser(chatUser);

        //If it's not open
        if (!exist) {

            //This Platform.runLater is to avoid Not on FX application thread Exception
            Platform.runLater(() -> {

                //Create a new directory to store the log files with the chats if it doesn't exist
                File directory = new File("logs");
                directory.mkdir();

                //Create a file directory with the log file, and the log file if it doesn't exist
                File logFile = createLogFile(chatUser);

                //open a new chat window
                ChatWindow chatWindow = new ChatWindow(chatUser);
                //Add this new chat window to the list of opened windows
                ClientLists.chatlist.add(chatWindow);
                try {
                    //Read the log file and write its content in the chart ares
                    ClientLists.readlogFile(chatUser, logFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        //If it's open
        } else {
            log("Chat is already open");
        }
    }

    /**
     *
     * @brief Button disconnect. Disconnect the client from the server
     *
     */
    public void eventDisconnect() {
        if (client != null) {
            //Send a message to the server telling him that the client want to disconnect
            ReceiveAndSendMethods.sendMessageServer("$$Client&&Message##OptionClientDisconnect");
        }
    }

    /**
     *
     * @brief Menu item close. Disconnect the client from the server and close the program
     *
     */
    public void eventClose() {
        if (client != null) {
            //Send a message to the server telling him that the client want to disconnect
            ReceiveAndSendMethods.sendMessageServer("$$Client&&Message##OptionClientDisconnect");
        }
        //Close the program
        System.exit(0);
    }

    /**
     *
     * @brief Method managing the log text area.
     *
     * @param logMessage Message to be written in log area
     *
     */
    public void log(String logMessage) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        txt_log.appendText("(" + dtf.format(LocalDateTime.now()) + ") " + logMessage + "\n");
    }

    /**
     *
     * @brief Open a log file with the content of the chat with an user and create it if it doesnt exist
     *
     * @param chatUser Name of the user to chat with
     *
     * @return File descriptor of the log file
     *
     */
    public static File createLogFile(String chatUser) {

        String fileName = "logs/" + chatUser + ".log";

        //Create file descriptor to the log file withe the chat with that user
        File logFile = new File(fileName);

        try {
            if (logFile.createNewFile()) {
                System.out.println("New log File + (" + chatUser + ".log) created");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return logFile;
    }

    public static void main(String[] args) {

        //Generate Private and Public Cryptographic Key for encrypted communication
        KeyPair ownKeys = RSAKeyHandling.RSAKeyPairGenerator();
        ownPrivateKey = ownKeys.getPrivate();
        ownPublicKey = ownKeys.getPublic();

        launch(args);
    }

}
