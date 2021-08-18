package client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @brief This class is the window with the individual chats between clients
 *
 */
public class ChatWindow extends Application {

    private final String userName;

    public ChatWindow(String userName) {
        this.userName = userName;
        Stage stage = new Stage();
        try{
            start(stage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getUserName() {
        return userName;
    }

    TextArea txt_chatArea = new TextArea();
    TextField txt_message = new TextField();

    public void setTxt_chatArea(String message) {
        txt_chatArea.appendText(message);
    }

    @Override
    public void start(Stage stage) {
        VBox topvbox = new VBox();
        topvbox.setPadding(new Insets(10));
        topvbox.setSpacing(10);

        Text chatwith = new Text ("Chat with: " + userName);

        txt_chatArea.setPrefHeight(400);

        Button btn_send = new Button("Send Message");
        btn_send.setPrefWidth(200);
        btn_send.setOnAction(e -> sendMessage());

        Button btn_file = new Button("Send File");
        btn_file.setPrefWidth(200);
        btn_file.setOnAction(e -> {
            try {
                sendFile();
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        });

        //To close the program with the x on the window
        stage.setOnCloseRequest(windowEvent -> {
            ClientLists.chatlist.remove(this);
            stage.close();
        });


        topvbox.getChildren().addAll(chatwith, txt_chatArea, txt_message, btn_send, btn_file);
        Scene scene = new Scene(topvbox, 600, 600);

        stage.setTitle(userName);
        stage.setScene(scene);
        stage.show();
    }

    /**
     *
     * @brief Method to write a string in the log file
     *
     * @param dtf Data format time
     */
    private void writeLogFile (DateTimeFormatter dtf){
        String fileName = "logs/" + Client.userName + ".log";
        FileWriter myWriter;
        try {
            myWriter = new FileWriter(fileName, true);
            myWriter.write ("(" + dtf.format(LocalDateTime.now()) + ") " +"(" + Client.userName + ") : " + txt_message.getText() + "\n");
            myWriter.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     *
     * @brief Method to manage send a message to another client
     *
     */
    private void sendMessage(){
        if (!txt_message.getText().isEmpty()){ //Avoid sending empty messages

            ReceiveAndSendMethods.sendMessageOtherClient(userName, txt_message.getText());

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
            setTxt_chatArea("(" + dtf.format(LocalDateTime.now()) + ") " +"(" + Client.userName + ") : " +txt_message.getText());
            setTxt_chatArea("\n");
            writeLogFile(dtf);

            txt_message.clear();
        }
    }


    /**
     *
     * @brief Method to manage send a file to another client
     *
     * @throws InterruptedException
     *
     */
    private void sendFile () throws InterruptedException {

        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(new Stage());

        if (selectedFile != null && selectedFile.length() < 245){
            String filename = selectedFile.getName();
            ReceiveAndSendMethods.sendMessageOtherClient(userName, "%%&&incomingfile&&%%" + filename);
            ReceiveAndSendMethods.sendFile(selectedFile);
            ReceiveAndSendMethods.sendMessageOtherClient(userName, "file " + filename + " received");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
            setTxt_chatArea("(" + dtf.format(LocalDateTime.now()) + ") " +"(" + Client.userName + ") : " + filename + " send");
            setTxt_chatArea("\n");
        } else {
            System.out.println("file too long");
        }
    }
}
