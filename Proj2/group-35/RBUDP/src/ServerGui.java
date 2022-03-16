import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JLabel;

public class ServerGui extends JFrame {

	private JPanel contentPane;
	private JTextField txtFileNametcp;
	private JLabel lblEnterRecievedFile;
	private JLabel lblStartServerOf;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ServerGui frame = new ServerGui();
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
	public ServerGui() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		txtFileNametcp = new JTextField();
		txtFileNametcp.setText("File Name(TCP)");
		txtFileNametcp.setBounds(165, 68, 114, 19);
		contentPane.add(txtFileNametcp);
		txtFileNametcp.setColumns(10);
		
		JButton btnStartUdpServer = new JButton("Start UDP server");
		btnStartUdpServer.setBounds(276, 221, 152, 25);
		contentPane.add(btnStartUdpServer);
		
		JButton btnStartTcpServer = new JButton("Start TCP Server");
		btnStartTcpServer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				TCPServer TCPserver = new TCPServer();
				TCPserver.fileName = txtFileNametcp.getText();
				txtFileNametcp.setEnabled(false);
				btnStartUdpServer.setEnabled(false);
				btnStartTcpServer.setEnabled(false);
				try {
					TCPserver.use();
				} catch (Exception e) {
					// TODO: handle exception
					System.out.println("error: " + e);
				}
				txtFileNametcp.setEnabled(true);
				btnStartUdpServer.setEnabled(true);
				btnStartTcpServer.setEnabled(true);
				txtFileNametcp.setText("");
			}
		});
		btnStartTcpServer.setBounds(25, 221, 152, 25);
		contentPane.add(btnStartTcpServer);
		
		lblEnterRecievedFile = new JLabel("Enter recieved file name for TCP:");
		lblEnterRecievedFile.setBounds(114, 22, 247, 34);
		contentPane.add(lblEnterRecievedFile);
		
		lblStartServerOf = new JLabel("Start server of type:");
		lblStartServerOf.setBounds(148, 175, 157, 34);
		contentPane.add(lblStartServerOf);
		
		
		btnStartUdpServer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Server UDPserver = new Server();
				txtFileNametcp.setEnabled(false);
				btnStartUdpServer.setEnabled(false);
				btnStartTcpServer.setEnabled(false);
				try {
					UDPserver.main(null);
				} catch (Exception e) {
					// TODO: handle exception
					System.out.println("error: " + e);
				}
				txtFileNametcp.setEnabled(true);
				btnStartUdpServer.setEnabled(true);
				btnStartTcpServer.setEnabled(true);
			}
		});
	}
}
