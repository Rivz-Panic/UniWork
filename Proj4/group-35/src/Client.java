import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javafx.application.*;
import javafx.scene.control.*;

public class Client {

	private static final int PACKET_SIZE = 1024;
	private static final String PREFIX = "230.0.0.";
	private static boolean recording;
	private static boolean inCall;

	private static AudioFormat audioFormat;
	private static AudioInputStream ais;

	private static Clip clip;
	private static SourceDataLine sourceLine;
	private static TargetDataLine targetLine;

	private int port;
	private int multicastPort;

	private String hostname;
	private String username;
	private static final String EXT = " (You)";

	private String multicastIP;
	private int groupNumber = -1;

	private Socket client;
	private MulticastSocket mcs;
	private DatagramSocket dgs;

	private DataInputStream dis;
	private DataOutputStream dos;

	private ByteArrayOutputStream baos;
	private ByteArrayOutputStream vnRecv;

	private Label lblVNRecv;

	private TextArea txaGlobal;
	private TextArea txaWhisper;
	private TextArea txaVN;
	private TextArea txaGroupMessages;

	private ListView<String> lstOnline;
	private ListView<String> lstOffline;
	private ListView<String> lstInCall;
	private ComboBox<String> cmbWhisperTo;
	private ComboBox<String> cmbVoicenoteTo;

	public Client(String host, int port) {
		this.hostname = host;
		this.port = port;
		this.username = "";
		this.baos = null;
		this.vnRecv = null;
		recording = false;
		inCall = false;
		ais = null;
		clip = null;
		sourceLine = null;
		targetLine = null;
		this.client = null;
		this.mcs = null;
		this.dgs = null;
		this.dis = null;
		this.dos = null;
		float sampleRate = 16000F;
		int sampleSize = 16;
		audioFormat = setupAudio(sampleRate, sampleSize);
	}

	public boolean setup() {
		try {
			this.client = new Socket(this.hostname, this.port);
		} catch (IOException e) {
			System.err.println("Error connecting client : " + e);
			return false;
		}
		try {
			this.dgs = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println("Error creating DatagramSocket : " + e);
			return false;
		}
		try {
			this.dis = new DataInputStream(this.client.getInputStream());
			this.dos = new DataOutputStream(this.client.getOutputStream());
		} catch (IOException e) {
			System.err.println("Error getting data streams : " + e);
			return false;
		}
		return true;
	}

	public AudioFormat getAudioFormat() {
		return audioFormat;
	}

