import java.io.*;
import javax.sound.sampled.*;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;

public class ClientGUI {

	private static final int LIMIT = 255;

	public BorderPane bpRoot;

	public Label lblUsername;
	public Label lblRecStatus;
	public Label lblVNRecv;
	public Label lblVNStatus;

	public Button btnFriends;
	public Button btnMessages;
	public Button btnCalls;
	public Button btnQuit;
	public Button btnMessageUser;
	public Button btnVNUser;
	public Button btnCallUser;
	public Button btnVoicenotes;
	public Button btnLeaveCall;
	public Button btnGroupSend;
	public Button btnRecord;
	public Button btnRecPlay;
	public Button btnRecStop;
	public Button btnPlay;
	public Button btnStop;
	public Button btnSendVN;
	public Button btnJoinGroup;
	public Button btnLeaveGroup;

	public TabPane tpRoot;
	public TabPane tpMessages;

	public Tab tabFriends;
	public Tab tabMessages;
	public Tab tabVoicenotes;
	public Tab tabCalls;
	public Tab tabGlobal;
	public Tab tabWhispers;
	public Tab tabGroup;

	public TextField txfMessage;
	public TextField txfGroup;
	public TextField txfGroupMessage;

	public TextArea txaGlobal;
	public TextArea txaWhispers;
	public TextArea txaGroupMessages;
	public TextArea txaVoicenoteActivity;

	public Accordion accVoicenotes;
	public TitledPane atpRecording;
	public TitledPane atpPlayback;

	public ListView<String> lstOnlineUsers;
	public ListView<String> lstOfflineUsers;
	public ListView<String> lstInCall;

	public ComboBox<String> cmbWhisperTo;
	public ComboBox<String> cmbVoicenoteTo;

	private boolean recording = false;
	private boolean playingRec = false;
	private boolean isFirstPlay = true;

	private int port;

	private String host;
	private String username;
	private int groupNum;

	private AudioFormat audioFormat = null;
	private AudioInputStream ais = null;
	private ByteArrayOutputStream baos = null;
	private Clip clip = null;

	private Client client = null;

	public ClientGUI() {
		this.bpRoot = new BorderPane();

		this.lblUsername = new Label();
		this.lblRecStatus = new Label();
		this.lblVNRecv = new Label();
		this.lblVNStatus = new Label();

		this.btnFriends = new Button();
		this.btnMessages = new Button();
		this.btnCalls = new Button();
		this.btnQuit = new Button();
		this.btnMessageUser = new Button();
		this.btnVNUser = new Button();
		this.btnCallUser = new Button();
		this.btnVoicenotes = new Button();
		this.btnLeaveCall = new Button();
		this.btnGroupSend = new Button();
		this.btnRecord = new Button();
		this.btnRecPlay = new Button();
		this.btnRecStop = new Button();
		this.btnPlay = new Button();
		this.btnStop = new Button();
		this.btnSendVN = new Button();

		this.tpRoot = new TabPane();
		this.tpMessages = new TabPane();

		this.tabFriends = new Tab();
		this.tabMessages = new Tab();
		this.tabVoicenotes = new Tab();
		this.tabCalls = new Tab();
		this.tabGlobal = new Tab();
		this.tabWhispers = new Tab();
		this.tabGroup = new Tab();

		this.txfMessage = new TextField();
		this.txfGroupMessage = new TextField();

		this.txaGlobal = new TextArea();
		this.txaWhispers = new TextArea();
		this.txaGroupMessages = new TextArea();
		this.txaVoicenoteActivity = new TextArea();

		this.accVoicenotes = new Accordion();
		this.atpRecording = new TitledPane();
		this.atpPlayback = new TitledPane();

		this.lstOnlineUsers = new ListView<>();
		this.lstOfflineUsers = new ListView<>();
		this.lstInCall = new ListView<>();

		this.cmbWhisperTo = new ComboBox<>();
		this.cmbVoicenoteTo = new ComboBox<>();
	}

	public void setup(String ip, int port, String username, Client client) {
		this.host = ip;
		this.port = port;
		this.username = username;
		this.client = client;

		txfMessage.requestFocus();

		LoginGUI.window.setOnCloseRequest(event -> {
			this.client.logout(this.username);
			System.exit(0);
		});

		tabGlobal.setContent(txaGlobal);

		System.out.printf("\nConnected to %s:%d with username %s\n", this.host, this.port,
				this.username);

		this.client.startListeners(this.txaGlobal, this.txaWhispers, this.txaVoicenoteActivity, this.lstOnlineUsers,
				this.lstOfflineUsers, this.cmbWhisperTo, this.cmbVoicenoteTo, this.lblVNRecv, this.txaGroupMessages,
				this.lstInCall);

		audioFormat = this.client.getAudioFormat();
		this.groupNum = -1;
	}

	public void setName(String username) {
		lblUsername.setText(username);
		this.username = username;
	}

