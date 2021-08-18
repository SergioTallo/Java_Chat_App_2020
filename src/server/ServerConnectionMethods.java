package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerConnectionMethods {

    /**
     *
     * @brief Method to create the server socket for the incoming connections
     *
     * @param port_number Port number to accept connections
     *
     * @return ServerSocket Returns the server socket for incoming connections
     *
     */
    public static ServerSocket openServer(int port_number) {
        try {
            return new ServerSocket(port_number);
        } catch (IOException e) {
            e.getStackTrace();
            return null;
        }
    }


    /**
     *
     * @brief Method to create the socket with the client
     *
     * @param server Serversocket to accept connections
     *
     * @return Socket Returns the socket with the client
     *
     */
    public static Socket openClientPipe(ServerSocket server) {

        try {
            Socket client = server.accept();
            System.out.println("Server: connected to Client " + client.getInetAddress());
            return client;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
