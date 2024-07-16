import java.io.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class HttpResponseBuilder {
/*The getHttpResponse function in the Main class constructs the HTTP response based on the HTTP method, 
URL path, and headers received from the client.*/
    public static String getHttpResponse(String httpMethod, String urlPath, Map<String, String> headers,
                                         BufferedReader inputStream, OutputStream outputStream) throws IOException {
        String httpResponse;
        if ("GET".equals(httpMethod)) {
            if ("/".equals(urlPath)) {
                httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
            } else if (urlPath.startsWith("/echo/")) {
                String echoStr = urlPath.substring(6); 
                String acceptEncoding = headers.get("Accept-Encoding");
                boolean supportsGzip = false;
                if (acceptEncoding != null) {
                    String[] encodings = acceptEncoding.split(",");
                    for (String encoding : encodings) {
                        if ("gzip".equalsIgnoreCase(encoding.trim())) {
                            supportsGzip = true;
                            break;
                        }
                    }
                }

                //Gzip is used for file compression and decompression.
                if (supportsGzip) {
                    
                    byte[] gzipData = compressString(echoStr);
                    httpResponse = "HTTP/1.1 200 OK\r\nContent-Encoding: gzip\r\nContent-Type: text/plain\r\nContent-Length: "
                            + gzipData.length + "\r\n\r\n";
                    outputStream.write(httpResponse.getBytes("UTF-8"));
                    outputStream.write(gzipData);
                } else {
                    httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: "
                            + echoStr.length() + "\r\n\r\n" + echoStr;
                }
            } else if ("/user-agent".equals(urlPath)) {
                String userAgent = headers.get("User-Agent");
                httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: "
                        + userAgent.length() + "\r\n\r\n" + userAgent;
            } else if (urlPath.startsWith("/files/")) {
                String filename = urlPath.substring(7); 
                File file = new File(Main.directory, filename);
                if (file.exists()) {
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: "
                            + fileContent.length + "\r\n\r\n" + new String(fileContent);
                } else {
                    httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
                }
            } else {
                httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
            }
        } else if ("POST".equals(httpMethod) && urlPath.startsWith("/files/")) {
            String filename = urlPath.substring(7); // Extract the filename after "/files/"
            File file = new File(Main.directory, filename);
            if (!file.getCanonicalPath().startsWith(new File(Main.directory).getCanonicalPath())) {
                httpResponse = "HTTP/1.1 403 Forbidden\r\n\r\n";
            } else {
                
                // Get the length of the request body
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                char[] buffer = new char[contentLength];
                int bytesRead = inputStream.read(buffer, 0, contentLength);
                if (bytesRead == contentLength) {
                    // Write the request body to the file
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                        writer.write(buffer, 0, bytesRead);
                    }
                    httpResponse = "HTTP/1.1 201 Created\r\n\r\n";
                } else {
                    httpResponse = "HTTP/1.1 500 Internal Server Error\r\n\r\n";
                }
            }
        } else {
            httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
        }
        return httpResponse;
    }

    private static byte[] compressString(String data) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(data.getBytes("UTF-8"));
        }
        return byteArrayOutputStream.toByteArray();
    }
}
