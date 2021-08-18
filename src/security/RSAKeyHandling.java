package security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class RSAKeyHandling {

    /**
     *
     * @brief Method to generate a new RSA Keypair (private and public)
     *
     * @return Keypair with a public and a private key
     *
     */
    public static KeyPair RSAKeyPairGenerator() {
        //Create a Generator
        KeyPairGenerator keyGen = null;
        try {
            //Initialize the generator with RSA protocol
            keyGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        assert keyGen != null;
        //Create the keypar RSA 2048
        keyGen.initialize(2048, new SecureRandom());
        return keyGen.generateKeyPair();
    }

    /**
     *
     * @brief Method to send the own Public key to a communication partner
     *
     * @param ownPublicKey Public key of this user
     *
     * @param socket       Socket to communicate with the partner
     *
     */
    public static void sendKeys(PublicKey ownPublicKey, Socket socket) {
        try {
            //Open out stream with the socket to the server
            ObjectOutputStream outputKeyStream = new ObjectOutputStream(socket.getOutputStream());
            //Send the keypar to the server
            outputKeyStream.writeObject(ownPublicKey);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @brief Receive a public key from the communication partner
     *
     * @param socket Socket to communicate with the partner
     *
     * @return PublicKey of the communication partner
     *
     */
    public static PublicKey receiveKeys(Socket socket) {
        try {
            //Open in stream with the socket to the server
            ObjectInputStream inputKeyStream = new ObjectInputStream(socket.getInputStream());
            //Get the public key of the server
            return (PublicKey) inputKeyStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @brief Encrypt String with RSA public key
     *
     * @param outputMessage String to encrypt
     *
     * @param publicKey     Public Key of the destination of the message
     *
     * @return Encrypted message in bytes[]
     *
     */
    public static byte[] rsaEncrypt(String outputMessage, PublicKey publicKey) {
        try {
            //Convert the message to send into bytes array
            byte[] data = outputMessage.getBytes(StandardCharsets.UTF_8);
            //Create the cypher RSA to encrypt
            Cipher cipher = Cipher.getInstance("RSA");
            //Initialize the Cypher with the public Key of the partner
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            //Encrypt the message
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @brief Decrypt String with own private key
     *
     * @param data       Incoming encrypted message
     *
     * @param privateKey Own Private key
     *
     * @return decrypted message as byte[]
     *
     */
    public static byte[] rsaDecrypt(byte[] data, PrivateKey privateKey) {
        try {
            //Create the cypher RSA to decrypt
            Cipher cipher = Cipher.getInstance("RSA");
            //Initialize the Cypher with the public Key of the partner
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            //Decrypt the message
            return cipher.doFinal(data);
        } catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @brief Encrypt String with RSA public key
     *
     * @param outputData Data to encrypt
     *
     * @param publicKey  Public Key of the destination of the message
     *
     * @return Encrypted message in bytes[]
     *
     */
    public static byte[] rsaEncryptFile(byte[] outputData, PublicKey publicKey) {
        try {
            //Create the cypher RSA to encrypt
            Cipher cipher = Cipher.getInstance("RSA");
            //Initialize the Cypher with the public Key of the partner
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            //Encrypt the file
            return cipher.doFinal(outputData);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }
}