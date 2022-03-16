import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.*;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextPane;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeListener;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.*;
import java.beans.PropertyChangeEvent;

public class ClientGUI extends JFrame {

	private JPanel contentPane;
	private JTextField txtUsername;
	private JTextField txtIP;
	private JTextField txtMessage;

	final static int ServerPort = 1234;
	public static String name;
	public DataInputStream dis;
	public DataOutputStream dos;

	volatile String txtUpdater;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) throws UnknownHostException, IOException {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ClientGUI frame = new ClientGUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public ClientGUI() throws UnknownHostException, IOException {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 417);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JPanel panel = new JPanel();
		panel.setBounds(0, -19, 440, 406);
		contentPane.add(panel);
		panel.setLayout(null);

		TextArea txtaChat = new TextArea();
		txtaChat.setBounds(22, 61, 276, 222);
		panel.add(txtaChat);
		txtaChat.setText(txtUpdater);

		TextArea txtaUsers = new TextArea();
		txtaUsers.setBounds(304, 64, 114, 216);
		panel.add(txtaUsers);

		txtUsername = new JTextField();
		txtUsername.setBounds(22, 36, 114, 19);
		panel.add(txtUsername);
		txtUsername.setColumns(10);

		txtIP = new JTextField();
		txtIP.setBounds(138, 36, 114, 19);
		panel.add(txtIP);
		txtIP.setColumns(10);

		JButton btnRefresh = new JButton("Refresh");
		btnRefresh.setBounds(162, 346, 117, 25);
		panel.add(btnRefresh);

		JButton btnConnect = new JButton("connect");
		btnConnect.setBounds(264, 33, 134, 25);
		panel.add(btnConnect);

		txtMessage = new JTextField();
		txtMessage.setBounds(22, 289, 257, 34);
		panel.add(txtMessage);
		txtMessage.setColumns(10);

		JButton btnSend = new JButton("send");
		btnSend.setBounds(281, 286, 117, 37);
		panel.add(btnSend);

		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (txtUsername.isEnabled()) {
					name = txtUsername.getText();
					InetAddress ip = null;
					try {
						// this should work
						ip = InetAddress.getByName(txtIP.getText());
					} catch (UnknownHostException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					Socket s = null;
					// establish the connection
					System.out.println("Connecting to server on port: " + ServerPort);
					try {
						s = new Socket(ip, ServerPort);
						System.out.println("Connection successful!");
						txtaChat.append("Connection successful!\n");
						dis = new DataInputStream(s.getInputStream());
						dos = new DataOutputStream(s.getOutputStream());
						dos.writeUTF(name);
					} catch (IOException e) {
						System.err.println("Fatal connection error!, server not found");
						txtaChat.append("Fatal connection error!, server not found\n");
					}
					txtUsername.setEnabled(false);
					txtIP.setEnabled(false);
					btnConnect.setText("disconnect");
				} else {
					try {
						dos.writeUTF("logout");
						txtUsername.setEnabled(true);
						txtIP.setEnabled(true);
						btnConnect.setText("connect");
					} catch (Exception e) {
						System.err.println("Fatal error!, could not disconnect");
						txtaChat.append("Fatal connection error!, server not found\n");
					}
				}
			}
		});

		btnRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					// refreshes Chat text area
					txtaChat.append(dis.readUTF() + "\n");
				} catch (IOException e) {
					System.out.println("Disconnected from Server!");
				}
			}
		});

		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!txtMessage.getText().isEmpty()) {
					// read the message to deliver.
					String msg = txtMessage.getText();
					try {
						// write on the output stream
						dos.writeUTF(msg);
					} catch (IOException e) {
						txtaChat.append(" Fatal error in Client Send Message");
					}
				}
			}
		});
	}
}
