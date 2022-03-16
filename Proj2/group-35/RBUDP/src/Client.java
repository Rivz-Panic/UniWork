import java.net.*;
import java.util.*;
import java.io.*;

public class Client implements Runnable
{	
    static double difference = 0.0;
    static int transferredAmount = 0;
    static double oldTime = 0.0;
    static int sizeChange = 0;
    static int oldSize = 0;
    static int sendSpeed = 100;
    static String serverAdd;
    static int port = 1234;
    static String fileName;
    static String sentFileName;
    static Timer timer = null;
    static int resent = 0;
    
    public static void main(final String[] args) throws Exception {
        Client.serverAdd = args[0];
        Client.fileName = args[1];
        Client.sentFileName = args[2];
        prep();
    }
    
    public static void prep() throws IOException {
        System.out.println("Prepping file");
        final DatagramSocket datagramSocket = new DatagramSocket();
        final InetAddress byName = InetAddress.getByName(serverAdd);
        final byte[] bytes = sentFileName.getBytes();
        datagramSocket.send(new DatagramPacket(bytes, bytes.length, byName, port));
        final File file = new File(fileName);
        final FileInputStream fileInputStream = new FileInputStream(file);
        final byte[] fileSize = new byte[(int)file.length()];
        fileInputStream.read(fileSize);
        startTimer();
        System.out.println("Sending file");
        sendFile(datagramSocket, fileSize, byName);
        final String finalStatistics = sendingInfo(fileSize, Client.resent);
        final byte[] bytes2 = finalStatistics.getBytes();
        datagramSocket.send(new DatagramPacket(bytes2, bytes2.length, byName, port));
        datagramSocket.close();
        fileInputStream.close();
    }
    
    private static void sendFile(final DatagramSocket datagramSocket, final byte[] array, final InetAddress address) throws IOException {
        int n = 0;
        int i = 0;
        int n2 = 0;
        for (int j = 0; j < array.length; j += 1021) {
            n++;
            final byte[] buf = new byte[1024];
            buf[0] = (byte)(n >> 8);
            buf[1] = (byte)n;
            boolean b;
            if (j + 1021 >= array.length) {
                b = true;
                buf[2] = 1;
            }
            else {
                b = false;
                buf[2] = 0;
            }
            if (!b) {
                for (int k = 0; k <= 1020; ++k) {
                    buf[k + 3] = array[j + k];
                }
            }
            else if (b) {
                for (int l = 0; l < array.length - j; ++l) {
                    buf[l + 3] = array[j + l];
                }
            }
            final int nextInt = new Random().nextInt(100);
            final DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, address, port);
            if (nextInt <= sendSpeed) {
                datagramSocket.send(datagramPacket);
            }
            Client.transferredAmount += datagramPacket.getLength();
            Client.transferredAmount = Math.round((float)Client.transferredAmount);
            
            System.out.println("Sent packet " + n);
            while (!false) {
                final byte[] buf2 = new byte[2];
                final DatagramPacket p3 = new DatagramPacket(buf2, buf2.length);
                boolean b2;
                try {
                    datagramSocket.setSoTimeout(50);
                    datagramSocket.receive(p3);
                    i = ((buf2[0] & 0xFF) << 8) + (buf2[1] & 0xFF);
                    b2 = true;
                }
                catch (SocketTimeoutException ex) {
                    System.out.println("Socket timed out ");
                    b2 = false;
                }
                if (i == n && b2) {
                    System.out.println("Received number " + i);
                    break;
                }
                datagramSocket.send(datagramPacket);
                System.out.println("Resending number " + n);
                n2++;
            }
        }
    }
    
    private static String sendingInfo(final byte[] array, final int i) {
        System.out.println("File " + fileName + " has been sent");
        final double n = array.length / 1024;
        final double d = Client.timer.getTimeElapsed() / 1000.0;
        final double d2 = n / 1000.0;
        final double n2 = d2 / d;
        System.out.println("");
        System.out.println("");
        System.out.println("Statistics of send");
        System.out.println("------------------------------");
        System.out.println("File " + fileName + " has been sent successfully.");
        System.out.println("With file size of  " + Client.transferredAmount / 1000 + " KB");
        System.out.println("Sending time of " + Client.timer.getTimeElapsed() / 1000.0 + " Seconds");
        System.out.printf("Throughput was %.2f MB Per Second\n", n2);
        System.out.println("Resent packages  " + i + " times");
        System.out.println("------------------------------");
        return "File Size: " + d2 + "mb\n" + "Throughput: " + n2 + " Mbps" + "\nTotal transfer time: " + d + " Seconds";
    }
    
    private static void startTimer() {
        Client.timer = new Timer(0);
    }
    
    @Override
    public void run() {
        try {
            prep();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}