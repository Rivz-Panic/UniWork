import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;

public class ClientHandler extends Thread {

	// globals
	private static final int CLIENT_LIMIT = 10; // allow maximum of 10 clients

	private boolean running = true;
	private boolean internal = false;
	private String nBoxIP = "";

	private Socket client;
	private DataInputStream dis;
	private DataOutputStream dos;

	private NATthings clientEntry;
	private ClientHandler[] curClients;

	private LinkedList<NATthings> natList;
	private LinkedList<NATthings> localList;

	public ClientHandler(Socket client, String nBoxIP, ClientHandler[] curClients,
			LinkedList<NATthings> natList, LinkedList<NATthings> localList) {

		this.client = client;
		this.nBoxIP = nBoxIP;
		this.curClients = curClients;
		this.natList = natList;
		this.localList = localList;
		this.clientEntry = this.natList.getLast();
	}

	/**
	 * Override the default Thread.run method
	 */
	@Override
	public void run() {
		// if data stream acqusition was unsuccessful
		
		boolean acqStreams;
		
		try {
			this.dis = new DataInputStream(this.client.getInputStream());
			this.dos = new DataOutputStream(this.client.getOutputStream());
		} catch (IOException e) {
			for (ClientHandler ch : this.curClients) {
				if (ch != this) {
					continue;
				}

				System.out.println(this.clientEntry.getintIP() +
					" has \033[32mtimed out\033[0m");

				ch = null;
				acqStreams = false;
			}
		}

		acqStreams = true;
		
		if (!acqStreams) {
			return;
		}

		// check if internal or external
		this.internal = !this.clientEntry.getintIP().equals(this.clientEntry.getextIP());

		byte[] packet = null;

		// handle packet reception
		while (running) {
			String msg = "";

			try {
				msg = this.dis.readUTF();
			} catch (IOException e) {
				running = false;
				break;
			}

			// if quit signal received
			if (msg.equals("quit")) {
				break;
			}

			packet = msg.trim().getBytes();

			// get contents from packet
			String[] content = NATthings.splitPacket(packet);
			String src = content[0];
			String des = content[1];
			String payload = content[2];

			boolean desExisits = false;
			boolean isSrcInternal = false;
			boolean isDestInternal = false;

			// check if source and destination are internal or external
			for (ClientHandler ch : this.curClients) {
				if (ch == null) {
					continue;
				}

				NATthings entry = ch.getClientEntry();
				String iip = entry.getintIP().trim();
				String eip = entry.getextIP().trim();

				if (des.equals(iip) || des.equals(eip)) {
					desExisits = true;
				}

				// if the internal IP is the same as the source
				if (iip.equals(src)) {
					isSrcInternal = !iip.equals(eip);
				}

				// if the internal or external IP is the same as the destination
				if (iip.equals(des) || eip.equals(des)) {
					isDestInternal = !iip.equals(eip);
				}
			}

			if (!desExisits) {
				System.out.println("\nClient \033[33m" + src + "\033[0m tried sending to non-existent client.\n");
				continue;
			}

			System.out.println("\n[" + src + "] -> [" + des + "] : \033[34m" +
				payload + "\033[0m");
			System.out.println("  source is " + getClientType(isSrcInternal));
			System.out.println("  destination is " + getClientType(isDestInternal) + "\n");

			// apply routing roules described in specification
			if (isSrcInternal) {
				// source is internal

				if (isDestInternal) {
					// destination is internal, i.e. internal -> internal
					// don't alter packet if valid

					if (!desExisits) {
						System.out.println("Invalid destination: " + des);
						des = "-1";
					} else {
						System.out.println("Packet valid.");
					}
				} else {
					// destination is external, i.e. internal -> external
					// translate the source

					for (ClientHandler ch : this.curClients) {
						if (ch == null) {
							continue;
						}

						if (ch.getClientEntry().getintIP().equals(src)) {
							src = nBoxIP;
							break;
						}
					}

					System.out.println("Packet source translated.");
				}
			} else {
				// source is external

				if (isDestInternal) {
					// destination is internal, i.e. external -> internal
					// translate the destination

					for (ClientHandler ch : this.curClients) {
						if (ch == null) {
							continue;
						}

						if (ch.getClientEntry().getextIP().equals(des)) {
							des = ch.getClientEntry().getintIP();
							break;
						}
					}

					System.out.println("Packet destination translated.");
				} else {
					// destination is external, i.e. external -> external
					// drop packet

					System.out.println("Packet has been dropped.");

					for (ClientHandler ch : this.curClients) {
						if (ch == null) {
							continue;
						}

						if (ch.getClientEntry().getintIP().equals(src) ||
								ch.getClientEntry().getextIP().equals(src)) {

							// construct packet
							String tp = payload + "#nd";
							packet = NATthings.genPacket(src, des, tp.getBytes());

							try {
								ch.getDos().write(packet);
							} catch (IOException e) {
								System.err.println("Error writing packet to output: " + e);
							}

							break;
						}
					}
				}
			}

			packet = NATthings.genPacket(src, des, payload.getBytes());

			// try send packet
			boolean sendSuccess = false;

			for (ClientHandler ch : this.curClients) {
				if (ch == null) {
					continue;
				}

				// if found client to send to
				if (ch.getClientEntry().getintIP().equals(des)) {
					try {
						ch.getDos().write(packet);
					} catch (IOException e) {
						System.out.println("Client connection \033[31mnot available\033[0m.");
						sendSuccess = false;
						break;
					}

					System.out.println("Packet sent \033[33msuccessfully\033[0m.\n");
					sendSuccess = true;
					break;
				}
			}

			// packet not sent
			if (!sendSuccess) {
				System.out.println("Packet \033[31mnot sent\033[0m.\n");

				for (ClientHandler ch : this.curClients) {
					if (ch == null) {
						continue;
					}

					if (ch.getClientEntry().getintIP().equals(src) ||
							ch.getClientEntry().getextIP().equals(src)) {

						String tp = payload + "#nd";
						packet = NATthings.genPacket(src, des, tp.getBytes());

						try {
							ch.getDos().write(packet);
						} catch (IOException e) {
							System.err.println("Error writing packet: " + e);
						}
					}
				}
			}
		}

		// remove from NAT List
		this.natList.remove(this.clientEntry);

		// allow reuse of IPv4 addresses
		for (NATthings ne : this.localList) {
			if (ne.getintIP().equals(this.clientEntry.getextIP())) {
				ne.setextIP("0");
			}
		}

		// disconnect client
		ClientHandler[] tempClients = new ClientHandler[CLIENT_LIMIT];

		for (int i = 0; i < this.curClients.length; i++) {
			if (this.curClients[i] == this) {
				System.out.println("client " + this.clientEntry.getintIP() +
					" has \033[31mdisconnected\033[0m");
				continue;
			}

			tempClients[i] = this.curClients[i];
		}

		this.curClients = tempClients;

		System.out.println("\nCurrent NAT List");
		System.out.println("------------------");
		NATthings.printList(natList);
		System.out.println("");

		closeAll();
	}


	/**
	 * Closes all connections.
	 */
	public void closeAll() {
		try {
			this.client.close();
			this.dis.close();
			this.dos.close();
		} catch (IOException e) {
			System.err.println("Error closing connections: " + e);
		}
	}

	public boolean isInternal() {
		return this.internal;
	}

	public String getClientType(boolean check) {
		return (check ? "\033[32minternal\033[0m" : "\033[33mexternal\033[0m");
	}
	
	public DataInputStream getDis() {
		return this.dis;
	}

	public DataOutputStream getDos() {
		return this.dos;
	}

	public NATthings getClientEntry() {
		return this.clientEntry;
	}
}
