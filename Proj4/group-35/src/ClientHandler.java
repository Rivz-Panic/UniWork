import javafx.application.*;
import javafx.fxml.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler extends Thread {
	private static Server server;
	private static Command command;

	private String username;
	private int groupNum = -1;
	private static final String EXT = " (You)";

	private FXMLLoader loader;

	private Socket client;
	private DataInputStream dis;
	private DataOutputStream dos;

	public ClientHandler(Server s, Socket c, FXMLLoader loader) {
		server = s;
		this.client = c;
		this.loader = loader;

		this.username = "";

		command = Command.INVALID;
		try {
			this.dis = new DataInputStream(this.client.getInputStream());
			this.dos = new DataOutputStream(this.client.getOutputStream());
		} catch (IOException e) {
			System.err.println("Failed to get data streams from client -- " + e);
		}
	}

	@Override
	public void run() {
		ServerGUI controller = this.loader.getController();
		while (true) {
			String recv = "";
			try {
				recv = this.dis.readUTF();
				if (!recv.contains("#")) {
					continue;
				}
				int idx = recv.indexOf("#");
				String header = recv.substring(0, idx);
				String body = recv.substring(idx + 1).trim();
				command = command.getValue(header);
				if (command == Command.INVALID) {
					controller.txaLogs.appendText("\nUser sent invalid command!\n");
					continue;
				}
				if (command == Command.LOGOUT) {
					logout(body);
					break;
				}
				switch (command) {
				case USERS:
					users(body);
				case LOGIN:
					login(body);
					break;
				case MSG:
					message(body);
					break;
				case GMSG:
					groupMessage(body);
					break;
				case WHSP:
					whisper(body);
					break;
				case VN:
					voicenote(body);
					break;
				case CALL:
					call(body);
					break;
				case INVITE:
					invite(body);
					break;
				case JOIN:
					joinGroup(body);
					break;
				case LEAVE:
					leaveGroup(body);
					break;
				default:
					break;
				}
			} catch (IOException e) {
				if (this.username.length() < 1) {
					break;
				}
				logout(this.username);
				break;
			}
		}
		close();
	}

	private void users(String user) {
		ArrayList<String> onlineUsers = server.getOnlineUsers();
		ArrayList<String> offlineUsers = server.getOfflineUsers();
		StringBuilder sb = new StringBuilder();
		for (String username : onlineUsers) {
			if (!username.equals(user)) {
				sb.append(username + "#");
			}
		}
		String online = "usersOn#" + sb.toString();
		sb = new StringBuilder();
		for (String username : offlineUsers) {
			sb.append(username + "#");
		}
		String offline = "usersOff#" + sb.toString();
		try {
			this.dos.writeUTF(online);
			this.dos.writeUTF(offline);
		} catch (IOException e) {
			System.err.println("Error writing user lists to output :: " + e);
		}
	}

	private void login(String user) {
		ServerGUI controller = this.loader.getController();
		if (server.checkUserOnline(user)) {
			try {
				this.dos.writeUTF("login#failure");
			} catch (IOException e) {
				System.err.println("Error sending login failure :: " + e);
			}
			return;
		}
		try {
			this.dos.writeUTF("login#success");
		} catch (IOException e) {
			System.err.println("Error sending login success :: " + e);
		}
		server.addUser(user);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				List<String> users = controller.lstOnline.getItems();
				users.add(user);
				String[] arrUsers = users.toArray(new String[0]);
				sortUsernames(arrUsers);
				controller.lstOnline.getItems().clear();
				controller.lstOnline.getItems().addAll(arrUsers);
				controller.lstOffline.getItems().remove(user);
			}
		});
		controller.txaLogs.appendText("\nWelcome  " + user + "\n");
		controller.txaLogs
				.appendText(server.getNumOnline() + " / " + server.getTotalUserCount() + "online users\n");
		this.username = user;
		ArrayList<ClientHandler> clients = server.getClients();
		String msg = "online#" + user + " is here";
		for (ClientHandler ch : clients) {
			if (user.equals(ch.getUsername())) {
				continue;
			}
			ch.sendStatus(msg);
		}
	}

	private void logout(String user) {
		ServerGUI controller = this.loader.getController();
		server.removeUser(this);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				List<String> users = controller.lstOffline.getItems();
				users.add(user);
				String[] arrUsers = users.toArray(new String[0]);
				sortUsernames(arrUsers);
				controller.lstOnline.getItems().remove(user);
				controller.lstOffline.getItems().clear();
				controller.lstOffline.getItems().addAll(arrUsers);
			}
		});
		controller.txaLogs.appendText("\n  " + user + " disconnected.\n");
		controller.txaLogs
				.appendText(server.getNumOnline() + " / " + server.getTotalUserCount() + " online users\n");

		this.username = "";
		ArrayList<ClientHandler> clients = server.getClients();
		String msg = "offline#" + user + " disconnected :(";
		for (ClientHandler ch : clients) {
			if (user.equals(ch.getUsername())) {
				continue;
			}
			ch.sendStatus(msg);
		}
	}

	private void message(String body) {
		ServerGUI controller = this.loader.getController();
		controller.txaLogs.appendText("\n" + this.username + " : " + body + "\n");
		ArrayList<ClientHandler> clients = server.getClients();
		String message = "msg#" + this.username + " : " + body;
		for (ClientHandler ch : clients) {
			if (this.username.equals(ch.getUsername())) {
				continue;
			}
			ch.sendStatus(message);
		}
	}

	private void groupMessage(String body) {
		ServerGUI controller = this.loader.getController();
		int idx = body.indexOf("#");
		String message = body.substring(0, idx);
		int gNum = Integer.parseInt(body.substring(idx + 1));
		controller.txaLogs.appendText("\n[Group " + gNum + "] " + this.username + " : " + message + "\n");
		ArrayList<ClientHandler> clients = server.getClients();
		String toSend = "gmsg#" + this.username + " : " + message;
		for (ClientHandler ch : clients) {
			if (gNum != ch.getGroupNum() || this.username.equals(ch.getUsername())) {
				continue;
			}
			ch.sendStatus(toSend);
		}
	}

	private void whisper(String body) {
		ServerGUI controller = this.loader.getController();
		int idx = body.indexOf("#");

		if (idx < 0) {
			controller.txaLogs.appendText("\nWhisper not sent, please select a user\n");
			return;
		}
		String toUser = body.substring(0, idx);
		String message = body.substring(idx + 1);
		if (this.username.equals(toUser)) {
			controller.txaLogs.appendText("\n" + this.username + " tried to whisper to themselves\n");
			return;
		}
		if (!server.checkUserOnline(toUser)) {
			controller.txaLogs.appendText(
					"\nSending whisper, " + this.username + " -> " + toUser + ", unsuccessful - user offline\n");
			return;
		}
		controller.txaLogs.appendText("\n" + this.username + " -> " + toUser + " : " + message + "\n");
		ArrayList<ClientHandler> clients = server.getClients();
		String whisp = "whsp#" + this.username + " : " + message;
		for (ClientHandler ch : clients) {
			if (!toUser.equals(ch.getUsername())) {
				continue;
			}
			ch.sendStatus(whisp);
		}
	}

	private void voicenote(String body) {
		ServerGUI controller = this.loader.getController();
		int dataLength = -1;
		try {
			dataLength = this.dis.readInt();
		} catch (IOException e) {
			System.err.println("Error getting data length :: " + e);
			controller.txaLogs.appendText("\nVoicenote discarded - corrupted\n");
			return;
		}
		if (dataLength < 0) {
			controller.txaLogs.appendText("\nVoicenote discarded - corrupted\n");
			return;
		}
		byte[] vnData = new byte[dataLength];
		try {
			this.dis.read(vnData);
		} catch (IOException e) {
			System.err.println("Could not get voicenote data - " + e);
		}
		int idx = body.indexOf("#");
		String fromUser = body.substring(0, idx);
		String toUser = body.substring(idx + 1);
		if (this.username.equals(toUser)) {
			controller.txaLogs.appendText("\n" + toUser + " tried to send a voicenote to themself\n");
			return;
		}
		if (!server.checkUserOnline(toUser)) {
			controller.txaLogs.appendText(
					"\nSending voicenote, " + fromUser + " -> " + toUser + ", unsuccessful - user offline.\n");
			return;
		}
		controller.txaLogs.appendText(
				"\n" + fromUser + " sent a voicenote to " + toUser + " of length " + dataLength + " bytes\n");
		ArrayList<ClientHandler> clients = server.getClients();
		String vn = "vn#" + fromUser + "#" + toUser;
		for (ClientHandler ch : clients) {
			if (!toUser.equals(ch.getUsername())) {
				continue;
			}
			ch.sendVoicenote(vn, vnData);
		}
	}

	private void call(String user) {
		int idx = user.indexOf("#");

		String username = user.substring(0, idx);
		String toCall = user.substring(idx + 1);
		String[] details = server.assignUserGroup(username);
		if (details == null) {
			try {
				this.dos.writeUTF("call#failure#" + toCall);
			} catch (IOException e) {
				System.err.println("Error writing to output - " + e);
			}

			return;
		}
		String mcIP = details[0];
		int mcPort = Integer.parseInt(details[1]);
		try {
			this.dos.writeUTF("call#success#" + toCall);
			this.dos.writeUTF(mcIP);
			this.dos.writeInt(mcPort);
		} catch (IOException e) {
			System.err.println("Failed writing IP and port to client - " + e);
			return;
		}
	}

	private void invite(String body) {
		String[] details = body.split("#");

		String hostUser = details[0];
		String toUser = details[1];
		int groupNum = Integer.parseInt(details[2]);
		server.assignUserGroup(toUser, groupNum);
		ArrayList<ClientHandler> clients = server.getClients();
		String inv = "invite#" + hostUser + "#" + groupNum;
		for (ClientHandler ch : clients) {
			if (!toUser.equals(ch.getUsername())) {
				continue;
			}
			ch.setGroupNum(groupNum);
			ch.sendStatus(inv);
		}
	}

	private void joinGroup(String body) {
		int idx = body.indexOf("#");
		String user = body.substring(0, idx);
		int group = Integer.parseInt(body.substring(idx + 1));

		String join = "join#" + group + "#";
		if (group < 0 || group > 254 || this.groupNum != -1) {
			join += "failure";
		} else {
			join += "success";
			this.groupNum = group;
			server.assignUserGroup(user, group);
		}
		try {
			this.dos.writeUTF(join);
		} catch (IOException e) {
			System.err.println("Failed writing join response - " + e);
		}
	}

	private void leaveGroup(String body) {
		int idx = body.indexOf("#");

		String user = body.substring(0, idx);
		int group = Integer.parseInt(body.substring(idx + 1));

		server.deleteUserFromGroup(user, group);
		this.groupNum = -1;
	}

	public void close() {
		try {
			if (client != null) {
				client.close();
			}

			if (this.dis != null) {
				this.dis.close();
			}

			if (this.dos != null) {
				this.dos.close();
			}
		} catch (IOException e) {
			System.err.println("Failed to close connections - " + e);
		}
	}

	public void sendStatus(String msg) {
		if (this.username.length() < 1) {
			return;
		}
		try {
			this.dos.writeUTF(msg);
		} catch (IOException e) {
			if (this.username.length() < 1) {
				return;
			}
			System.err.println("Could not send status to '" + this.username + "' - " + e);
		}
	}

	public void sendVoicenote(String tcpMessage, byte[] voicenote) {
		int vnLen = voicenote.length;
		try {
			this.dos.writeUTF(tcpMessage);
			this.dos.writeInt(vnLen);
			this.dos.write(voicenote);
		} catch (IOException e) {
			System.err.println("Failed to send voicenote to client - " + e);
			return;
		}

	}

	public String getUsername() {
		return this.username;
	}

	public void setGroupNum(int groupNum) {
		this.groupNum = groupNum;
	}

	public int getGroupNum() {
		return this.groupNum;
	}

	public enum Command {
		USERS("users"), LOGIN("login"), LOGOUT("logout"), MSG("msg"), GMSG("gmsg"), WHSP("whsp"), CALL("call"),
		VN("vn"), INVITE("invite"), JOIN("join"), LEAVE("leave"), INVALID;

		private String key;

		private Command() {
		}

		private Command(String key) {
			this.key = key;
		}

		public String getKey() {
			return this.key;
		}

		public Command getValue(String k) {
			Command ret = INVALID;
			for (Command cmd : Command.values()) {
				if (cmd.getKey().equals(k)) {
					ret = cmd;
					break;
				}
			}

			return ret;
		}
	}

	private static void stripExtension(String[] usernames) {
		for (int i = 0; i < usernames.length; i++) {
			if (!usernames[i].endsWith(EXT)) {
				continue;
			}

			int idx = usernames[i].indexOf(EXT);
			String user = usernames[i].substring(0, idx);
			usernames[i] = user;
		}
	}

	public static void sortUsernames(String[] usernames) {
		if (usernames.length == 0) {
			return;
		}
		int len = usernames.length;
		for (int i = (len / 2) - 1; i >= 0; i--) {
			heapify(usernames, len, i);
		}
		for (int i = len - 1; i >= 0; i--) {
			String temp = usernames[0];
			usernames[0] = usernames[i];
			usernames[i] = temp;

			heapify(usernames, i, 0);
		}
	}

	public static void sortUsernames(String[] usernames, String user) {
		stripExtension(usernames);
		sortUsernames(usernames);

		for (int i = 0; i < usernames.length; i++) {
			if (!usernames[i].equals(user)) {
				continue;
			}

			usernames[i] = usernames[i] + EXT;
		}
	}

	private static void heapify(String[] array, int length, int i) {
		int left = (2 * i) + 1;
		int right = (2 * i) + 2;
		int largest = i;
		if (left < length && array[left].compareToIgnoreCase(array[largest]) > 0) {
			largest = left;
		}
		if (right < length && array[right].compareToIgnoreCase(array[largest]) > 0) {
			largest = right;
		}
		if (largest != i) {
			String temp = array[i];
			array[i] = array[largest];
			array[largest] = temp;
			heapify(array, length, largest);
		}
	}
}
