import java.io.*;
import java.net.*;

public class Server {

	static int totTrans = 0;
	static double diff = 0.0;
	static Timer timer;
	static double prevTime = 0.0;
	static int sizeDiff = 0;
	static int previousSize = 0;
	String dedcoded;
	static String fileName = "";
	static int port = 1234;

	public Server() {
		this.dedcoded = null;
	}

	public static void main(final String[] array) throws Exception {
		System.out.println("Ready!");

		final DatagramSocket datagramSocket = new DatagramSocket(port);
		final byte[] arr = new byte[1024];
		datagramSocket.receive(new DatagramPacket(arr, arr.length));
		String s = null;
		try {
			s = new String(arr, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
		}
		fileName = s.trim();
		final FileOutputStream fileOut = new FileOutputStream(new File(fileName));
		final boolean check = false;
		int i = 0;
		int j = 0;
		while (!check) {
			final byte[] buf = new byte[1024];
			final byte[] b2 = new byte[1021];
			final DatagramPacket p = new DatagramPacket(buf, buf.length);
			datagramSocket.setSoTimeout(0);
			datagramSocket.receive(p);
			final byte[] data = p.getData();
			totTrans += p.getLength();
			totTrans = Math.round((float) totTrans);
			if (i == 0) {
				timer = new Timer(0);
			}
			if (Math.round((float) (totTrans / 1000)) % 50 == 0) {
				System.out.println("");
				System.out.println("");
				System.out.println("------------------------------");
				System.out.println("Milestone Statistics");
				sizeDiff = totTrans / 1000 - previousSize;
				System.out.println("Receieved another: " + sizeDiff + "Kb");
				diff = timer.getTimeElapsed() - prevTime;
				System.out.println("Receieved " + totTrans / 1000 + "Kb");
				System.out.println("Time taken so far: " + timer.getTimeElapsed() / 1000.0 + " Seconds");
				prevTime = timer.getTimeElapsed();
				previousSize = totTrans / 1000;
				System.out.println("Throughput average :" + totTrans / 1000 / timer.getTimeElapsed() + "Mbps");
				System.out.println("Throughput for last 50: " + sizeDiff / diff + "Mbps");
				System.out.println("------------------------------");
				System.out.println("");
				System.out.println("");
			}
			final InetAddress address = p.getAddress();
			port = p.getPort();
			i = ((data[0] & 0xFF) << 8) + (data[1] & 0xFF);
			final boolean b3 = (data[2] & 0xFF) == 0x1;
			if (i == j + 1) {
				j = i;
				for (int k = 3; k < 1024; ++k) {
					b2[k - 3] = data[k];
				}
				fileOut.write(b2);
				System.out.println("Received: Sequence number:" + j);
				recieved(j, datagramSocket, address, port);
			} else {
				System.out.println("Expected sequence number: " + (j + 1) + " but received " + i + ". DISCARDING");
				recieved(j, datagramSocket, address, port);
			}
			if (b3) {
				fileOut.close();
				break;
			}
		}
		final byte[] arr2 = new byte[1024];
		datagramSocket.receive(new DatagramPacket(arr2, arr2.length));
		try {
			final String s2 = new String(arr2, "UTF-8");
			System.out.println("");
			System.out.println("");
			System.out.println("Statistics of transfer");
			System.out.println("------------------------------");
			System.out.println("File has been saved as: " + fileName);
			System.out.println("Statistics of transfer");
			System.out.println("" + s2.trim());
			System.out.println("------------------------------");
		} catch (UnsupportedEncodingException ex2) {
			ex2.printStackTrace();
		}
	}

	private static void recieved(final int i, final DatagramSocket datagramSocket, final InetAddress address, final int port) throws IOException {
		final byte[] buf = { (byte) (i >> 8), (byte) i };
		datagramSocket.send(new DatagramPacket(buf, buf.length, address, port));
		System.out.println("Sent ack: Sequence Number = " + i);
	}

}