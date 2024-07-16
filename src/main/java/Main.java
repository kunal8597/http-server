import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.HashMap;
import java.util.Map;

public class Main {
  public static String directory; 

    public static void main(String[] args) {
        // Parse command line arguments
        
        if (args.length > 1 && args[0].equals("--directory")) {
            directory = args[1];
        }
        System.out.println("Logs from your program will appear here!");
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);
            while (true) {
              // Wait for connection from client.
                Socket clientSocket = serverSocket.accept(); 
                System.out.println("accepted new connection");
                // Handle each client connection in a separate thread.
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
// Management of client connections to the HTTP server
    private static void handleClient(Socket clientSocket) {
        try {
            BufferedReader inputStream = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            // Read the request line
            String requestLine = inputStream.readLine();
            if (requestLine == null) {
                return; // No request line received, close the connection.
            }
            String httpMethod = requestLine.split(" ")[0];
            // Read all the headers from the HTTP request.
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            while (!(headerLine = inputStream.readLine()).isEmpty()) {
                String[] headerParts = headerLine.split(": ");
                headers.put(headerParts[0], headerParts[1]);
            }
            // Extract the URL path from the request line.
            String urlPath = requestLine.split(" ")[1];
            OutputStream outputStream = clientSocket.getOutputStream();
            // Write the HTTP response to the output stream.
            String httpResponse = HttpResponseBuilder.getHttpResponse(httpMethod, urlPath, headers, inputStream, outputStream);
            System.out.println("Sending response: " + httpResponse);
            outputStream.write(httpResponse.getBytes("UTF-8"));
            // Close the input and output streams.
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            
            try {
              //Socket is closed
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }
}
