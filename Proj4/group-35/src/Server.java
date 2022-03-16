import javafx.fxml.FXMLLoader;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
	public static final String PREFIX = "230.0.0.";
	private static final int POOL_SIZE = 25;

	private int port;
	private int totalOnline;
	private int totalOffline;

	private Socket client;
	private static ServerSocket server;

	private FXMLLoader load;

	private static ArrayList<String> listUsersOnline;
	private static ArrayList<String> listUsersOffline;
	private static ArrayList<String> Users;

	private static ArrayList<ClientHandler> clients;
	private static LinkedList<AssignGroup> pool;

	public Server(int port, FXMLLoader loader) {
		this.port = port;
		this.client = null;
		this.load = loader;

		server = null;

		listUsersOnline = new ArrayList<>();
		listUsersOffline = new ArrayList<>();
		Users = new ArrayList<>();

		clients = new ArrayList<>();
		pool = new LinkedList<>();
	}

	public void start() {
		ServerGUI serverInterface = this.load.getController();
		try {
			server = new ServerSocket(this.port);
		} catch (IllegalArgumentException | IOException e) {
			System.out.println("\nCould not start server");
			serverInterface.txaLogs.appendText("Could not start server\n");
			e.printStackTrace();
			quit();
		}

		System.out.println("\nServer started");
		serverInterface.txaLogs.appendText("Server started\n");
		System.out.printf("Accepting clients at port:\n\n", this.port);
		serverInterface.txaLogs.appendText("Accepting clients at port: " + this.port + "\n");
		pool.removeAll(pool);
		for (int i = 0; i < POOL_SIZE; i++) {
			String multicastIP = PREFIX + "" + i;
			AssignGroup ag = new AssignGroup(multicastIP);
			pool.add(ag);
		}

		Thread qt = new Thread() {
			@Override
			public void run() {
				Scanner q = new Scanner(System.in);
				String readIn = q.nextLine();

				while (!readIn.equals("quit")) {
					readIn = q.nextLine();
				}

				for (ClientHandler currClient : clients) {
					currClient.close();
				}
				q.close();
				quit();
			}
		};
		qt.start();
		while (true) {
			try {
				this.client = server.accept();
				ClientHandler ch = new ClientHandler(this, this.client, this.load);
				clients.add(ch);
				ch.start();
			} catch (IOException e) {
				if (server.isClosed()) {
					break;
				}
				System.out.println("Could not accept client -- " + e);
				serverInterface.txaLogs.appendText("Could not accept client --  " + e + "\n");
			}
		}
	}

	public void quit() {
		try {
			if (this.client != null) {
				this.client.close();
			}
			if (server != null) {
				server.close();
			}
			clients.clear();
			listUsersOnline.clear();
			listUsersOffline.clear();
			Users.clear();
		} catch (IOException e) {
			System.err.println("Could not close connections -- " + e);
		}
		System.out.println("\nYou're out");
		System.exit(0);
	}

	public boolean assignUserGroup(String user, int group) {
		if (group < 0 || group >= POOL_SIZE) {
			return false;
		}

		pool.get(group).addUser(user);
		return true;
	}

	public String[] assignUserGroup(String user) {
		String[] userDetails = null;
		for (int i = 0; i < pool.size(); i++) {
			if (pool.get(i).getNumUsers() > 0) {
				continue;
			}
			pool.get(i).addUser(user);
			AssignGroup cg = pool.get(i);
			userDetails = new String[] { cg.getIP(), "" + cg.getPort() };
			break;
		}
		return userDetails;
	}

	void deleteUserFromGroup(String user, int group) {
		if (group >= 0 && group < POOL_SIZE) {
			pool.get(group).removeUser(user);
			return;
		}
		for (int i = 0; i < pool.size(); i++) {
			if (pool.get(i).getNumUsers() < 1) {
				continue;
			}
			if (pool.get(i).getUsers().contains(user)) {
				pool.get(i).removeUser(user);
			}
		}
	}

	public void deleteUserFromGroup(String user) {
		deleteUserFromGroup(user, -1);
	}

	public String getGroupUsers(int group) {
		if (group < 0 || group >= POOL_SIZE) {
			return null;
		}
		HashSet<String> groupUsers = pool.get(group).getUsers();
		StringBuilder sb = new StringBuilder();
		for (String user : groupUsers) {
			sb.append(user + "#");
		}
		String us = sb.toString();
		String userStr = us.substring(0, us.length() - 1);
		return userStr;
	}

	public boolean checkUserOnline(String username) {
		boolean valid = false;
		synchronized (listUsersOnline) {
			valid = listUsersOnline.contains(username);
		}
		return valid;
	}

	public boolean checkUserConnected(String username) {
		boolean valid = false;
		synchronized (Users) {
			valid = Users.contains(username);
		}
		return valid;
	}

	public void addUser(String username) {
		synchronized (listUsersOnline) {
			if (!listUsersOnline.contains(username)) {
				listUsersOnline.add(username);
				this.totalOnline = listUsersOnline.size();
			}
		}
		synchronized (listUsersOffline) {
			if (listUsersOffline.contains(username)) {
				listUsersOffline.remove(username);
				this.totalOffline = listUsersOffline.size();
			}
		}
		synchronized (Users) {
			if (!Users.contains(username)) {
				Users.add(username);
			}
		}
	}

	public void addUser(ClientHandler user) {
		addUser(user.getUsername());
	}

	public void removeUser(ClientHandler user) {
		String username = user.getUsername();
		synchronized (listUsersOnline) {
			if (listUsersOnline.contains(username)) {
				listUsersOnline.remove(username);
				this.totalOnline = listUsersOnline.size();
			}
		}
		synchronized (listUsersOffline) {
			if (!listUsersOffline.contains(username)) {
				listUsersOffline.add(username);
				this.totalOffline = listUsersOffline.size();
			}
		}
		synchronized (clients) {
			clients.remove(user);
		}
	}

	public int getNumOnline() {
		return this.totalOnline;
	}

	public int getNumOffline() {
		return this.totalOffline;
	}

	public int getTotalUserCount() {
		return this.totalOnline + this.totalOffline;
	}

	public ArrayList<String> getOnlineUsers() {
		return listUsersOnline;
	}

	public ArrayList<String> getOfflineUsers() {
		return listUsersOffline;
	}

	public ArrayList<String> getAllUsers() {
		return Users;
	}

	public ArrayList<ClientHandler> getClients() {
		return clients;
	}
}
