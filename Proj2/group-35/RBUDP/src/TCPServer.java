import java.io.*;
import java.net.*;

public class TCPServer {
	public static String fileName;
	public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        fileName= args[0];
        try {
            serverSocket = new ServerSocket(1234);
            System.out.println("Ready!");
        } catch (IOException ex) {
            System.out.println("Can't setup server on this port number. ");
        }

        Socket socket = null;
        InputStream in = null;
        OutputStream out = null;

        try {
            socket = serverSocket.accept();
        } catch (IOException ex) {
            System.out.println("Can't accept client connection. ");
        }

        try {
            in = socket.getInputStream();
        } catch (IOException ex) {
            System.out.println("Can't get socket input stream. ");
        }

        try {
            out = new FileOutputStream(fileName);
        } catch (FileNotFoundException ex) {
            System.out.println("File not found. ");
        }

        byte[] bytes = new byte[16*1024];

        int count;
        while ((count = in.read(bytes)) > 0) {
            out.write(bytes, 0, count);
            System.out.println("Amount of bytes being sent: " + count);
        }

        out.close();
        in.close();
        socket.close();
        serverSocket.close();
    }
	
	public static void use() throws IOException {
		
		ServerSocket serverSocket = null;
		
		try {
            serverSocket = new ServerSocket(1234);
            System.out.println("Ready!");
        } catch (IOException ex) {
            System.out.println("Can't setup server on this port number. ");
        }

        Socket socket = null;
        InputStream in = null;
        OutputStream out = null;

        try {
            socket = serverSocket.accept();
        } catch (IOException ex) {
            System.out.println("Can't accept client connection. ");
        }

        try {
            in = socket.getInputStream();
        } catch (IOException ex) {
            System.out.println("Can't get socket input stream. ");
        }

        try {
            out = new FileOutputStream(fileName);
        } catch (FileNotFoundException ex) {
            System.out.println("File not found. ");
        }

        byte[] bytes = new byte[16*1024];

        int count;
        while ((count = in.read(bytes)) > 0) {
            out.write(bytes, 0, count);
            System.out.println("Amount of bytes being sent: " + count);
        }

        out.close();
        in.close();
        socket.close();
        serverSocket.close();
	}
}
