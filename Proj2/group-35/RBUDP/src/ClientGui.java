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

public class ClientGui extends JFrame {

	private JPanel contentPane;
	private JTextField txtEnterServerIp;
	private JTextField txtEnterFile;
	private JTextField txtFilenameAtServer;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ClientGui frame = new ClientGui();
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
	public ClientGui() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		txtEnterServerIp = new JTextField();
		txtEnterServerIp.setText("Enter Server IP");
		txtEnterServerIp.setBounds(264, 17, 114, 19);
		contentPane.add(txtEnterServerIp);
		txtEnterServerIp.setColumns(10);
		
		txtEnterFile = new JTextField();
		txtEnterFile.setText("Enter File");
		txtEnterFile.setBounds(264, 73, 114, 19);
		contentPane.add(txtEnterFile);
		txtEnterFile.setColumns(10);
		
		txtFilenameAtServer = new JTextField();
		txtFilenameAtServer.setText("Filename at server (UDP)");
		txtFilenameAtServer.setBounds(264, 124, 114, 19);
		contentPane.add(txtFilenameAtServer);
		txtFilenameAtServer.setColumns(10);
		
		JButton btnTcpSend = new JButton("TCP send");
		btnTcpSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				txtEnterFile.setEnabled(false);
				txtEnterServerIp.setEnabled(false);
				txtFilenameAtServer.setEnabled(false);
				String IP = txtEnterServerIp.getText();
				String fileLoc = txtEnterFile.getText();
				TCPClient TCPsend = new TCPClient();
				TCPsend.fileName = fileLoc;
				TCPsend.host = IP;
				try {
					TCPsend.use();
				} catch (Exception e) {
					// TODO: handle exception
					System.out.println("error: " + e);
				}
				txtEnterFile.setEnabled(true);
				txtEnterServerIp.setEnabled(true);
				txtFilenameAtServer.setEnabled(true);
				txtEnterFile.setText("");
				txtFilenameAtServer.setText("");
			}
		});
		btnTcpSend.setBounds(74, 210, 117, 25);
		contentPane.add(btnTcpSend);
		
		JButton btnUdpSend = new JButton("UDP send");
		btnUdpSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				txtEnterFile.setEnabled(false);
				txtEnterServerIp.setEnabled(false);
				txtFilenameAtServer.setEnabled(false);
				String IP = txtEnterServerIp.getText();
				String fileLoc = txtEnterFile.getText();
				String fileSentName = txtFilenameAtServer.getText();
				Client UDPsend = new Client();
				UDPsend.fileName = fileLoc;
				UDPsend.serverAdd = IP;
				UDPsend.sentFileName = fileSentName;
				try {
					UDPsend.prep();
				} catch (Exception e) {
					// TODO: handle exception
					System.out.println("error: " + e);
				}
				txtEnterFile.setEnabled(true);
				txtEnterServerIp.setEnabled(true);
				txtFilenameAtServer.setEnabled(true);
				txtEnterFile.setText("");
				txtFilenameAtServer.setText("");
			}
		});
		btnUdpSend.setBounds(264, 210, 117, 25);
		contentPane.add(btnUdpSend);
		
		JLabel lblEnterIpOf = new JLabel("Enter IP of server to send to:");
		lblEnterIpOf.setBounds(39, 12, 218, 28);
		contentPane.add(lblEnterIpOf);
		
		JLabel lblEnterFilenameTo = new JLabel("Enter filename to send:");
		lblEnterFilenameTo.setBounds(39, 75, 207, 15);
		contentPane.add(lblEnterFilenameTo);
		
		JLabel lblEnterFileName = new JLabel("Enter server side filename");
		lblEnterFileName.setBounds(39, 102, 218, 59);
		contentPane.add(lblEnterFileName);
	}
}
