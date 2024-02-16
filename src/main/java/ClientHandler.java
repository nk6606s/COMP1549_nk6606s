import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class ClientHandler {
	private static final Map<String, Socket> clients = Server.getClients();
	private static String coordinatorId;
    
//	private static final Map<String, Consumer<String[]>> commandMap = new HashMap<>();

//    static {
//        commandMap.put("/request", args -> handleUserListRequest(args));
//        commandMap.put("/approve", args -> handleApproveRequest(args[0], args[1]));
//        // Add more commands as needed
//    }
    

    
    public static Map<String, Socket> getClients() {
        return clients;
    }

    //Main method called from Server class
    public static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            out.println("Enter User ID:");
            String clientId = readClientId(in, out);
            clients.put(clientId, clientSocket);
            System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress() + ", ID: " + clientId);
            handleCoordinatorAssignment(out, clientId);
            listenForCommands(clientId, in, out);

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            removeClient(clientSocket);
        }
    }
    
    public static void pushUserListToCoordinator() {
    	
    	Socket coordinatorSocket = clients.get(coordinatorId);
        if (coordinatorSocket != null) {
            try {
                PrintWriter coordinatorOut = new PrintWriter(coordinatorSocket.getOutputStream(), true);
                coordinatorOut.println("Active users: " + getUsersDetails());
            } catch (IOException e) {
                System.err.println("Error notifying coordinator: " + e.getMessage());
            }
        }
    }


    //Function is validating if user_id is unique, informing user who is coordinator
    private static String readClientId(BufferedReader in, PrintWriter out) throws IOException {
        String clientId;
        boolean idTaken;
        do {
            clientId = in.readLine();
            synchronized (clients) {
                idTaken = clients.containsKey(clientId);
            }
            if (idTaken) {
                out.println("User ID already taken, enter another User ID:");
            }
        } while (idTaken);
        if (coordinatorId != null) {
            out.println("Coordinator is: " + coordinatorId);
        } else {
            out.println("No coordinator assigned yet.");
        }
        out.println("User ID accepted, you can type messages!");
        return clientId;
    }

    private static void handleCoordinatorAssignment(PrintWriter out, String clientId) {
        if (coordinatorId == null) {
            coordinatorId = clientId;
            out.println("You are the coordinator.");
            System.out.println("Client " + clientId + " is the coordinator.");
        }
    }

    public static void listenForCommands(String clientId, BufferedReader in, PrintWriter out) throws IOException {
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            if (inputLine.startsWith("/request")) {
                handleUserListRequest(clientId, out);
            } else if (inputLine.startsWith("/approve")) {
                handleApproveRequest(clientId, inputLine);
            } else if (inputLine.startsWith("/")) {
                handlePrivateMessage(clientId, inputLine);
            } else {
                System.out.println("Message from client " + clientId + ": " + inputLine);
                broadcastMessage(clientId, inputLine);
            }
        }
    }

    
