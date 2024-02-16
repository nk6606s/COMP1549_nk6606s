import static org.junit.Assert.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ClientTest {

    private ServerSocket serverSocket;
    private Thread serverThread;
    private ByteArrayOutputStream serverOutput;

    @Before
    public void setUp() throws IOException {
        // Start a server in a separate thread
        serverOutput = new ByteArrayOutputStream();
        serverSocket = new ServerSocket(6666);
        serverThread = new Thread(() -> {
            try {
                Socket clientSocket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    // Echo back received messages to simulate server response
                    out.println(inputLine);
                    serverOutput.write(inputLine.getBytes());
                    serverOutput.write('\n');
                    serverOutput.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
    }

    @After
    public void tearDown() throws IOException, InterruptedException {
        // Stop the server and wait for the server thread to join
        serverSocket.close();
        serverThread.join();
    }

    @Test
    public void testClientConnection() {
        // Create a Client object
        Client client1 = new Client("user1", "localhost", 6666);

        // Redirect System.in to provide input to the client
        ByteArrayInputStream input = new ByteArrayInputStream("Test Message".getBytes());
        System.setIn(input);

        // Capture System.out to check client's output
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));

        // Connect to the server
        client1.connectToServer();

        // Validate that the client sent the correct ID to the server
        assertTrue(serverOutput.toString().contains("user1"));

        // Validate that the client received the expected response from the server
        assertTrue(output.toString().contains("Test Message"));

        // Reset System.in and System.out
        System.setIn(System.in);
        System.setOut(System.out);
    }





}
