import javafx.application.*;
import javafx.event.*;
import javafx.event.EventHandler;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.*;

import java.io.*;


public class ServerGUI extends Application implements EventHandler<ActionEvent> {

	public static Stage window;

	public TextArea txaLogs;

	public ListView<String> lstOffline;
	public ListView<String> lstOnline;

	private Scene scServer;

	@Override
	public void start(Stage primaryStage) throws IOException {
		this.txaLogs = new TextArea();
		this.lstOffline = new ListView<>();
		this.lstOnline = new ListView<>();
		this.scServer = null;
		window = primaryStage;
		FXMLLoader loader = new FXMLLoader(getClass().getResource("Server.fxml"));
		Parent root = loader.load();
		scServer = new Scene(root);
		Server serv = new Server(1234, loader);
		window.setOnCloseRequest(event -> {
			serv.quit();
			System.exit(0);
		});
		Thread qt = new Thread() {
			@Override
			public void run() {
				serv.start();
			}
		};
		qt.start();
		window.setTitle("VOIP");
		window.setScene(scServer);
		window.show();
	}

	@Override
	public void handle(ActionEvent event) { }

	public static void main(String[] args) throws Exception {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					System.err.println("Error with shutdown hook : " + e);
				}
			}
		});

		launch(args);
	}
}
