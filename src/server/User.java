package server;

import java.io.*;

/**
 *
 * @brief: This class have the user data from the database. All instances in this class will be
 * stored in a list
 *
 */
public class User {

    private final String username;
    private final String password;

    //Constructor to set userName and Password
    public User(String userName, String password) {
        this.password = password;
        this.username = userName;
    }

    //getter Methods for userName and password
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }

    /**
     *
     * @brief Method to create a User instance and afterwards store in the list
     *
     * @param userName Name of the user
     *
     * @param password Password of the user (Hashcode of the real password)
     *
     */
    public static void addNewRecord(String userName, String password){

        String fileName = "userdatabank.csv";

        //Create a new User
        User record = new User (userName, password);

        //Add to the users list
        ServerLists.users.add(record);

        //Create a new text line with the format "user,password"
        String newLine = (record.getUsername() + "," + record.getPassword() + "\n");

        try{
            //Append the new text line at the end of the database file
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
            out.write(newLine);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @brief Method to create the File descriptor of the csv File with the database, and the file if it doesnt exist
     *
     * @return userDatabase Returns the File descriptor of the database
     *
     */
    public static File createCsvFile (){
        String fileName = "userdatabank.csv";

        File userDatabase = new File(fileName);

        try {
            if (userDatabase.createNewFile()){
                System.out.println("New csv File (userdatabank.csv) created ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return userDatabase;
    }

}
