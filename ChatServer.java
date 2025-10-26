import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 12345;
    
    private static final ConcurrentHashMap<String, PrintWriter> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Chat server starting on port " + PORT + "...");
        ServerSocket serverSocket = new ServerSocket(PORT);

        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
               
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } finally {
            serverSocket.close();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private String username;
        private BufferedReader in;
        private PrintWriter out;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                
                out.println("ENTER_USERNAME");
                String requestedName = in.readLine();
                if (requestedName == null) return;

                requestedName = requestedName.trim();
                if (requestedName.isEmpty()) {
                    out.println("ERROR Username cannot be empty. Connection closing.");
                    closeConnection();
                    return;
                }


                if (clients.putIfAbsent(requestedName, out) != null) {
                    out.println("ERROR Username already taken. Connection closing.");
                    closeConnection();
                    return;
                }

                username = requestedName;
                out.println("WELCOME " + username);
                broadcast("SERVER: " + username + " has joined the chat.");

                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.equalsIgnoreCase("/quit") || line.equalsIgnoreCase("/exit")) {
                        break;
                    }
                    if (line.isEmpty()) continue;

                    if (line.startsWith("@")) {
                        int spaceIdx = line.indexOf(' ');
                        if (spaceIdx > 1) {
                            String target = line.substring(1, spaceIdx);
                            String msg = line.substring(spaceIdx + 1);
                            sendPrivate(target, username + " (private): " + msg);
                        } else {
                            out.println("SERVER: Invalid private message format. Use: @username message");
                        }
                    } else {
                        broadcast(username + ": " + line);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client " + username + ": " + e.getMessage());
            } finally {
                // Clean up
                if (username != null) {
                    clients.remove(username);
                    broadcast("SERVER: " + username + " has left the chat.");
                }
                closeConnection();
            }
        }

        private void sendPrivate(String targetUsername, String message) {
            PrintWriter pw = clients.get(targetUsername);
            if (pw != null) {
                pw.println(message);
                // also send confirmation to sender
                out.println(message);
            } else {
                out.println("SERVER: User '" + targetUsername + "' not found.");
            }
        }

        private void broadcast(String message) {
            // iterate over snapshot of current clients
            for (PrintWriter writer : clients.values()) {
                writer.println(message);
            }
        }

        private void closeConnection() {
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            if (out != null) out.close();
            try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        }
    }
}