	public void updateUserList() {
		try {
			this.dos.writeUTF("users#" + this.username);
		} catch (IOException e) {
			System.err.println("Error sending syn for users - " + e);
			return;
		}
		String respOn = "";
		String respOff = "";
		try {
			respOn = this.dis.readUTF().trim();
			respOff = this.dis.readUTF().trim();
		} catch (IOException e) {
			System.err.println("Error reading server response - " + e);
		}
		if (!respOn.startsWith("usersOn#") || !respOff.startsWith("usersOff#")) {
			System.out.println("Invalid response from server");
			return;
		}
		int idxOn = respOn.indexOf("#");
		int idxOff = respOff.indexOf("#");
		String onUsers = respOn.substring(idxOn + 1, respOn.length());
		String offUsers = respOff.substring(idxOff + 1, respOff.length());
		if (!onUsers.contains("#") && !offUsers.contains("#")) {
			return;
		}
		if (onUsers.contains("#")) {
			String[] on = onUsers.split("#");
			sortUsernames(on);
			this.lstOnline.getItems().clear();
			this.lstOnline.getItems().addAll(on);

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					cmbWhisperTo.getItems().clear();
					cmbWhisperTo.getItems().addAll(on);

					cmbVoicenoteTo.getItems().clear();
					cmbVoicenoteTo.getItems().addAll(on);
				}
			});
		}
		if (offUsers.contains("#")) {
			String[] off = offUsers.split("#");
			sortUsernames(off);
			this.lstOffline.getItems().clear();
			this.lstOffline.getItems().addAll(off);
		}
	}

	public boolean login(String user) {
		user = user.trim();

		if (user.length() < 1) {
			return false;
		}

		try {
			this.dos.writeUTF("login#" + user);
		} catch (IOException e) {
			System.err.println("Failed sending login packet - " + e);
			return false;
		}
		String packet = "";
		try {
			packet = this.dis.readUTF().trim();
		} catch (IOException e) {
			System.err.println("Error reading packet : " + e);
			return false;
		}
		if (!packet.equals("login#success")) {
			return false;
		}
		this.username = user;
		return true;
	}

	public void logout(String user) {
		if (user.length() < 1) {
			return;
		}
		String command = "logout#" + user;
		try {
			this.dos.writeUTF(command);
		} catch (IOException e) {
			System.out.println("\nServer is dead");
		}
		this.username = "";
		quit();
	}

	public void send(String message) {

		if (message.equals("quit")) {
			logout(this.username);
			return;
		}

		try {
			this.dos.writeUTF("msg#" + message);
		} catch (Exception e) {
			System.err.println("Server shutdown");
			this.txaGlobal.appendText("[ message not sent ]");
		}
	}

	public void sendToGroup(String message, int group) {
		if (message.length() < 1) {
			return;
		}

		if (group < 0 || group > 254) {
			this.txaGroupMessages.appendText("\nNot part of any group\n");
			return;
		}

		String gmsg = "gmsg#" + message + "#" + group;
		try {
			this.dos.writeUTF(gmsg);
		} catch (IOException e) {
			System.err.println("Server is dead");
		}
	}

	public void whisper(String toUser, String message) {
		if (message.equals("quit")) {
			logout(this.username);
			return;
		}

		try {
			this.dos.writeUTF("whsp#" + toUser + "#" + message);
		} catch (Exception e) {
			System.err.println("Server shutdown.");
			this.txaGlobal.appendText("[ message not sent ]");
		}
	}

	public void voicenote(String user) {
		if (user.equals(this.username)) {
			this.txaVN.appendText("\nCannot send voicenote to yourself\n");
			return;
		}

		if (this.baos == null) {
			this.txaVN.appendText("\nPlease record voicenote to send\n");
			return;
		}

		String msg = "vn#" + this.username + "#" + user;
		byte[] data = this.baos.toByteArray();

		try {
			this.dos.writeUTF(msg);
			this.dos.writeInt(data.length);
			this.dos.write(this.baos.toByteArray());
		} catch (IOException e) {
			System.err.println("Error writing voicenote to server - " + e);
			this.txaVN.appendText("\nNo voicenote sent\n");
			return;
		}

		this.txaVN.appendText("\nSent voicenote to '" + user + "' [" + data.length + " bytes] :)\n");
	}

	public void call(String user) {
		if (inCall) {
			this.txaGlobal.appendText("\nAlready in call\n");
			return;
		}

		if (user.equals(this.username)) {
			this.txaGlobal.appendText("\nCannot call yourself\n");
			return;
		}

		String data = "call#" + this.username + "#" + user;
		try {
			this.dos.writeUTF(data);
		} catch (IOException e) {
			System.err.println("Error writing to server - " + e);
			return;
		}
	}

	public void invite(String user) {
		if (this.username.equals(user)) {
			return;
		}

		String inv = "invite#" + this.username + "#" + user + "#" + this.groupNumber;
		try {
			this.dos.writeUTF(inv);
		} catch (IOException e) {
			System.err.println("Failed writing to output stream - " + e);
		}
	}

	public void joinGroupNum(String user, int group) {
		if (group < 0 || group > 254) {
			return;
		}

		String join = "join#" + user + "#" + group;
		try {
			this.dos.writeUTF(join);
		} catch (IOException e) {
			System.err.println("Failed sending group join - " + e);
		}
	}

	public void leaveGroup(String user, int gNum) {
		if (gNum < 0 || gNum > 254 || this.groupNumber == -1) {
			this.groupNumber = -1;
			return;
		}

		String leave = "leave#" + user + "#" + gNum;
		try {
			this.dos.writeUTF(leave);
		} catch (IOException e) {
			System.err.println("Error leaving group - " + e);
		}

		this.txaGroupMessages.appendText("\nLeft group " + this.groupNumber + "!\n");
		this.groupNumber = -1;
	}

	public void startListeners(TextArea txaGlobal, TextArea txaWhisper, TextArea txaVN, ListView<String> lstOnline,
			ListView<String> lstOffline, ComboBox<String> cmbWhisperTo, ComboBox<String> cmbVoicenoteTo,
			Label lblVNRecv, TextArea txaGroupMessages, ListView<String> lstInCall) {

		this.txaGlobal = txaGlobal;
		this.txaWhisper = txaWhisper;
		this.txaVN = txaVN;
		this.lstOnline = lstOnline;
		this.lstOffline = lstOffline;
		this.cmbWhisperTo = cmbWhisperTo;
		this.cmbVoicenoteTo = cmbVoicenoteTo;
		this.lblVNRecv = lblVNRecv;
		this.txaGroupMessages = txaGroupMessages;
		this.lstInCall = lstInCall;

		updateUserList();

		Thread messages = new Thread() {
			@Override
			public void run() {
				listenMessages();
			}
		};
		messages.start();
	}

	public AudioFormat setupAudio(float sampleRate, int sampleSize) {
		int channels = 1; // mono audio
		boolean signed = true;
		boolean bigEndian = false;

		return new AudioFormat(sampleRate, sampleSize, channels, signed, bigEndian);
	}

	private void updateLocalStatus(String body, boolean online) {
		this.txaGlobal.appendText("\n--> " + body + "\n");

		String user = body.split(" ")[0];

		if (online) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					List<String> users = lstOnline.getItems();
					users.add(user);
					String[] arrUsers = users.toArray(new String[0]);
					sortUsernames(arrUsers, username);

					lstOnline.getItems().clear();
					lstOnline.getItems().addAll(arrUsers);

					lstOffline.getItems().remove(user);

					cmbWhisperTo.getItems().clear();
					cmbWhisperTo.getItems().addAll(arrUsers);
					cmbWhisperTo.getItems().remove(username + " (You)");

					cmbVoicenoteTo.getItems().clear();
					cmbVoicenoteTo.getItems().addAll(arrUsers);
					cmbVoicenoteTo.getItems().remove(username + " (You)");
				}
			});
		} else {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					List<String> users = lstOffline.getItems();
					users.add(user);
					String[] arrUsers = users.toArray(new String[0]);
					sortUsernames(arrUsers, username);

					lstOnline.getItems().remove(user);

					lstOffline.getItems().clear();
					lstOffline.getItems().addAll(arrUsers);

					cmbWhisperTo.getItems().remove(user);
					cmbVoicenoteTo.getItems().remove(user);
				}
			});
		}

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			System.err.println("Error thread sleeping - " + e);
		}
	}

	public void receiveVoiceNote(String body) {
		int vnLen = 0;
		byte[] data = null;

		if (this.vnRecv != null) {
			this.vnRecv.reset();
		}

		try {
			vnLen = this.dis.readInt();
			data = new byte[vnLen];
			this.dis.read(data);
		} catch (IOException e) {
			System.err.println("Error reading data - " + e);
		}

		int idx = body.indexOf("#");
		String from = body.substring(0, idx);

		this.txaVN.appendText("\nReceived voicenote from '" + from + "' [" + data.length + " bytes]\n");

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				lblVNRecv.setText(from);
			}
		});

		this.vnRecv = new ByteArrayOutputStream(data.length);
		this.vnRecv.write(data, 0, data.length);
	}

	private void updateCallStatus(String body) {
		int idx = body.indexOf("#");

		String resp = body.substring(0, idx);
		String toUser = body.substring(idx + 1);

		if (!resp.equals("success")) {
			this.txaGroupMessages.appendText("\nCannot setup call now\n");
			return;
		}

		try {
			multicastIP = this.dis.readUTF();
			multicastPort = this.dis.readInt();
		} catch (IOException e) {
			System.err.println("Error getting port and IP - " + e);
			return;
		}

		this.groupNumber = Integer.parseInt("" + multicastIP.charAt(multicastIP.length() - 1));
		inCall = true;
		invite(toUser);
		joinGroupNum(this.username, this.groupNumber);

		this.txaGroupMessages.appendText("\nCall started successfully \nAssigned IP " + multicastIP + " and port "
				+ multicastPort + " with group " + this.groupNumber + "\n");

		DataLine.Info dlInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

		try {
			targetLine = (TargetDataLine) AudioSystem.getLine(dlInfo);
			targetLine.open(audioFormat);
		} catch (LineUnavailableException e) {
			System.err.println("Failed to get target line for call - " + e);
			return;
		}

		targetLine.flush();
		targetLine.start();

		try {
			this.mcs = new MulticastSocket(multicastPort);
			InetAddress group = InetAddress.getByName(multicastIP);
			this.mcs.joinGroup(group);
		} catch (IOException e) {
			System.err.println("Failed to establish multicast socket : " + e);
			return;
		}

		Thread c = new Thread() {
			@Override
			public void run() {
				captureUserAudio();
			}
		};
		c.start();

		Thread d = new Thread() {
			@Override
			public void run() {
				listenVoice();
			}
		};
		d.start();
	}

	private void receiveInvite(String body) {
		int idx = body.indexOf("#");

		String hostUser = body.substring(0, idx);
		int groupNum = Integer.parseInt(body.substring(idx + 1));

		Object[] choices = { "Accept", "Decline" };
		Object defaultChoice = choices[0];

		int option = JOptionPane.showOptionDialog(null, "Incoming call from " + hostUser, "Incoming call",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, choices, defaultChoice);

		if (option != 0) {
			return;
		}

		if (mcs != null) {
			this.txaGroupMessages.appendText("\nAlready in a call. Please leave before joining another\n");
			return;
		}

		DataLine.Info dlInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

		try {
			targetLine = (TargetDataLine) AudioSystem.getLine(dlInfo);
			targetLine.open(audioFormat);
		} catch (LineUnavailableException e) {
			System.err.println("Unable to get target line for call : " + e);
			return;
		}

		targetLine.flush();
		targetLine.start();

		multicastIP = PREFIX + groupNum;
		multicastPort = 8001;
		this.groupNumber = groupNum;

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				lstInCall.getItems().add(hostUser);
				txaGroupMessages.appendText("\nAccepted invite! In group " + groupNum + "\n");
			}
		});

		try {
			this.mcs = new MulticastSocket(multicastPort);
			InetAddress group = InetAddress.getByName(multicastIP);
			this.mcs.joinGroup(group);
		} catch (IOException e) {
			System.err.println("Failed to establish multicast socket - " + e);
			return;
		}

		Thread c = new Thread() {
			@Override
			public void run() {
				captureUserAudio();
			}
		};
		c.start();

		Thread d = new Thread() {
			@Override
			public void run() {
				listenVoice();
			}
		};
		d.start();
	}

	private void receiveJoin(String body) {
		int idx = body.indexOf("#");

		int gNum = Integer.parseInt(body.substring(0, idx));
		String resp = body.substring(idx + 1);

		if (!resp.equals("success")) {
			this.groupNumber = -1;
			return;
		}

		this.txaGroupMessages.appendText("\nSuccessfully joined group " + gNum + "\n");
		this.groupNumber = gNum;
	}

	private void close() {
		try {
			if (this.client != null) {
				this.client.close();
			}
			if (this.dis != null) {
				this.dis.close();
			}
			if (this.dos != null) {
				this.dos.close();
			}
			if (this.mcs != null) {
				this.mcs.close();
			}
			if (this.dgs != null) {
				this.dgs.close();
			}
			if (this.baos != null) {
				this.baos.reset();
				this.baos.close();
			}

			if (ais != null) {
				ais.close();
			}
			if (clip != null) {
				clip.close();
			}
			if (sourceLine != null) {
				sourceLine.close();
			}
			if (targetLine != null) {
				targetLine.close();
			}
		} catch (IOException e) {
			System.err.println("Error closing connections - " + e);
		}
	}

	private void quit() {
		System.out.println("\nExiting");

		close();
		System.exit(0);
	}

	public void recordVoiceNote() {
		DataLine.Info dlInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

		try {
			targetLine = (TargetDataLine) AudioSystem.getLine(dlInfo);
			targetLine.open(audioFormat);
		} catch (LineUnavailableException e) {
			System.err.println("Unable to obtain target data line : " + e);
		}

		targetLine.flush();
		targetLine.start();

		Thread rt = new Thread() {
			@Override
			public void run() {
				recAudio();
			}
		};
		rt.start();
	}

	public void playVoiceNote(ByteArrayOutputStream vn) {
		DataLine.Info dlInfo = new DataLine.Info(Clip.class, audioFormat);

		byte[] bufVN = vn.toByteArray();
		int aisLen = (int) Math.ceil((double) bufVN.length / (double) audioFormat.getFrameSize());

		InputStream stream = new ByteArrayInputStream(bufVN);
		ais = new AudioInputStream(stream, audioFormat, aisLen);

		try {
			clip = (Clip) AudioSystem.getLine(dlInfo);
			clip.open(ais);
		} catch (LineUnavailableException | IOException e) {
			System.err.println("Unable to obtain or open clip : " + e);
			return;
		}

		clip.flush();
		clip.start();

		Thread pt = new Thread() {
			@Override
			public void run() {
				playClip();
			}
		};
		pt.start();
	}

	public void playVoiceNote() {
		if (this.baos == null) {
			return;
		}

		playVoiceNote(this.baos);
	}

	public ByteArrayOutputStream getVnRecv() {
		return this.vnRecv;
	}

	private void captureUserAudio() {
		byte[] data = new byte[PACKET_SIZE];
		short[] half = new short[data.length / 2];
		double rmsq = 0;
		int read = 0;

		inCall = true;
		try {
			InetAddress group = InetAddress.getByName(multicastIP);

			while (inCall) {
				read = targetLine.read(data, 0, data.length);
				rmsq = 0;

				ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(half);

				for (int i = 0; i < half.length; i++) {
					float normal = half[i] / 32768f;
					rmsq += normal * normal;
				}

				rmsq = Math.sqrt(rmsq / ((double) half.length));
				if (rmsq < 0.125) {
					continue;
				}

				if (read > 0) {
					byte[] toSend = Arrays.copyOfRange(data, 0, read);

					DatagramPacket dgp = new DatagramPacket(toSend, toSend.length, group, multicastPort);
					this.dgs.send(dgp);
				}
			}
		} catch (Exception e) {
			System.out.println("Error in listenCall method : " + e);
		}
	}

	private void listenVoice() {
		byte[] bufCall = null;
		DatagramPacket dgp = null;

		DataLine.Info dlInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
		try {
			sourceLine = (SourceDataLine) AudioSystem.getLine(dlInfo);
			sourceLine.open(audioFormat);
			sourceLine.start();

			while (inCall) {
				bufCall = new byte[PACKET_SIZE];

				dgp = new DatagramPacket(bufCall, bufCall.length);
				this.mcs.receive(dgp);

				sourceLine.write(bufCall, 0, bufCall.length);
			}
		} catch (LineUnavailableException | IOException e) {
		}
	}

	private void listenMessages() {
		this.lstOnline.getItems().add(this.username + " (You)");

		List<String> users = this.lstOnline.getItems();
		String[] arrUsers = users.toArray(new String[0]);
		sortUsernames(arrUsers, this.username);

		this.lstOnline.getItems().clear();
		this.lstOnline.getItems().addAll(arrUsers);

		String msg = "";

		while (true) {
			try {
				msg = this.dis.readUTF();

				if (!msg.contains("#")) {
					continue;
				}

				int idx = msg.indexOf("#");
				String header = msg.substring(0, idx);
				String body = msg.substring(idx + 1).trim();

				if (header.equals("login") || header.equals("logout")) {
					continue;
				}

				switch (header) {
				case "msg":
					this.txaGlobal.appendText("\n" + body + "\n");
					break;
				case "gmsg":
					this.txaGroupMessages.appendText("\n" + body + "\n");
					break;
				case "whsp":
					this.txaWhisper.appendText("\n" + body + "\n");
					break;
				case "online":
					updateLocalStatus(body, true);
					break;
				case "offline":
					updateLocalStatus(body, false);
					break;
				case "vn":
					receiveVoiceNote(body);
				case "call":
					updateCallStatus(body);
					break;
				case "invite":
					receiveInvite(body);
					break;
				case "join":
					receiveJoin(body);
					break;
				}
			} catch (IOException e) {
				break;
			}
		}

		close();
	}

	private void recAudio() {
		byte[] data = new byte[PACKET_SIZE];
		int read = 0;

		if (this.baos != null) {
			this.baos.reset();
		}

		this.baos = new ByteArrayOutputStream();
		recording = true;

		while (recording) {
			read = targetLine.read(data, 0, data.length);

			if (read > 0) {
				this.baos.write(data, 0, read);
			}
		}

		try {
			targetLine.drain();
			this.baos.close();
		} catch (IOException e) {
			System.err.println("Cannot close byte array output stream : " + e);
		}
	}

	public void stopRec() {
		recording = false;
	}

	public void leaveCall() {
		if (!inCall) {
			return;
		}

		inCall = false;
		System.out.println("leaving group");
		leaveGroup(this.username, this.groupNumber);

		this.groupNumber = -1;
		if (this.mcs == null) {
			return;
		}

		try {
			InetAddress group = InetAddress.getByName(multicastIP);
			this.mcs.leaveGroup(group);
		} catch (IOException e) {
			System.err.println("Exceptions galore : " + e);
		}

		mcs = null;
	}

	public void playClip(Clip toPlay) {
		if (toPlay.isRunning()) {
			toPlay.stop();
		}

		toPlay.setFramePosition(0);
		toPlay.start();
	}

	public void playClip() {
		if (clip.isRunning()) {
			clip.stop();
		}

		clip.setFramePosition(0);
		clip.start();
	}

	public void resumeClip(Clip toResume) {
		toResume.start();
	}

	public void resumeClip() {
		clip.start();
	}

	public void pauseClip(Clip toPause) {
		toPause.stop();
	}

	public void pauseClip() {
		clip.stop();
	}

	public void stopClip(Clip toStop) {
		pauseClip(toStop);

		toStop.flush();
		toStop.setFramePosition(0);
	}

	public void stopClip() {
		pauseClip();

		clip.flush();
		clip.setFramePosition(0);
	}

	public int getGroupNumber() {
		synchronized (this) {
			return this.groupNumber;
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
