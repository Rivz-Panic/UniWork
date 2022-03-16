import java.util.*;

public class AssignGroup {

	private String IP;
	private int port;
	private int numUsers;

	private HashSet<String> users;

	public AssignGroup(String IP, int port) {
		this.IP = IP;
		this.port = port;

		this.numUsers = 0;
		this.users = new HashSet<>();
	}

	public AssignGroup(String IP) {
		this(IP, 8001);
	}

	public void addUser(String user) {
		this.users.add(user);
		this.numUsers = this.users.size();
	}

	public void removeUser(String user) {
		this.users.remove(user);
		this.numUsers = this.users.size();
	}

	public String getIP() {
		return this.IP;
	}

	public int getPort() {
		return this.port;
	}

	public int getNumUsers() {
		return this.numUsers;
	}

	public HashSet<String> getUsers() {
		return this.users;
	}
}
