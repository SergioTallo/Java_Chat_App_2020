package server;

import security.RSAKeyHandling;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.util.concurrent.TimeUnit;

public class Server {

    public static void main(String[] args) {

        int portNumber = 0;

        //Method to parse the command line arguments
        try{
            portNumber = getPortNumber(args[0]);
        } catch (Exception e) {
            System.out.println("Invalid program argument. Please write the port number you want to use");
        }

        //Generate Private and Public Cryptographic Key for encrypted communication
        KeyPair ownKeys = RSAKeyHandling.RSAKeyPairGenerator();

        //Method to create the csv File (userdatabank.csv) if it doesn't exist
        File dataBase = User.createCsvFile();

        //read the csv File, and create a user list with all usernames and passwords in the database
        ServerLists.createUserList(dataBase);

        //Method to create the server socket for incoming communications
        ServerSocket server_socket = ServerConnectionMethods.openServer(portNumber);

        //Open new socket for every client connection and a new thread that handles that connection. It waits for further Connections
        while (true) {

            System.out.println("Server: waiting for connections. Port: " + portNumber + " (" + ServerLists.usersLogList.size() + " users online)");
            assert server_socket != null;

            //Open a new socket to connect with a client
            Socket clientPipe = ServerConnectionMethods.openClientPipe(server_socket);

            //Open a new thread to handle a new communication with a client and start this thread
            ServerComHandler communicationHandler = new ServerComHandler(clientPipe, ownKeys);
            communicationHandler.start();

            try{
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *
     * @brief Method to parse the command line argument
     *
     * @param args Receive the first and only argument
     *
     * @return Port number Returns the port number
     *
     */
    public static int getPortNumber(String args) {

        try {
            //Receive the port number from the command line argument and store in a variable
            //port number should be an integer between 1025 and 65499
            int port_number = Integer.parseInt(args);
            if (port_number < 1024 | port_number > 65500) {
                throw new IllegalArgumentException();
            }
            return port_number;
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid program argument. Please write the port number you want to use.");
            System.out.println("Port number should be between 1025 and 65500");
            return 0;
        }
    }
}
