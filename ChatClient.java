import java.io.*;
import java.net.*;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in))) {

            String serverMsg = serverIn.readLine();
            if (serverMsg == null) {
                System.out.println("Server closed connection.");
                return;
            }

            if (serverMsg.equals("ENTER_USERNAME")) {
                System.out.print("Enter a username: ");
                String username = userIn.readLine();
                serverOut.println(username);
            } else {
                System.out.println("Server: " + serverMsg);
            }

            Thread readerThread = new Thread(() -> {
                String msg;
                try {
                    while ((msg = serverIn.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            String input;
            while ((input = userIn.readLine()) != null) {
                serverOut.println(input);
                if (input.equalsIgnoreCase("/quit") || input.equalsIgnoreCase("/exit")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("Unable to connect to server: " + e.getMessage());
        }
    }
}
