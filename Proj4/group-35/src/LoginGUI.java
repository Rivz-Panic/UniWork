import javafx.application.*;
import javafx.event.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.*;

import javax.swing.JOptionPane;

import java.io.IOException;


public class LoginGUI extends Application implements EventHandler<ActionEvent> {

	public static Stage window;
	private static Client client;

	public int port;
	public String ip, username;

	public Button btnLogin;
	public TextField txfIP, txfPort, txfUsername, txfChat;

	private Scene loginSC;


	private void initGlobals() {
		this.port = 1234;
		this.ip = "localhost";
		this.username = "";

		this.btnLogin = new Button();
		this.txfIP = new TextField();
		this.txfPort = new TextField();
		this.txfUsername = new TextField();
		this.txfChat = new TextField();

		this.loginSC = null;
		client = null;
	}

	@Override
	public void start(Stage primaryStage) throws IOException {
		initGlobals();

		window = primaryStage;

		FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
		Parent root = loader.load();
		loginSC = new Scene(root);

		window.setTitle("VOIP");
		window.setScene(loginSC);
		window.show();
	}

	private void loadScene(String fxmlPath) throws IOException {
		String hname = "", prt = "", uname = "";

		hname = txfIP.getText();
		prt = txfPort.getText();
		uname = txfUsername.getText();

		boolean validIP = hname.matches("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

		if (hname.length() == 0) {
			hname = "localhost";
		} else if (!hname.equals("localhost") && !validIP) {
			JOptionPane.showMessageDialog(null, "Invalid IPv4 Address");
			return;
		}

		if (prt.length() == 0) {
			prt = "1234";
		}

		if (uname.length() == 0 || uname.contains(" ")) {
			JOptionPane.showMessageDialog(null, "Invalid Username");
			return;
		}

		boolean validPort = prt.matches("^[0-9]{4}$");
		if (!validPort) {
			JOptionPane.showMessageDialog(null, "Invalid Port Number");
			return;
		}

		this.port = Integer.parseInt(prt);
		this.username = uname;

		this.ip = hname;
		client = new Client(this.ip, this.port);

		if (!client.setup()) {
			JOptionPane.showMessageDialog(null, "Error in setup.");
			return;
		}

		if (!client.login(this.username)) {
			this.username = "";
			JOptionPane.showMessageDialog(null, "Username already taken");
			return;
		}

		FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
		Parent root = loader.load();
		ClientGUI controller = loader.getController();

		controller.setName(this.username);
		btnLogin.getScene().setRoot(root);
		controller.setup(this.ip, this.port, this.username, client);
	}

	public void enter(KeyEvent event) throws IOException {
		if (event.getCode() == KeyCode.ENTER) {
			loadScene("Client.fxml");
		}
	}

	public void login(ActionEvent event) throws IOException {
		loadScene("Client.fxml");
	}

	@Override
	public void handle(ActionEvent event) { }

	public static void main(String[] args) throws Exception {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(250);

					if (client == null) {
						System.out.println("\nShutting down\n");
						return;
					}

					System.err.println("\nClient shutting down\n");
				} catch (InterruptedException e) {
					System.err.println("Error with shutdown hook : " + e);
				}
			}
		});

		launch(args);
	}
}
