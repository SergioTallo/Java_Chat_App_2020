package server;

import java.io.*;
import java.net.Socket;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class ServerLists {

    //List with all the users and passwords from the database
    public static List<User> users = new ArrayList<>();

    //List with all the active connections with a client
    public static ArrayList<ServerComHandler> usersLogList = new ArrayList<>();

    /**
     *
     * @brief Create the List of users from the database. Iterates through all the lines in the csv file,
     * tokenize the data, create the user records and add them to the list
     *
     * @param database File descriptor of the database
     *
     */
    public static void createUserList(File database){

        BufferedReader reader;

        try {
            //Open a new buffered reader to read the database file line by line
            reader = new BufferedReader(new FileReader(database));
            String line = reader.readLine();
            while (line != null) {

                String userName = null;
                String password = null;

                //When a line is read, it have the format user,password must have to spilt into user and password
                String[] fields = line.split(",");
                int field = 0;

                for(String token : fields) {
                    if (field == 0){
                        userName = token;
                        field += 1;
                    } else {
                        password = token;
                    }
                }

                //With the username and the password create a new entry in the users list
                User record = new User (userName, password);
                users.add(record);

                //repeat till there is no more lines
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     *
     * @brief Method to search a username in the database and to returns his password
     *
     * @param userName Name of the user we want to know the password
     *
     * @return Password Returns the password of the user from the database. Null if there is no user with this name
     *
     */
    public static String searchUserPassword(String userName){

        //Iterate through the users list to find the password of a user
        for(User user : ServerLists.users) {
            String name = user.getUsername();
            if (userName.equals(name)){
                return user.getPassword();
            }
        }
        return null;
    }

    /**
     *
     * @brief Method to search an online user and to returns his socket
     *
     * @param userName Name of the user we want to know the socket
     *
     * @return Socket Returns the socket of the online user want to send a message. Null if this user is not online
     *
     */
    public static Socket searchUserSocket(String userName){

        //Iterate through the online users to find his socket
        for(ServerComHandler user : ServerLists.usersLogList) {
            String name = user.getUserName();
            if (userName.equals(name)){
                return user.getClientSocket();
            }
        }
        return null;
    }

    /**
     *
     * @brief Method to search an online user and to returns his publicKey
     *
     * @param userName Name of the user we want to know the socket
     *
     * @return PublicKey Returns the PublicKey of the online user want to send a message. Null if this user is not online
     *
     */
    public static PublicKey searchUserPublicKey (String userName){

        //Iterate through the online users to find his public key
        for(ServerComHandler user : ServerLists.usersLogList) {
            String name = user.getUserName();
            if (userName.equals(name)){
                return user.getClientPublicKey();
            }
        }
        return null;
    }
}
