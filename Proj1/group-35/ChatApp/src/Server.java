/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.*;
import java.util.*;
import java.net.*;

/**
 *
 * @author 21028540
 */
public class Server implements Runnable {
	// Vector to store active clients
	static Vector<ClientHandler> vc = new Vector<>();
	int portNumber = 1234;
	ServerSocket ss = null;
	Socket s = null;
	private String name;

	// counter for clients
	static int i = 0;

	public Server() throws IOException {
		// server is listening on port 1234
		portNumber = 1234;
		ss = null;
		s = null;
		// Attempting to reach server
		try {
			System.out.println("Server.<init>()");
			ss = new ServerSocket(portNumber);
			this.run();
		} catch (IOException e) {
			System.err.println("Couldnt listen to port: " + portNumber);
			System.exit(1);
		}
	}

	// running infinite loop for getting
	// client request

	@Override
	public void run() {
		try {
			System.out.println("Server.run()");
			while (true) {
				// Accept the incoming request
				s = ss.accept();

				System.out.println("New client request received : " + s);

				// obtain input and output streams
				DataInputStream dis = new DataInputStream(s.getInputStream());
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());
				System.out.println("Creating a new handler for this client...");

				// Create a new handler object for handling this request.

				ClientHandler ch = new ClientHandler(s, "Client" + i, dis, dos);

				// Create a new Thread with this object.
				Thread t = new Thread(ch);

				System.out.println("Adding this client to active client list");

				// add this client to active clients list
				vc.add(ch);
				// start the thread.
				t.start();

				// increment i for new client.
				i++;

			}
		} catch (Exception e) {
			System.err.printf("Server couldnt run");
		}

	}

	public static void logout(Socket s) throws IOException {
		if (s.isConnected()) {
			s.close();
		}
	}

}