//    private static void handleUserListRequest(String[] args) {
//        String clientId = args[0];
//        Socket clientSocket = clients.get(clientId); // Assuming clients is a map of client IDs to sockets
//        try {
//            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
//            if (!clientId.equals(coordinatorId)) {
//                out.println("Request sent to coordinator for user details.");
//                notifyCoordinator(clientId);
//            } else {
//                out.println("User details: " + getUsersDetails());
//            }
//        } catch (IOException e) {
//            // Handle IOException
//            e.printStackTrace();
//        }
//    }


    private static void handleUserListRequest(String clientId, PrintWriter out) {
        if (!clientId.equals(coordinatorId)) {
            out.println("Request sent to coordinator for user details.");
            notifyCoordinator(clientId);
        } else {
            out.println("User details: " + getUsersDetails());
        }
    }

    private static void handleApproveRequest(String clientId, String inputLine) {
        if (clientId.equals(coordinatorId)) {
            String[] parts = inputLine.split(" ", 2);
            if (parts.length == 2 && parts[0].startsWith("/")) {
                String recipientId = parts[1].trim();
                String message = getUsersDetails();
                sendPrivateMessage(clientId, recipientId, message);
            }
        }
    }

    private static void handlePrivateMessage(String clientId, String inputLine) {
        String[] parts = inputLine.split(" ", 2);
        if (parts.length == 2 && parts[0].startsWith("/")) {
            String recipientId = parts[0].substring(1);
            String message = parts[1].trim();
            sendPrivateMessage(clientId, recipientId, message);
        }
    }

    public static void sendPrivateMessage(String senderId, String recipientId, String message) {
        Socket recipientSocket = clients.get(recipientId);
        if (recipientSocket != null) {
            try {
                PrintWriter recipientOut = new PrintWriter(recipientSocket.getOutputStream(), true);
                String formattedMessage = "[Private from " + senderId + "]: " + message;
                recipientOut.println(formattedMessage);
            } catch (IOException e) {
                System.err.println("Error sending private message to client " + recipientId + ": " + e.getMessage());
            }
        } else {
            System.err.println("Recipient " + recipientId + " not found or disconnected.");
        }
    }

    private static void notifyCoordinator(String requesterId) {
        Socket coordinatorSocket = clients.get(coordinatorId);
        if (coordinatorSocket != null) {
            try {
                PrintWriter coordinatorOut = new PrintWriter(coordinatorSocket.getOutputStream(), true);
                coordinatorOut.println("User " + requesterId + " requested user details. Use /approve + user_id to approve.");
            } catch (IOException e) {
                System.err.println("Error notifying coordinator: " + e.getMessage());
            }
        }
    }

    private static void removeClient(Socket clientSocket) {
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        }
        String disconnectedClientId = clients.entrySet().stream()
                .filter(entry -> entry.getValue().equals(clientSocket))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        if (disconnectedClientId != null) {
            clients.remove(disconnectedClientId);
            System.out.println("Client " + disconnectedClientId + " left the group.");
            handleCoordinatorDisconnection(disconnectedClientId);
        }
    }

    private static void handleCoordinatorDisconnection(String disconnectedClientId) {
        if (disconnectedClientId.equals(coordinatorId)) {
            System.out.println("Coordinator disconnected. Reassigning coordinator...");
            if (!clients.isEmpty()) {
                coordinatorId = clients.keySet().iterator().next();
                System.out.println("New coordinator is client " + coordinatorId);
                try {
                    PrintWriter coordinatorOut = new PrintWriter(clients.get(coordinatorId).getOutputStream(), true);
                    coordinatorOut.println("You are the coordinator now.");
                } catch (IOException e) {
                    System.err.println("Error assigning new coordinator: " + e.getMessage());
                }
            } else {
                System.out.println("No more clients in the group.");
                coordinatorId = null;
            }
        }
    }

    public static String getUsersDetails() {
        StringBuilder userDetails = new StringBuilder();
        for (Map.Entry<String, Socket> entry : clients.entrySet()) {
            String userId = entry.getKey();
            Socket clientSocket = entry.getValue();
            String address = clientSocket.getInetAddress().getHostAddress();
            int port = clientSocket.getPort(); // Get the port number
            userDetails.append(userId).append(": ").append(address).append(":").append(port);
            if (userId.equals(coordinatorId)) {
                userDetails.append(" (Coordinator)");
            }
            userDetails.append(", ");
        }
        if (userDetails.length() > 2) {
            userDetails.setLength(userDetails.length() - 2); // Remove trailing comma and space
        }
        return userDetails.toString();
    }

    public static void broadcastMessage(String senderId, String message) {
        String formattedMessage = "[" + senderId + "]: " + message;
        // Regular broadcast message
        for (Map.Entry<String, Socket> entry : clients.entrySet()) {
            String clientId = entry.getKey();
            Socket clientSocket = entry.getValue();
            if (clientId.equals(senderId) || clientSocket.isClosed()) {
                continue; // Skip sending message to the sender or closed sockets
            }
            try {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println(formattedMessage);
            } catch (IOException e) {
                System.err.println("Error sending message to client " + clientId + ": " + e.getMessage());
            }
        }
    }
}
