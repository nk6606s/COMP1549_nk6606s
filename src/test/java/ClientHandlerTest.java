import static org.junit.Assert.*;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;

public class ClientHandlerTest {

    @Test
    public void testListenForCommands() throws IOException {
        // Prepare input for BufferedReader
        String input = "/request\n/approve user2\n/private user3 Hello\nNormal message";

        // Redirect System.out to capture the output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        // Create a BufferedReader with the prepared input
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        // Create a StringWriter to capture output from PrintWriter
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(byteArrayOutputStream);

        // Call listenForCommands method
        ClientHandler.listenForCommands("user1", bufferedReader, printWriter);

        // Validate the output
        String expectedOutput = "Message from client user1: Normal message\n";
        assertEquals(expectedOutput, outputStream.toString());
    }
}
