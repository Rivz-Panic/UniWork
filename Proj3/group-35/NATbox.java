import java.io.*;
import java.net.*;
import java.util.*;

public class NATbox {

	private static final int MaxClients = 10;
	private static final int MaxPacSize = 1000;
	private static final long RefreshRate = 60000;

	private static final int port = 1234;
	private static final String IP = NATthings.genIP();
	private static final String MAC = NATthings.genMAC();

	private static final byte delim = NATthings.getByteDelim();

	private static int currentClient = 0;
	private static long refresher = 0L;
	private static boolean available = true;

	private static Socket client;
	private static ServerSocket server;

	private static ClientHandler[] clients;

	private static LinkedList<NATthings> local;
	private static LinkedList<NATthings> natTable;

    public NATbox() {
		client = null;
		server = null;
		clients = new ClientHandler[MaxClients];
		local = new LinkedList<>();
		natTable = new LinkedList<>();
	}

	public void run() {
		final Scanner sc = new Scanner(System.in);

		System.out.print("\nEnter the NAT table refresh time in milliseconds: ");
		final String input = sc.nextLine();

		sc.close();

		if (input.length() < 1) {
			refresher = RefreshRate;
		}

		try {
			refresher = Long.parseLong(input);
		} catch (final NumberFormatException e) {
			refresher = RefreshRate;
		}

		System.out.println("\nNATbox\033[32mdetails\033[0m:");
		System.out.println("  refresh: " + refresher + " ms");
		System.out.println("  IP:      " + IP);
		System.out.println("  MAC:      " + MAC);
		System.out.println(".........................................");

		final Timer timer = new Timer();

		final TimerTask tt1 = new TimerTask() {
			@Override
			public void run() {
				
				synchronized (natTable) {
					natTable.removeFirst();
				}

				synchronized (local) {
					local.get(currentClient).setextIP("0");
				}

				clients[currentClient].closeAll();

				try {
					Thread.sleep(50);
				} catch (final InterruptedException e) {
				}

				currentClient += 1;
				System.out.println("\nCurrent NAT Table (after refresh)");
				System.out.println(".........................................");
				NATthings.printList(natTable);
				System.out.println("\nCurrent Internal IPs (after refresh)");
				System.out.println(".........................................");
				NATthings.printList(local);
				available = true;
			}
		};

		final TimerTask tt2 = new TimerTask() {
			@Override
			public void run() {
				synchronized (natTable) {
					natTable.removeFirst();
				}
				synchronized (local) {
					local.get(currentClient).setextIP("0");
				}
				clients[currentClient].closeAll();
				try {
					Thread.sleep(50);
				} catch (final InterruptedException e) {
				}
				currentClient += 1;
				System.out.println("\nCurrent NAT Table (after refresh)");
				System.out.println(".........................................");
				NATthings.printList(natTable);
				System.out.println("\nCurrent Internal IPs (after refresh)");
				System.out.println(".........................................");
				NATthings.printList(local);
				available = false;
			}
		};

		makeInternalIPs(MaxClients);

		try {
			server = new ServerSocket(port);
		} catch (final IOException e) {
			System.err.println("Error creating server socket: " + e);
		}

		while (true) {
			DatagramSocket dgs = null;
			DatagramPacket dgp = null;

			String clientIP = "";
			int readPointer = 0;
			int localAddress = 0;
			boolean shouldListen = true;

			try {
				dgs = new DatagramSocket(port);
			} catch (final SocketException e) {
				System.err.println("Error on port " + port + ": " + e);
			}

			byte[] dataPac = new byte[MaxPacSize];
			dgp = new DatagramPacket(dataPac, dataPac.length);

			System.out.println("\n\033[33mWaiting\033[0m for connection ...\n");

			while (shouldListen) {
				try {
					dgs.receive(dgp);
				} catch (final IOException e) {
					System.err.println("Error receiving packets: " + e);
					dgs.close();
					return;
				}

				final byte[] data = dgp.getData();
				final int clientPort = dgp.getPort();
				readPointer = 0;

				final InetAddress clientAddress = dgp.getAddress();
				if (data[0] == 'i') {
					dataPac = new byte[MaxPacSize];
					String st = "";

					dataPac[0] = 'm';
					dataPac[1] = delim;
					st += new String(new byte[] { dataPac[0], dataPac[1] });

					int idx = 2;
					byte[] bb = IP.getBytes();

					for (int i = 0; i < bb.length; i++) {
						dataPac[idx] = bb[i];
						st += new String(new byte[] { dataPac[idx] });
						idx += 1;
					}
					dataPac[idx + 1] = delim;

					idx += 2;
					bb = MAC.getBytes();
					for (int i = 0; i < bb.length; i++) {
						dataPac[idx] = bb[i];
						st += new String(new byte[] { dataPac[idx] });
						idx += 1;
					}
					dgp = new DatagramPacket(dataPac, dataPac.length, clientAddress, clientPort);
					try {
						dgs.send(dgp);
					} catch (final IOException e) {
						System.err.println("Error sending IP and MAC to client: " + e);
					}
				}

				if (data[0] == 'e') {
					System.out.println("[external] Packet received from " + clientAddress.getHostAddress());

					for (int i = 2; i < data.length; i++) {
						readPointer += 1;
						if (data[i] == delim) {
							break;
						}
					}

					clientIP = new String(data, 2, readPointer - 1);
					System.out.println("\nexternal client " + clientIP + " has \033[32mconnected\033[0m.");
					final NATthings ne = new NATthings(clientIP, clientIP);
					natTable.add(ne);
					shouldListen = false;
					dgs.close();
				} else {
					System.out.println("[internal] Packet received from " + clientAddress.getHostAddress());
					readPointer = 0;
					if (data[0] == 'd') {
						for (int i = 2; i < data.length; i++) {
							readPointer += 1;

							if (data[i] == delim) {
								break;
							}
						}

						clientIP = new String(data, 2, readPointer - 1).trim();
						System.out.println("    public IP of client is " + clientIP);
						byte[] internalIP = null;
						dataPac = new byte[MaxPacSize];
						dataPac[0] = 'o';
						dataPac[1] = delim;

						readPointer = 1;
						for (int i = 0; i < local.size(); i++) {
							if (!local.get(i).getextIP().equals("0")) {
								continue;
							}
							internalIP = local.get(i).getintIP().getBytes();
							localAddress = i;
							for (int j = 0; j < internalIP.length; j++) {
								dataPac[j + 2] = internalIP[j];
								readPointer += 1;
							}

							dataPac[readPointer + 1] = delim;
							break;
						}

						dgp = new DatagramPacket(dataPac, dataPac.length, clientAddress, clientPort);
						try {
							dgs.send(dgp);
						} catch (final IOException e) {
							System.err.println("Error sending packet: " + e);
						}

						final String sip = new String(internalIP);
						System.out.println("    sent offer IP of " + sip + " to client\n");
					}

					if (data[0] == 'r') {
						String internalIP = "";
						readPointer = 0;
						for (int i = 2; i < data.length; i++) {
							readPointer += 1;
							if (data[i] == delim) {
								break;
							}
						}

						internalIP = new String(data, 2, readPointer - 1);
						System.out.println("    request to use IP of " + internalIP + " recieved");
						final NATthings ne = new NATthings(internalIP, clientIP, refresher);
						natTable.add(ne);
						local.get(localAddress).setextIP(clientIP);

						dataPac = new byte[MaxPacSize];
						dataPac[0] = 'a';
						dataPac[1] = delim;
						dgp = new DatagramPacket(dataPac, dataPac.length, clientAddress, clientPort);
						try {
							dgs.send(dgp);
						} catch (final IOException e) {
							System.err.println("Error sending packet: " + e);
						}

						System.out.println(
								"    request accepted, client " + clientIP + " assigned IP of " + internalIP + "\n");
						System.out.println("internal client " + clientIP + " has \033[32mconnected\033[0m.");

						shouldListen = false;
						dgs.close();
					}
				}
			}

			if (refresher < RefreshRate) {
				if (available) {
					timer.schedule(tt1, refresher);
					available = false;
				} else {
					timer.schedule(tt2, refresher);
					available = true;
				}
			}

			System.out.println("\nCurrent NAT Table");
			System.out.println("......................");
			NATthings.printList(natTable);

			try {
				client = server.accept();
			} catch (final IOException e) {
				System.err.println("Error accepting client: " + e);
				continue;
			}

			int i;
			for (i = 0; i < MaxClients; i++) {
				if (clients[i] == null) {
					clients[i] = new ClientHandler(client, IP, clients, natTable, local);

					clients[i].start();
					break;
				}
			}

			if (i == MaxClients) {
				System.out.println("Maximum number of clients has been reached.");

				try {
					client.close();
				} catch (final IOException e) {
					System.err.println("Error closing client: " + e);
				}
			}
		}
	}


	private static void makeInternalIPs(final int limit) {
		if (limit > 256) {
			System.err.println("Too many clients for internal IP creation.");
			System.exit(0);
		}
		local.removeAll(local);
		final String pre = "192.168.0.";

		for (int i = 0; i < limit; i++) {
			final String lip = pre + "" + i;
			final NATthings ne = new NATthings(lip, "0");
			local.add(ne);
		}
	}

	public static void main(final String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
					System.out.println("Quitting ...");

					if (client != null) {
						client.close();
					}

					if (server != null) {
						server.close();
					}
				} catch (InterruptedException | IOException e) {
					System.err.println("Error in shutdown hook : " + e);
				}
			}
		});

		final NATbox
 nb = new NATbox();
		nb.run();
	}

}
