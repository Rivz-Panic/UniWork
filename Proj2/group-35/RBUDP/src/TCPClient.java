import java.io.*;
import java.net.*;

public class TCPClient {
	
	public static String host;
	public static String fileName;
	
    public static void main(String[] args) throws IOException {
        Socket socket = null;
        host = args[0];

        socket = new Socket(host, 1234);
        fileName = args[1];
        File file = new File(fileName);
        // Get the size of the file
        long fileLen = file.length();
        byte[] bytePkg = new byte[(int)file.length()];
        InputStream in = new FileInputStream(file);
        OutputStream out = socket.getOutputStream();
        System.out.println("Sending file "+ file.getName());

        int count;
        while ((count = in.read(bytePkg)) > 0) {
            out.write(bytePkg, 0, count);  
            System.out.println("Sent "+ count + " bytes");
        }

        out.close();
        in.close();
        socket.close();
    }
    
    public static void use() throws UnknownHostException, IOException {
    	Socket socket = null;
        socket = new Socket(host, 1234);
        File file = new File(fileName);
        // Get the size of the file
        long fileLen = file.length();
        byte[] bytePkg = new byte[(int)file.length()];
        InputStream in = new FileInputStream(file);
        OutputStream out = socket.getOutputStream();
        System.out.println("Sending file "+ file.getName());

        int count;
        while ((count = in.read(bytePkg)) > 0) {
            out.write(bytePkg, 0, count);  
            System.out.println("Sent "+ count + " bytes");
        }

        out.close();
        in.close();
        socket.close();
    }
}
