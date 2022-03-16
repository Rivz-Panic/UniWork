/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.*;
import java.net.*;
import java.util.Scanner;
import javax.swing.JOptionPane;
import java.util.Vector;
import javax.swing.JFrame;

/**
 *
 * @author 21028540
 */
public class Client {
	final static int ServerPort = 1234;
	public static String name;
	public static boolean duplicate = false;

	public static void main(String args[]) throws UnknownHostException, IOException {

		Scanner scn = new Scanner(System.in);

		// getting localhost ip
		InetAddress ip = InetAddress.getByName("localhost");
		Socket s = null;
		// establish the connection
		System.out.println("Conencting to server on port: " + ServerPort);
		try {
			s = new Socket(ip, ServerPort);
			System.out.println("Conenction successful!");
		} catch (IOException e) {
			System.err.println("Fatal connection error!, server not found");
			System.exit(0);
		}

		// obtaining input and out streams
		DataInputStream dis = new DataInputStream(s.getInputStream());
		DataOutputStream dos = new DataOutputStream(s.getOutputStream());
		System.out.println("Please select unique nickname");
		name = scn.nextLine();

		JFrame frame = new JFrame("ChatClient");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(650, 400);

		dos.writeUTF(name);
		if (s.isConnected()) {
			// sendMessage thread
			Thread sendMessage = new Thread(new Runnable() {
				@Override
				public void run() {
					while (true) {
						if (scn.hasNext()) {
							// read the message to deliver.
							String msg = scn.nextLine();

							try {
								// write on the output stream
								dos.writeUTF(msg);
							} catch (IOException e) {
								System.out.println(" Fatal error in Client Send Message");
								break;
							}
						}

						else {
							System.exit(0);
						}
					}
				}
			});

			// readMessage thread
			Thread readMessage = new Thread(new Runnable() {
				@Override
				public void run() {

					while (true) {
						JFrame frame = new JFrame("ChatClient");
						try {
							// read the message sent to this client
							String msg = dis.readUTF();
							System.out.println(msg);
						} catch (IOException e) {
							System.out.println("Disconnected from Server!");
							break;
						}
					}
					System.exit(0);
				}
			});

			sendMessage.start();
			readMessage.start();
		}
	}
}
