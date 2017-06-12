import java.net.*;
import java.util.*;
import java.io.*;


/*


*/

public class IPManager{
	public static final int TIMEOUT = 1000; // used for SoTimeout of Datagramsocket
	public static final int REPS = 7; // max number of repeitions packet if the message is not responded
	public static final int BUF_SIZE = 1300;

	Set<String> ips;
	DatagramSocket soc;
	Thread th;

	public IPManager(int port) throws IOException {
		soc = new DatagramSocket(port);
		soc.setSoTimeout(4*TIMEOUT);
		soc.setBroadcast(true);
		ips = new HashSet<String>();
	}

	public IPManager(int port, Set<String> ips) throws IOException {
		soc = new DatagramSocket(port);
		soc.setSoTimeout(4*TIMEOUT);
		soc.setBroadcast(true);
		this.ips = new HashSet<String>(ips);
	}


	private void listen() throws IOException, SocketException {
		DatagramPacket pack = new DatagramPacket(new byte[BUF_SIZE],BUF_SIZE);
		while(!th.interrupted()){
			//System.out.println(th.interrupted());
			System.out.println("\nEsperando peticiones de lista de ip ... ");
			try{soc.receive(pack);}
			catch(SocketTimeoutException e){continue; }
			System.out.println("Peticion recibida de : "  + pack.getAddress().toString().substring(1) + " " + pack.getPort() );
			String ip = pack.getAddress().toString().substring(1);
			ips.add(ip);
			byte[] ips_bytes = ipsToBytes(ips);
			System.out.println("Enviando " + ips.size() + " ip a: " + pack.getAddress().toString().substring(1) + " " + pack.getPort() );
			pack.setData(ips_bytes);
			soc.send(pack);
		}
	}


	/*
		The server starts to listen for request of the ips
		Each time a requeste is received it respons sending the ips set
	*/
	public void start(){
		Runnable task = new Runnable(){
			public void run()  {
				try{ listen();}
				catch(IOException e){ e.printStackTrace(); };
			}
		};

		th = new Thread(task);
		th.start();
	}

	public void stop() throws InterruptedException {
		System.out.println("interrupted");
		th.interrupt();
		th.join();
	}


	/* 
	request the list of ips to a server with the specified ip and port
	*/
	public static Set<String> requestIps(String ip, int port) throws IOException, SocketException {
		DatagramSocket soc = new DatagramSocket();
		soc.setSoTimeout(TIMEOUT);
		soc.setBroadcast(true);
		int count = REPS;
		while(count>0){
			DatagramPacket pack = new DatagramPacket(new byte[0], 0, InetAddress.getByName(ip), port );
			System.out.println("\nEnviando peticion de lista de ip al servidor : "  + ip + " " + port);
			soc.send(pack);
			pack.setData(new byte[BUF_SIZE]);
			try{ soc.receive(pack); }
			catch(SocketTimeoutException e){
				count--;
				System.out.println("No hubo respuesta, Reintentando");
				continue;
			}
			System.out.println("Recibidas " + pack.getLength()/4 + "  IPs de: " + pack.getAddress().toString().substring(1) + " " + pack.getPort() );
			return bytesToIps(Arrays.copyOfRange(pack.getData(),0,pack.getLength()));
		}
		System.out.println("No hubo ninguna respuesta ");
		return null;
	}

	/* Transforms a set of strings containg ip address into  a byte array */
	public static byte[] ipsToBytes(Set<String> ips) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		for(String ip: ips){ bos.write(IPtoBytes(ip)); }
		bos.flush();
		return bos.toByteArray();
	}

	/* Tansforms a byte array containing ip address each one represente as 4 bytes */
	public static Set<String> bytesToIps(byte[] b){
		Set<String> ips = new HashSet<String>();
		for(int i=0;i<b.length/4;i++){
			ips.add( bytesToIP(Arrays.copyOfRange(b,i*4,i*4+4)));
		}
		return ips;
	}

	/* Transforms an ip address into 4 bytes */
	public static byte[] IPtoBytes(String ip){
		String[] tokens = ip.split("\\.");
		byte[] bytes = new byte[4];
		for(int i=0;i<4;i++){
			bytes[i] = (byte) Integer.parseInt(tokens[i]);
		}
		return bytes;
	}


	/*  Transforms 4 bytes into an IP address */
	public static String  bytesToIP(byte[] b){
		String s = "";
		for(int i=0;i<4;i++){
			int x = ((int)b[i]+256)%256;
			s += x;
			if(i<3) s += ".";
		}
		return s;
	}


	public static void ips_conversion_test(String[] args) throws IOException, SocketException{
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





	// Test


	public static void clienttest1(String ip, int port) throws IOException, SocketException{
		Set<String> ips = IPManager.requestIps(ip,port);
		for(String s: ips){
			System.out.println(s);
		}
	}

	public static void servertest1(int port) throws IOException, SocketException, InterruptedException {
		IPManager man = new IPManager(port);
		man.start();
		Scanner sc = new Scanner(System.in);
		int x = sc.nextInt();
		man.stop();
	}

	public static void listen_request_test(String[] args) throws IOException, SocketException, InterruptedException {
		if( Integer.parseInt(args[0]) == 0 ){
			servertest1( Integer.parseInt(args[1]) );
		}else{
			clienttest1( args[1], Integer.parseInt(args[2]) );
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		listen_request_test(args);
	}


}