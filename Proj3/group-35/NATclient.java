import java.net.*;
import java.io.*;

public class NATclient extends Thread {
	
	private static final int MAX_DATA_LEN = 1000;
	private static final String IP = NATthings.genIP();
	private static final String MAC = NATthings.genMAC();
	private static final byte delim = NATthings.getByteDelim();

	private static int port;
	private static int type; // internal (0) or external (1)

	private static String hostname;
	private static String givenIP;

	private static String nBoxIP;
	private static String nBoxMAC;

	private static BufferedReader br;
	private static DataInputStream dis;
	private static DataOutputStream dos;

	private static Socket client;
	private static DatagramSocket dgs;
	private static DatagramPacket dgp;
	
	public  NATclient() {
		br = null;
		dis = null;
		dos = null;

		client = null;
		dgs = null;
		dgp = null;
	}
	
	@Override
	public void run() {
			
		br = new BufferedReader(new InputStreamReader(System.in));

		System.out.print("\nEnter the hostname: ");
		String input = "";

		try {
			input = br.readLine().trim();
		} catch (IOException e) {
			System.err.println("Error reading input: " + e);
			quit();
		}

		// check for valid IPv4 address
		boolean checkIP = input.matches("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

		if (input.length() == 0) {
			input = "localhost";
		} else if (!input.equals("localhost") && !checkIP) {
			quit();
		}

		// set global hostname
		hostname = input;

		// get the port number
		System.out.print("Enter the port number: ");
		input = "";

		try {
			input = br.readLine().trim();
		} catch (IOException e) {
			System.err.println("Error reading input: " + e);
			quit();
		}

		// check for valid port number
		boolean checkPort = input.matches("^[0-9]{4}$");

		if (input.length() == 0) {
			input = "1234";
		} else if (!checkPort) {
			quit();
		}

		// set global port
		port = Integer.parseInt(input);

		System.out.print("Internal [0] or external [1] client? ");
		input = "";

		try {
			input = br.readLine().trim();
		} catch (IOException e) {
			System.err.println("Error reading input: " + e);
			quit();
		}

		// check if entered 0 or 1
		boolean checkType = input.matches("^[0|1]$");

		if (input.length() == 0) {
			input = "0";
		} else if (!checkType) {
			quit();
		}

		type = Integer.parseInt(input);

		// output the set defaults
		System.out.println("\nClient \033[32mdetails\033[0m:");
		System.out.println("  hostname -> " + hostname);
		System.out.println("  port     -> " + port);
		System.out.println("  type     -> " + type + " (" + getClientType() + ")");
		System.out.println("  IP       -> " + IP);
		System.out.println("  MAC      -> " + MAC);
		System.out.println("--------------------------------\n");
		 		
		try {
			dgs = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println("Error creating first DatagramSocket: " + e);
			nBoxIP = "N/A";
			nBoxMAC = "N/A";
			return;
		}

		InetAddress hostAdd = null;

		try {
			hostAdd = InetAddress.getByName(hostname);
		} catch (UnknownHostException e) {
			System.err.println("Error getting host name: " + e);
			nBoxIP = "N/A";
			nBoxMAC = "N/A";
			return;
		}

		byte[] data = new byte[MAX_DATA_LEN];

		// send signal to get IP and MAC of natbox
		data[0] = 'i';
		data[1] = delim;

		// send signal
		dgp = new DatagramPacket(data, data.length, hostAdd, port);
		try {
			dgs.send(dgp);
		} catch (IOException e) {
			System.err.println("Error sending IP and MAC signal: " + e);
		}

		boolean check = true;

		// wait to recieve IP and MAC from natbox
		while (check) {
			try {
				dgs.receive(dgp);
			} catch (IOException e) {
				System.err.println("Error recieving packet: " + e);
				continue;
			}

			// get packet from sender
			byte[] packet = dgp.getData();

			// if natbox sent IP and MAC address
			if (packet[0] == 'm') {
				String pack = new String(packet).trim();
				pack = pack.substring(2);

				int idx = pack.indexOf("#");
				nBoxIP = pack.substring(0, idx);
				nBoxMAC = pack.substring(idx + 1).trim();

				System.out.println("NATBox \033[32mdetails\033[0m:");
				System.out.println("  IP  -> " + nBoxIP);
				System.out.println("  MAC -> " + nBoxMAC + "\n");
				check = false;
				break;
			}
		}
		
		if (type == 0) {
			// internal client
			// request IP via DHCP
			int idx = 0;

			try {
				dgs = new DatagramSocket();
			} catch (SocketException e) {
				System.err.println("Error creating DatagramSocket: " + e);
				return;
			}

			hostAdd = null;

			try {
				hostAdd = InetAddress.getByName(hostname);
			} catch (UnknownHostException e) {
				System.err.println("Error getting host name: " + e);
				quit();
			}

			data = new byte[MAX_DATA_LEN];

			// send discover signal to receive offered IP
			data[0] = 'd';
			data[1] = delim;
			idx = 2;

			// add IP to data
			byte[] bip = IP.getBytes();

			for (int i = 0; i < bip.length; i++) {
				data[idx] = bip[i];
				idx += 1;
			}

			// send discover packet with IP attached
			dgp = new DatagramPacket(data, data.length, hostAdd, port);
			try {
				dgs.send(dgp);
			} catch (IOException e) {
				System.err.println("Error sending discover packet: " + e);
				quit();
			}

			System.out.println("Discover packet sent, awaiting response ...");

			String recOffIP = "";
			check = true;

			// keep waiting for offer, then send request to use and wait for ack
			while (check) {
				try {
					dgs.receive(dgp);
				} catch (IOException e) {
					System.err.println("Error recieving packet: " + e);
					continue;
				}

				// get packet from sender
				byte[] packet = dgp.getData();
				int readLen = 0;

				// if received 'offer'
				if (packet[0] == 'o') {
					data = new byte[MAX_DATA_LEN];

					// construct request packet
					data[0] = 'r';
					data[1] = delim;

					// copy offered IP to request for use
					for (idx = 2; idx < packet.length; idx++) {
						data[idx] = packet[idx];
						readLen += 1;

						if (packet[idx] == delim) {
							break;
						}
					}

					recOffIP = new String(packet, 2, readLen - 1);
					System.out.println("Recieved offer for IP: " + recOffIP);

					// send request packet
					dgp = new DatagramPacket(data, data.length, hostAdd, port);
					try {
						dgs.send(dgp);
					} catch (IOException e) {
						System.err.println("Error sending data: " + e);
						quit();
					}

					continue;
				}

				// if received 'ack'
				if (packet[0] == 'a') {
					givenIP = recOffIP;
					check = false;
					dgs.close();
					System.out.println("Request acknowledged, internal IP:  " + givenIP + "\n");
				}
			}
			System.out.println("--------------------------------\n");
		} else {
			// external client
			dgs = null;
			hostAdd = null;

			try {
				hostAdd = InetAddress.getByName(hostname);
			} catch (UnknownHostException e) {
				System.err.println("Unable to get host address: " + e);
			}

			int idx = 0;
			givenIP = IP;

			try {
				dgs = new DatagramSocket();
			} catch (SocketException e) {
				System.err.println("Error creating DatagramSocket: " + e);
				return;
			}

			// signal that client is external
			data = new byte[MAX_DATA_LEN];
			data[0] = 'e';
			data[1] = delim;

			idx = 2;
			byte[] bip = IP.getBytes();

			// add IP to data packet
			for (int i = 0; i < bip.length; i++) {
				data[idx] = bip[i];
				idx += 1;
			}

			data[idx + 1] = delim;

			// send data packet
			dgp = new DatagramPacket(data, data.length, hostAdd, port);
			try {
				dgs.send(dgp);
			} catch (IOException e) {
				System.err.println("Error sending external client's packet: " + e);
			}
		}

		// connect the client to the NAT box
		try {
			client = new Socket(hostname, port);
			dis = new DataInputStream(client.getInputStream());
			dos = new DataOutputStream(client.getOutputStream());
			br = new BufferedReader(new InputStreamReader(System.in));
		} catch (IOException e) {
			System.err.println("Cannot create socket: " + e);
			return;
		}

		// start listening for messages
		startRecieve();

		System.out.println("You may now type messages to other connected clients.");
		System.out.println("\nThe commands that are recognized are:");
		System.out.println("    \033[32mmsg\033[0m     e.g. \033[32mmsg\033[0m <client IP> <message>");
		System.out.println("    \033[31mquit\033[0m\n");

		// get input from client
		while (true) {
			String msg = "";

			// allow client to type message
			System.out.print("[" + givenIP + "] ~ ");
			try {
				msg = br.readLine();
			} catch (IOException e) {
				continue;
			}

			if (msg == null) {
				continue;
			}

			// cut off excess whitespace
			msg = msg.trim();

			// if client wants to message another client
			if (msg.startsWith("msg ")) {
				String shortMsg = msg.substring(msg.indexOf(" ") + 1);

				int cidx = shortMsg.indexOf(" ");
				if (cidx < 0) {
					System.out.println("Message not sent.\n");
					continue;
				}

				String tempIP = shortMsg.substring(0, cidx);
				String content = shortMsg.substring(cidx + 1).trim();

				if (content.length() < 1 || content.equals(" ")) {
					System.out.println("Message not sent.\n");
					continue;
				}

				// check if IP is valid
				checkIP = tempIP.matches(
					"^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

				if (!checkIP) {
					continue;
				}

				// send packet containing source # destination # payload
				data = new byte[MAX_DATA_LEN];

				data = NATthings.genPacket(givenIP, tempIP, content.getBytes());
				String dr = new String(data);

				// write to output stream
				try {
					dos.writeUTF(dr);
				} catch (IOException e) {
					System.err.println("Error writing output: " + e);
				}
			}

			// if client wants to quit
			if (msg.startsWith("quit")) {
				System.out.println("\nGoodbye!");
				break;
			}
		}

		quit();
	}
	
	public String getClientType() {
		return (type == 0 ? "internal" : "external");
	}
	
	private void startRecieve() {
		new Thread() {
			@Override
			public void run() {
				recieve();
			}
		}.start();
	}
	
	private void recieve() {
		byte[] packet = new byte[MAX_DATA_LEN];

		// continually listen
		while (true) {
			try {
				dis.read(packet);
			} catch (IOException e) {
				continue;
			}

			// split packet
			String[] data = NATthings.splitPacket(packet);

			if (data.length != 3) {
				System.out.println("\n\033[31mMessage not sent.\033[0m.");
				System.out.print("[" + givenIP + "] ~ ");
				continue;
			}

			String src = data[0];
			String content = data[2].trim();

			System.out.println("\n\033[34m[" + src + "] ~ " + content + "\033[0m");
			System.out.print("[" + givenIP + "] ~ ");
		}

	}
	
	private void quit() {
		System.out.println("\nExiting ...");

		try {
			if (br != null) {
				br.close();
			}

			if (dis != null) {
				dis.close();
			}

			if (dos != null) {
				dos.close();
			}

			if (client != null) {
				client.close();
			}

			if (dgs != null) {
				dgs.close();
			}
		} catch (IOException e) {
			System.err.println("Error quitting: " + e);
		}

		System.exit(0);
	}
	
	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(100);

					if (br != null) {
						br.close();
					}

					if (dis != null) {
						dis.close();
					}

					if (dos != null) {
						dos.close();
					}

					if (client != null) {
						client.close();
					}

					if (dgs != null) {
						dgs.close();
					}
				} catch (InterruptedException | IOException e) {
					System.err.println("Error in shutdown hook : " + e);
				}
			}
		});

		NATclient cl = new NATclient();
		cl.start();
	}
}