import java.net.*;
import java.util.*;
import java.io.*;

public class IPManager{
	Set<String> ips;
	DatagramSocket soc;

	public IPManager(int port) throws IOException {
		soc = new DatagramSocket(port);
	}


	public void listen(){

	}

	public void start(){

	}

	public static byte[] ipsToBytes(Set<String> ips) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		for(String ip: ips){ bos.write(IPtoBytes(ip)); }
		bos.flush();
		return bos.toByteArray();
	}

	public static Set<String> bytesToIps(byte[] b){
		Set<String> ips = new HashSet<String>();
		for(int i=0;i<b.length/4;i++){
			ips.add( bytesToIP(Arrays.copyOfRange(b,i*4,i*4+4)));
		}
		return ips;
	}

	public static byte[] IPtoBytes(String ip){
		String[] tokens = ip.split("\\.");
		byte[] bytes = new byte[4];
		for(int i=0;i<4;i++){
			bytes[i] = (byte) Integer.parseInt(tokens[i]);
		}
		return bytes;
	}

	public static String  bytesToIP(byte[] b){
		String s = "";
		for(int i=0;i<4;i++){
			int x = ((int)b[i]+256)%256;
			s += x;
			if(i<3) s += ".";
		}
		return s;
	}

	public static void main(String[] args) throws IOException {
		Scanner sc = new Scanner(System.in);
		Set<String> ips = new HashSet<String>();
		int n= sc.nextInt();
		while(n!=0){
			ips.add(sc.next());
			n--;
		}

		byte[] buf = ipsToBytes(ips);
		Set<String> ans = bytesToIps(buf);

		System.out.println("\n\n");
		for(String e: ans){
			System.out.println(e);
		}
	}


}