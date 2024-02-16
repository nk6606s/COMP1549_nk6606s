import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
	
    //comment
	private String id;
	private String serverIP;
	private int serverPort;
	private Socket socket;
	private PrintWriter out;
	//private BufferedReader in;
	
	
	public Client(String id, String serverIP, int serverPort) {
        this.id = id;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }
	
	
	public void connectToServer() {
        try {
            socket = new Socket(serverIP, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            // Send client ID to the server
            out.println(id);

            // Start a thread to handle incoming messages from the server
            Thread messageListener = new Thread(() -> {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            messageListener.start();

            // Continuously read user input and send it to the server
            String userInputLine;
            while ((userInputLine = userInput.readLine()) != null) {
                out.println(userInputLine);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	
	
	public static void main(String[] args) {
        // Check if the correct number of command-line arguments are provided
        if (args.length != 3) {
            System.out.println("Usage: java Client <ID> <serverIP> <serverPort>");
            return;
        }

        // Retrieve parameters from command-line arguments
        String id = args[0];
        String serverIP = args[1];
        int serverPort = Integer.parseInt(args[2]);

        // Create a client object and connect to the server
        Client client = new Client(id, serverIP, serverPort);
        client.connectToServer();
    }

}