	public void message(String msg) {
		if (msg.equals("")) {
			return;
		}

		String fullMsg = String.format("\n%s (You) : %s\n", this.username, msg);

		txaGlobal.appendText(fullMsg);
		tabGlobal.setContent(txaGlobal);
		txfMessage.setText("");
		this.client.send(msg);
	}

	public void whisper(String toUser, String msg) {
		if (msg.equals("")) {
			return;
		}

		if (toUser == null) {
			txaWhispers.appendText("\n-- no selected user --\n");
			return;
		}

		String fullMsg = String.format("\n%s (You) : %s\n", this.username, msg);

		txaWhispers.appendText(fullMsg);
		txfMessage.setText("");
		this.client.whisper(toUser, msg);
	}

	public void send(String msg) {
		if (tabGlobal.isSelected()) {
			if (msg.equals("")) {
				return;
			}

			String fullMsg = String.format("\n%s (You) : %s\n", this.username, msg);

			txaGlobal.appendText(fullMsg);
			tabGlobal.setContent(txaGlobal);
			txfMessage.setText("");
			this.client.send(msg);
		} else if (tabWhispers.isSelected()) {
			String selected = cmbWhisperTo.getSelectionModel().getSelectedItem();
			whisper(selected, msg);
		}

		txfMessage.setText("");
		LoginGUI.window.show();
	}

	public void sendGroup(String message) {
		int gNum = this.client.getGroupNumber();

		if (message.length() < 1 || gNum < 0 || gNum > 254) {
			return;
		}

		this.groupNum = gNum;

		if (message.startsWith("/invite ")) {
			int idx = message.indexOf(" ");

			String us = message.substring(idx + 1).trim();

			if (us.length() < 1) {
				txfGroupMessage.setText("");
				return;
			}

			txaGroupMessages.appendText("\nInvited " + us + " to group " + gNum + "\n");
			txfGroupMessage.setText("");
			this.client.invite(us);
			return;
		}

		String fullMsg = String.format("\n%s (You) : %s\n", this.username, message);

		txaGroupMessages.appendText(fullMsg);
		txfGroupMessage.setText("");
		this.client.sendToGroup(message, this.groupNum);
	}

	public void startRecvVN(ByteArrayOutputStream voicenote) {
		DataLine.Info dlInfo = new DataLine.Info(Clip.class, audioFormat);

		byte[] bufVN = voicenote.toByteArray();
		int aisLen = (int) Math.ceil((double) bufVN.length / (double) audioFormat.getFrameSize());

		InputStream stream = new ByteArrayInputStream(bufVN);
		ais = new AudioInputStream(stream, audioFormat, aisLen);

		try {
			clip = (Clip) AudioSystem.getLine(dlInfo);
			clip.open(ais);
		} catch (LineUnavailableException | IOException e) {
			System.err.println("Unable to obtain or open clip :: " + e);
			return;
		}

		clip.flush();
		clip.start();

		Thread pt = new Thread() {
			@Override
			public void run() {
				client.playClip(clip);
			}
		};
		pt.start();
	}

	public void disconnect(ActionEvent event) {
		this.client.logout(this.username);
		System.exit(0);
	}

	public void checkKey(KeyEvent event) {
		String currText = txfMessage.getText();

		if (currText.length() >= LIMIT) {
			txfMessage.setText(currText.substring(0, LIMIT - 1));
			txfMessage.positionCaret(LIMIT);
		}

		if (event.getCode() == KeyCode.ENTER) {
			String message = currText;
			send(message);
		}
	}

	public void checkGroupKey(KeyEvent event) {
		String currText = txfGroupMessage.getText();

		if (currText.length() >= LIMIT) {
			txfGroupMessage.setText(currText.substring(0, LIMIT - 1));
			txfGroupMessage.positionCaret(LIMIT);
		}

		if (event.getCode() == KeyCode.ENTER) {
			String message = currText;
			sendGroup(message);
		}
	}

	public void sendMessage(ActionEvent event) {
		String message = txfMessage.getText();
		send(message);
	}

	public void sendGroupMsg(ActionEvent event) {
		String message = txfGroupMessage.getText();
		sendGroup(message);
	}

	public void messageUser(ActionEvent event) {
		String user = lstOnlineUsers.getSelectionModel().getSelectedItem();

		if (user == null) {
			return;
		}

		if (user.equals(this.username + " (You)")) {
			return;
		}
		tpRoot.getSelectionModel().select(tabMessages);
		tpMessages.getSelectionModel().select(tabWhispers);
		cmbWhisperTo.getSelectionModel().select(user);
	}

	public void vnUser(ActionEvent event) {
		String user = lstOnlineUsers.getSelectionModel().getSelectedItem();

		if (user == null) {
			return;
		}

		if (user.equals(this.username + " (You)")) {
			return;
		}

		tpRoot.getSelectionModel().select(tabVoicenotes);
		accVoicenotes.setExpandedPane(atpRecording);
		cmbVoicenoteTo.getSelectionModel().select(user);
	}

