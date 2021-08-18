package client;

import security.RSAKeyHandling;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PublicKey;

public class ReceiveAndSendMethods {

    /**
     *
     * @brief Method to manage the sending messages to another client
     *
     * @param destination Client that should recieve the message
     *
     * @param message Message itself
     *
     */
    public static void sendMessageOtherClient(String destination, String message) {

        //Open socket with the server
        Socket client = Client.client;
        //Get the public key of the server
        PublicKey serverPublicKey = Client.serverPublicKey;
        //Create final String with the message
        String outgoingMessage = ("$$&&##send," + destination + ";" + message);
        //Encrypt the message
        byte[] bytes = RSAKeyHandling.rsaEncrypt(outgoingMessage, serverPublicKey);

        try {
            //Open stream and send the message
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            out.writeObject(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @brief Method to manage the sending messages to the server
     *
     * @param message Message to send to the server
     *
     */
    public static void sendMessageServer(String message) {

        //Open socket with the server
        Socket client = Client.client;
        //Get the public key of the server
        PublicKey serverPublicKey = Client.serverPublicKey;

        //Encrypt the message
        byte[] bytes = RSAKeyHandling.rsaEncrypt(message, serverPublicKey);

        try {
            //Open stream and send the message
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            out.writeObject(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @brief Method to send a File to another client
     *
     * @param fileToSend File descriptor of the File to send
     *
     */
    public static void sendFile(File fileToSend) {

        //Open socket with the server
        Socket client = Client.client;

        //Get the public key of the server
        PublicKey serverPublicKey = Client.serverPublicKey;

        //Open a new bytes array
        byte[] bytes = new byte[(int) fileToSend.length()];
        try{
            //Open a filestream to read file and store in bytes array
            FileInputStream fis = new FileInputStream(fileToSend);
            fis.read(bytes);
        }catch(IOException ioExp){
            ioExp.printStackTrace();
        }

        //Encrypt the file
        byte[] encryptedBytes = RSAKeyHandling.rsaEncryptFile(bytes, serverPublicKey);

        try {
            //Open stream and send the file
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            out.writeObject(encryptedBytes);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
