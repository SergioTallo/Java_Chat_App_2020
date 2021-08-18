package client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ClientLists {

    //List with all the online users
    public static ObservableList<String> onlineUsers = FXCollections.observableArrayList();

    //List with all the active chats
    public static ArrayList<ChatWindow> chatlist = new ArrayList<>();

    /**
     *
     * @brief Search if the user name is online
     *
     * @param userName User name to search
     *
     * @return true if the user is online
     */
    public static boolean searchonlineUsers (String userName){

        //Iterate in the online users list to check the name of the user
        for (String name : onlineUsers){
            if (name.equals(userName)){
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @brief Searchist there is a chat window open with that user
     *
     * @param user Username to search
     *
     * @return true if there is a chat window open
     *
     */
    public static boolean searchChatUser(String user) {

        //Iterate in the open chats list if there is an open chat with that user
        for (ChatWindow chatuser : ClientLists.chatlist) {
            String name = chatuser.getUserName();
            if (name.equals(user)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @brief Read the log file with the user to chat and write its content in the chat area
     *
     * @param user Username with we want to chat
     *
     * @param File File descriptor of the log file
     *
     * @throws IOException
     */
    public static void readlogFile (String user, File File) throws IOException {

        for (ChatWindow chatuser : ClientLists.chatlist) {
            String name = chatuser.getUserName();
            if (name.equals(user)) {
                FileReader fr = new FileReader(File);   //reads the file
                BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
                String line;
                while((line = br.readLine())!=null)
                {
                    chatuser.setTxt_chatArea(line);      //appends line to string buffer
                    chatuser.setTxt_chatArea("\n");     //line feed
                }
                fr.close();
            }
        }
    }


    /**
     *
     * @brief Write message into char area
     *
     * @param user User from the nmessage came
     *
     * @param message Message itself
     */
    public static void sendMessageToChat(String user, String message) {

        for (ChatWindow chatuser : ClientLists.chatlist) {
            String name = chatuser.getUserName();
            if (name.equals(user)) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
                chatuser.setTxt_chatArea("(" + dtf.format(LocalDateTime.now()) + ") " +"(" + user + ") : " + message);
                chatuser.setTxt_chatArea("\n");

                //Write the outgoing message in the logfile too
                String fileName = "logs/" + Client.userName + ".log";
                FileWriter myWriter;
                try {
                    myWriter = new FileWriter(fileName, true);
                    myWriter.write("(" + dtf.format(LocalDateTime.now()) + ") " +"(" + user + ") : " + message + "\n");
                    myWriter.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }
}
