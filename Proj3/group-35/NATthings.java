import java.util.*;


public class NATthings {

    private static final byte delim = '#';
	private static final long DEF_reshfreshTime = 30000;

	private String intIP;
	private String extIP;
	private long reshfreshTime;

	public NATthings(String intIP, String extIP, long reshfreshTime) {
		this.intIP = intIP.trim();
		this.extIP = extIP.trim();
		this.reshfreshTime = reshfreshTime;
	}

	public NATthings(String intIP, String extIP) {
		this(intIP, extIP, DEF_reshfreshTime);
	}

	public void setextIP(String extIP) {
		this.extIP = extIP.trim();
	}

	public String getintIP() {
		return this.intIP;
	}

	public String getextIP() {
		return this.extIP;
	}

	public long getreshfreshTime() {
		return this.reshfreshTime;
	}

	@Override
	public String toString() {
		return "internal IP is " + this.intIP + ", external IP is " +
			this.extIP;
	}

	public static String genIP() {
		Random r = new Random();
		StringBuilder ip = new StringBuilder(15);

		int ri;
		for (int i = 0; i < 4; i++) {
			ri = r.nextInt(256);

			if (i == 3) {
				ip.append(String.format("%02d", ri));
			} else {
				ip.append(String.format("%02d", ri) + ".");
			}
		}

		return ip.toString().trim();
	}

	public static String genMAC() {
		Random r = new Random();

		byte[] macAddress = new byte[6];
		r.nextBytes(macAddress); // generate 6 random bytes
		macAddress[0] = (byte) (macAddress[0] & (byte) 254); // makes unicast

		StringBuilder sb = new StringBuilder(18);
		for (byte b : macAddress) {
			if (sb.length() > 0) {
				sb.append(":");
			}

			sb.append(String.format("%02x", b));
		}

		return sb.toString();
	}

	public static byte[] genPacket(String sender, String destination, byte[] payload) {
		String del = new String(new byte[] {delim});
		String data = new String(payload);

		String pac = sender + del + destination + del + data;
		return pac.getBytes();
	}

	public static String[] splitPacket(byte[] packet) {
		String pac = new String(packet);
		return pac.split(new String(new byte[] {delim}));
	}

	public static void printList(LinkedList<NATthings> table) {
		for (int i = 0; i < table.size(); i++) {
			System.out.println("(" + (i+1) + ")  " + table.get(i).toString());
		}

		System.out.println("");
	}

	public static byte getByteDelim() {
		return delim;
	}

	public static String getStrDelim() {
		return new String(new byte[] {delim});
	}
}
