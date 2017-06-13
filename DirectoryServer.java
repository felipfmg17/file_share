import java.net.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.*;

/* Servidor que envia los un Set con el nombre de todos los archivos en la carpeta */

class DirectoryServer{
	public static final int PACK_SIZE = 1024;
	public static final int TIMEOUT = 1000; // used for SoTimeout of Datagramsocket
	public static final int REPS = 7; // max number of repeitions packet if the message is not responded


	String path;
	DatagramSocket soc;
	ExecutorService executor;
	Thread th;
	Random rand;



	public DirectoryServer(String path, int port) throws SocketException {
		this.path = path;
		soc = new DatagramSocket(port);
		soc.setBroadcast(true);
		executor = Executors.newCachedThreadPool();
		rand = new Random();
	}

	private void answerRequest(DatagramPacket pack) throws IOException, InterruptedException {
		Thread.sleep((rand.nextInt(5))*TIMEOUT);
		System.out.println("DirectoryServer: Peticion recibida de " + pack.getAddress().toString().substring(1) );
		DatagramSocket soc = new DatagramSocket();
		Set<String> file_names = DirectoryScanner.getFiles(new File(path));
		//System.out.println(file_names);
		byte[] buf = Tool.serialize((Serializable)file_names);
		pack.setData(buf);
		System.out.println("DirectoryServer: Respondiendo peticion ");
		soc.send(pack);
		soc.close();
	}

	private void runAnswerRequest(DatagramPacket pack){
		Runnable task = new Runnable(){
			public void run(){
				try{
					answerRequest(pack);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		};
		executor.submit(task);
	}

	private void listen() throws IOException{
		System.out.println("DirectoryServer: Servidor iniciado con exito ");
		while(true){
			DatagramPacket pack = new DatagramPacket(new byte[PACK_SIZE], PACK_SIZE );
			System.out.println("DirectoryServer: Esperando peticiones ...");
			soc.receive(pack);
			runAnswerRequest(pack);
		}
	}

	public void start(){
		Runnable task = new Runnable(){
			public void run(){
				try{
					listen();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		};
		th = new Thread(task);
		th.start();
	}



	public static  Set<String> requestFileNames(String ip, int port) throws IOException, SocketException, ClassNotFoundException {
		DatagramSocket soc = new DatagramSocket();
		soc.setSoTimeout(TIMEOUT);
		soc.setBroadcast(true);
		DatagramPacket pack = new DatagramPacket(new byte[0], 0, InetAddress.getByName(ip), port);
		DatagramPacket npack = new DatagramPacket(new byte[PACK_SIZE], PACK_SIZE);
		int count = REPS;
		Set<String> file_names = null;
		while(count>0){
			soc.send(pack);
			try{
				System.out.println("Enviando peticion de nombres de archivos");
				soc.receive(npack);
				System.out.println("Se recibieron " + npack.getLength() + " bytes ");
			}catch(IOException e){
				count--;
				continue;
			}
			byte[] buf = npack.getData();
			file_names = (Set<String>)Tool.deSerialize(buf);
			break;
		}
		return file_names;

	}

	public static void clientTest(String ip, int port) throws IOException, SocketException, ClassNotFoundException {
		Set<String> files = DirectoryServer.requestFileNames(ip,port);
		System.out.println(files);
	}


	public static void serverTest(int port, String path)  throws IOException, SocketException, ClassNotFoundException {
		DirectoryServer ds = new DirectoryServer(path, port);
		ds.start();
	}

}