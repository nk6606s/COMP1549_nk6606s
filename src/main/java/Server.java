import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Server {
    private static final int PORT = 5555;
    private static final long CHECK_INTERVAL = 20000; // 20 seconds
    private static final Map<String, Socket> clients = new HashMap<>();
    
    public static Map<String, Socket> getClients() {
        return clients;
    }

    public static void main(String[] args) {
        System.out.println("Server started. Listening on port " + PORT + "...");
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new CheckActiveClientsTask(), 0, CHECK_INTERVAL);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> ClientHandler.handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class CheckActiveClientsTask extends TimerTask {
        @Override
        public void run() {
        	ClientHandler.pushUserListToCoordinator();
        }
    }

}
