package server;

import security.RSAKeyHandling;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 *
 * @brief This class is the thread that manage all the communication between server and client
 * with every client there is a new thread. All of this Threads are stored in a List
 *
 */
public class ServerComHandler extends Thread{

    private final Socket clientSocket;
    private final PrivateKey ownPrivateKey;
    private final PublicKey ownPublicKey;
    private String userName;
    private PublicKey clientPublicKey;

    //Constructor to set clientSocket, the ownPrivateKey and the ownPublicKey
    public ServerComHandler(Socket clientSocket, KeyPair ownKeys) {
        this.clientSocket = clientSocket;
        this.ownPrivateKey = ownKeys.getPrivate();
        this.ownPublicKey = ownKeys.getPublic();
    }

    //getter Methods for userName, clientSocket and clientPublicKey
    public Socket getClientSocket() {
        return clientSocket;
    }
    public String getUserName() {
        return userName;
    }
    public PublicKey getClientPublicKey() {
        return clientPublicKey;
    }

    @Override
    public void run() {

        //If userLogin returns true, means the client is successfully connected to the server. The server waits
        //for messages in "Listen Mode"
        if (userLogin()){
            listenMode();
        }

        //When the Client disconnects (or the password check was not successfully) the socket will be closed,
        //the thread ends and the server waits to further connections
        try {
            //close the socket
            clientSocket.close();

            //remove this thread from the users list
            ServerLists.usersLogList.remove(this);

            //send a message to every user that this user disconnected
            for (ServerComHandler user : ServerLists.usersLogList){
                sendServerMessageToClient(user.clientSocket,"clearListClient", user.userName);
            }

            System.out.println("Server: " + this.userName + " disconnects.");
            System.out.println("Server: waiting for connections."  + " (" + ServerLists.usersLogList.size() + " users online)");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @brief Method managing the Login process. Compare the user with his password to see if everything is correct
     * and act in consequence with the different options
     *
     * @return boolean true if the login process is successfully
     *
     */
    private boolean userLogin() {

        //First client and server exchange the public keys for encrypted communication
        clientPublicKey = RSAKeyHandling.receiveKeys(clientSocket);
        RSAKeyHandling.sendKeys(ownPublicKey, clientSocket);

        //Than the server receive from the client his login credentials (name and password)
        userName = readMessage();
        String password = readMessage();

        //Than search for the username in the Arraylist of Users and if there is one with
        //this name compare the passwords. Than manage the different possibilities.
        String passwordFromDatabase = ServerLists.searchUserPassword(userName);

        if (passwordFromDatabase == null) { //There is no User with that name. Create a new user, logIn with this user,
            // and start the listen mode
            logInCorrect(password, true);
            return true;
        } else if (passwordFromDatabase.startsWith(password)) { //There is a User with that name and the password is correct,
            // logIn with this user, and start the listen mode
            logInCorrect(password, false);
            return true;
        } else { //There is a User with that name and the password is not correct, send a message to the client and tell
            //him to close the socket and finish the communication.
            sendServerMessageToClient(this.clientSocket,"LogInIncorrect", this.userName);
            return false;
        }
    }

    /**
     *
     * @brief Method to manage if the LogIn data are correct. If the user doesn't exist, it creates a new record in the
     * user list. Than send the client a message to tell him the login process was successful. Add this communication to
     * the list and send it to all of the online users.
     *
     * @param password Users password
     *
     * @param newUser Boolean to know if the user already exists in the list
     *
     */
    private void logInCorrect(String password, boolean newUser){

        //If the username is not in the database, create a new user, add to the list and to the database
        if (newUser) {
            User.addNewRecord(this.userName, password);
            System.out.println("Server: New User " + this.userName + " created.");
        }

        //Send message to client to inform that the login process was successfully and add the username
        //to the online users list
        ServerLists.usersLogList.add(this);
        sendServerMessageToClient(this.clientSocket, "LogInCorrect", this.userName);


        //Iterate through all the online users to tell them who are now online
        for (ServerComHandler user : ServerLists.usersLogList){
            for (ServerComHandler users : ServerLists.usersLogList) {
                sendServerMessageToClient(user.clientSocket, "newlog," + users.userName, user.userName);
            }
        }

        System.out.println("Server: " + this.userName + " successfully logged.");
    }

    /**
     *
     * @brief This method manage the "Listen Mode" from this thread in the server
     *
     */
    private void listenMode() {

        String input;

        while (true) {

            //read the messages incoming from the client
            input = readMessage();

            //If the incoming message from the client start with $$&&##send it means that the message should be
            //forwarded to another client.
            if (input.startsWith("$$&&##send")){
                comingFromAClient(input);
            }

            //If the incoming message from the client is $$Client&&Message##OptionClientDisconnect it means that
            //the client want to close the communication with the server (disconnect).
            if (input.startsWith("$$Client&&Message##OptionClientDisconnect")){

                //First return a message to the client telling him that he should close the socket too
                sendServerMessageToClient(this.clientSocket, input, this.userName);

                //Remove the connection handler from the userlist
                ServerLists.usersLogList.remove(this);

                //Finally, close the socket with the client and break the while loop
                try {
                    this.clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }

            //If the connection with the client broke, readMessage returns "socket failure" and stops the listen mode
            if (input.equals("Socket failure")){
                System.out.println("Server: Connection with " + this.userName + " broke.");
                break;
            }
        }
    }

    /**
     *
     * @brief Read from the socket with the client and store it in a String
     *
     * @return String with the stored incoming message
     *
     */
    private String readMessage() {

        String input;

        try {

            //Read from the stream, store in an object
            ObjectInputStream in = new ObjectInputStream(this.clientSocket.getInputStream());
            Object inputObject = in.readObject();

            //Decrypt this object
            byte[] byteString = RSAKeyHandling.rsaDecrypt((byte[]) inputObject, ownPrivateKey);
            assert byteString != null;

            //Convert the bytes in a readable String and return it
            input = new String(byteString, StandardCharsets.UTF_8);
            return input;

        } catch (IOException | ClassNotFoundException e) {
            return "Socket failure";
        }
    }

    /**
     *
     * @brief This Method manage all the server specific messages that the server send to the client.
     *
     * @param outSocket Socket with the client
     *
     * @param input Input to manage the different messages
     *
     */
    private void sendServerMessageToClient(Socket outSocket, String input, String userToSend){

        String outputMessage = null;

        try{
            //There is 5 different specific messages from the server to the client:
            //OptionClientDisconnect: Finish the communication
            //LogInCorrect: Login credentials were correct.
            //LogInIncorrect: Login credentials were incorrect and finish the communication
            //clear + username: Remove userName from the online users list
            //newlog, + input: Add this user to the online users list

            if (input.startsWith("$$Client&&Message##OptionClientDisconnect")){
                outputMessage = "$$Server&&Message##OptionClientDisconnect";
            } else if (input.startsWith("LogInCorrect")){
                outputMessage = "$$Server&&Message##LogInCorrect";
            } else if (input.startsWith("LogInIncorrect")){
                outputMessage = "$$Server&&Message##LogInIncorrect";
            } else if (input.startsWith("clearListClient")){
                outputMessage = "$$Server&&Message##clear," + userName;
            } else if (input.startsWith("newlog,")){
                outputMessage = ("$$Server&&Message##" + input);
            }

            //Encrypt the message string and store as bytes
            assert outputMessage != null;
            byte[] encryptedMessage = RSAKeyHandling.rsaEncrypt(outputMessage, ServerLists.searchUserPublicKey(userToSend));

            //Finally send the bytes to the client
            ObjectOutputStream out = new ObjectOutputStream(outSocket.getOutputStream());
            out.writeObject(encryptedMessage);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @brief Method to manage the messages that came from another client and have to be forwarded
     *
     * @param input String with the incoming message
     *
     */
    private void comingFromAClient(String input) {

        String userToSend = null;
        String message = null;

        //This messages come with this format: "$$&&##send,chatUser;message" and have to be split into tokens
        //to manage the information
        String [] token = input.split(";");

        int field = 0;
        int field2 = 0;

        for(String messageBuffer : token) {

            if (field == 0){ //field 0 means the token is: $$&&##send,chatUser
                //Now we have to separate $$&&##send and chatUser
                String [] token2 = messageBuffer.split(",");
                for (String name : token2){
                    if (field2 == 1){ //We only need chatUser
                        userToSend = name;
                    }
                    field2 +=1;
                }
            } else if (field == 1){ //field 1 means the token is: message
                message = messageBuffer;
            }
            field += 1;
        }

        sendClientMessageToClient(userToSend, message);
    }


    /**
     *
     * @brief Forward a message coming from a client to another client
     *
     * @param userToSend final user that receive the message
     *
     * @param message message itself
     *
     */
    private void sendClientMessageToClient(String userToSend, String message) {
        //Find the socket to the destination client
        Socket userSocket = ServerLists.searchUserSocket(userToSend);
        //Create the final String to send
        String outputMessage = "$$Client&&Message##;#&" + userName + ";#&" + message;

        try{
            //Encrypt the message string and store as bytes
            byte[] encryptedMessage = RSAKeyHandling.rsaEncrypt(outputMessage, ServerLists.searchUserPublicKey(userToSend));
            //Finally send the bytes to the client
            assert userSocket != null;
            ObjectOutputStream out = new ObjectOutputStream(userSocket.getOutputStream());
            out.writeObject(encryptedMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (message.startsWith ("%%&&incomingfile&&%%")){
            sendFileToClient(userToSend, userSocket);
        }
    }

    /**
     *
     * @brief Forward a file coming from a client to another client
     *
     * @param userToSend final user that receive the message
     *
     * @param userSocket socket with the final user that receive the file
     *
     */
    private void sendFileToClient(String userToSend, Socket userSocket) {

        try {
            //Read from the stream, store in an object
            ObjectInputStream in = new ObjectInputStream(this.clientSocket.getInputStream());
            Object inputObject = in.readObject();

            //Decrypt this object
            byte[] byteString = RSAKeyHandling.rsaDecrypt((byte[]) inputObject, ownPrivateKey);
            //Encrypt this object with the next client public key
            byte[] encryptedFile = RSAKeyHandling.rsaEncryptFile(byteString, ServerLists.searchUserPublicKey(userToSend));

            //Send object to client
            ObjectOutputStream out = new ObjectOutputStream(userSocket.getOutputStream());
            out.writeObject(encryptedFile);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
