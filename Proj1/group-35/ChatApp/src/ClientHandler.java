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
public class ClientHandler implements Runnable {
	Scanner scn = new Scanner(System.in);
	public String name;
	final DataInputStream dis;
	final DataOutputStream dos;
	Socket s;
	public boolean isloggedin;

	// constructor
	public ClientHandler(Socket s, String name, DataInputStream dis, DataOutputStream dos) {
		this.dis = dis;
		this.dos = dos;
		this.name = name;
		this.s = s;
		this.isloggedin = true;

	}

	public void run() {
		setClient();
		System.out.println("ClientHandler.run()");
		System.out.println(this.name + " has succesfully connected!");
		sendToAll(this.name + " has succesfully connected!");
		String received;
		while (true) {
			try {
				// receive the string
				received = dis.readUTF();
				String st = received;
				if (received.equals("logout")) {
					System.out.println(this.name + " is logging out...");
					this.isloggedin = false;
					System.out.println(this.name + " has logged out!");
					sendToAll(this.name + " disconnected!");
					updateList(this);
					Server.logout(this.s);
					s.close();
					break;
				}

				else if (st.startsWith("/help")) {
					this.dos.writeUTF("Funcitonality: \n" + "/users - To see current online users\n"
							+ "/w <name> <message> - To whisper to a friend\n");
				} else if (st.startsWith("/users")) {
					this.dos.writeUTF("List of all current users: " + Server.vc.size() + " currently online");
					for (ClientHandler mr : Server.vc) {
						this.dos.writeUTF(mr.name);
					}
				} else if (st.startsWith("/w ")) {
					int index = st.indexOf(' ', 3);

					// For whispers
					// break the string into message and recipient part for whispers

					String recipient = st.substring(3, index);
					String msgToSend = st.substring(index);
					boolean whisper = false;
					// search for the recipient in the connected devices list.
					// vc is the vector storing client of active users
					for (ClientHandler mc : Server.vc) {
						// if the recipient is found, write on its
						// output stream
						if (mc.name.equals(recipient)) {
							whisper = true;
							try {
								mc.dos.writeUTF(this.name + " whispered: " + /* MsgToSend */ msgToSend);
								whisper = true;
							} catch (IOException e) {
								break;
							}
						}

					}
					if (whisper == false) {
						try {
							this.dos.writeUTF("No one to whisper to with that name!");
						} catch (IOException e) {
							break;
						}
					}
				}
				// For all other send to all messages
				else {
					sendToAll(received);

				}
				System.out.println(this.name + ": " + received);
			} catch (IOException e) {
				System.out.println(this.name + " disconnected!");
				sendToAll(this.name + " disconnected!");
				updateList(this);
				break;
			}

		}
		try {
			// closing resources
			this.dis.close();
			this.dos.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void updateList(ClientHandler clientHandler) {
		Server.vc.remove(clientHandler);

	}

	public void setClient() {
		String tempName = "";
		boolean duplicate = false;
		try {
			tempName = dis.readUTF();
		} catch (IOException e) {

		}
		for (ClientHandler mc : Server.vc) {

			if (tempName.equals(mc.name)) {
				duplicate = true;
			}
		}
		if (duplicate == false) {
			this.name = tempName;
			try {
				dos.writeUTF("unique nickname selected!");
				dos.writeUTF("Your friends can whisper to you at /w " + this.name);
				dos.writeUTF("Use /help for list of functionality");
			} catch (IOException e) {

			}

		} else {
			try {
				dos.writeUTF("Name already taken, please select another!");
			} catch (IOException e) {

			}
			setClient();
		}
	}

	public void sendToAll(String received) {
		for (ClientHandler mc : Server.vc) {
			try {
				mc.dos.writeUTF(this.name + ": " + /* MsgToSend */ received);
			} catch (IOException e) {
				break;
			}
		}
	}

}
