import static org.junit.Assert.*;
import org.junit.Test;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ServerTest {

    @Test
    public void testClientConnection() throws IOException {
        // Start the server in a separate thread
        Thread serverThread = new Thread(() -> {
            Server.main(new String[]{});
        });
        serverThread.start();

        // Simulate a client connection
        Socket clientSocket = new Socket("localhost", 5555);

        // Capture output from the server
        ByteArrayOutputStream serverOutput = new ByteArrayOutputStream();
        System.setOut(new PrintStream(serverOutput));

        // Simulate a message sent by the client
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println("Test Message from Client");

        // Wait for the server to process the message
        try {
            Thread.sleep(1000); // Wait for 1 second to ensure the message is processed
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify that the server output contains the received message
        assertTrue(serverOutput.toString().contains("Test Message from Client"));

        // Clean up
        clientSocket.close();
        serverThread.interrupt();
    }
}
