package client;

import java.io.IOException;
import java.net.Socket;

public class ClientConnectionsMethods {

    /**
     *
     * @brief Method managing the connection to the server
     *
     * @param host Server address
     *
     * @param port Server port
     *
     * @return Socket with the server
     *
     */
    public static Socket connectToServer(String host, int port){
        try {
            //Connect to the waiting socket in the server
            return new Socket (host, port);
        } catch (IOException e) {
            return null;
        }
    }
}