	public void callUser(ActionEvent event) {
		String user = lstOnlineUsers.getSelectionModel().getSelectedItem();

		if (user == null) {
			return;
		}

		if (user.equals(this.username + " (You)")) {
			return;
		}

		tpRoot.getSelectionModel().select(tabCalls);
		lstInCall.getItems().add(user);

		btnRecord.setDisable(true);
		btnRecPlay.setDisable(true);
		btnRecStop.setDisable(true);
		btnSendVN.setDisable(true);
		btnPlay.setDisable(true);
		btnStop.setDisable(true);
		btnJoinGroup.setDisable(true);
		btnLeaveGroup.setDisable(true);
		btnVNUser.setDisable(true);

		this.client.call(user);
	}

	public void leaveCall(ActionEvent event) {
		btnRecord.setDisable(false);
		btnRecPlay.setDisable(false);
		btnRecStop.setDisable(false);
		btnSendVN.setDisable(false);
		btnPlay.setDisable(false);
		btnStop.setDisable(false);
		btnJoinGroup.setDisable(false);
		btnLeaveGroup.setDisable(false);
		btnVNUser.setDisable(false);

		this.client.leaveCall();

		lstInCall.getItems().clear();
		txaGroupMessages.setText("");
		txaGroupMessages.setPromptText("Group messages go here :)");
		txfMessage.setText("");
	}

	public void recordVN(ActionEvent event) {
		if (recording) {
			this.client.stopRec();

			btnRecord.setText("Record");
			btnSendVN.setDisable(false);
			btnRecPlay.setDisable(false);
			btnRecStop.setDisable(false);
			btnCallUser.setDisable(false);
			btnPlay.setDisable(false);
			btnStop.setDisable(false);
			lblRecStatus.setStyle("-fx-text-fill: #ff5c5c;");
			lblRecStatus.setText("Not recording");
			recording = false;
			return;
		}

		this.client.recordVoiceNote();

		btnRecord.setText("Stop");
		btnRecPlay.setText("Play");
		btnSendVN.setDisable(true);
		btnRecPlay.setDisable(true);
		btnRecStop.setDisable(true);
		btnCallUser.setDisable(true);
		btnPlay.setDisable(true);
		btnStop.setDisable(true);
		lblRecStatus.setStyle("-fx-text-fill: #acff5e;");
		lblRecStatus.setText("Recording");
		recording = true;
	}

	public void sendVN(ActionEvent event) {
		String user = cmbVoicenoteTo.getSelectionModel().getSelectedItem();

		if (user == null) {
			txaVoicenoteActivity.appendText("\nPlease select user to send voicenote to\n");
			return;
		}

		this.client.voicenote(user);
	}

	public void playRecVN(ActionEvent event) {
		if (playingRec) {
			btnRecPlay.setText("Resume");
			playingRec = false;
			this.client.pauseClip();
			return;
		}

		btnRecPlay.setText("Pause");
		playingRec = true;

		if (isFirstPlay) {
			this.client.playVoiceNote();
			isFirstPlay = false;
			return;
		}

		this.client.resumeClip();
	}

	public void stopRecVN(ActionEvent event) {
		btnRecPlay.setText("Play");
		this.client.stopClip();
	}

	public void playSentVN(ActionEvent event) {
		if (baos == null) {
			baos = this.client.getVnRecv();
		}

		if (baos == null) {
			return;
		}

		startRecvVN(baos);
		btnRecord.setDisable(true);
		btnRecPlay.setDisable(true);
		btnRecStop.setDisable(true);
		lblVNStatus.setStyle("-fx-text-fill: #acff5e;");
		lblVNStatus.setText("Playing");
	}

	public void stopSentVN(ActionEvent event) {
		if (clip == null) {
			return;
		}

		btnRecord.setDisable(false);
		btnRecPlay.setDisable(false);
		btnRecStop.setDisable(false);
		lblVNStatus.setStyle("-fx-text-fill: #ff5c5c;");
		lblVNStatus.setText("Not Playing");
		this.client.stopClip(clip);
	}

	public void joinGroup(ActionEvent event) {
		String g = txfGroup.getText();

		int gNum = -1;
		try {
			gNum = Integer.parseInt(g);
		} catch (NumberFormatException e) {
		}

		this.client.joinGroupNum(this.username, gNum);

		if (gNum < 0 || gNum > 254) {
			txaGroupMessages.appendText("\nInvalid group number.\n");
			return;
		}

		this.groupNum = gNum;
	}

	public void leaveGroup(ActionEvent event) {
		this.client.leaveGroup(this.username, this.groupNum);

		this.txaGroupMessages.setText("");
		this.txfGroup.setText("");
	}
}
