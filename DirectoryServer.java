import java.net.*;
import java.util.*;
import java.io.*;


class DirectoryServer{
	public static final int PACK_SIZE = 1024;
	public static final int TIMEOUT = 1000; // used for SoTimeout of Datagramsocket
	public static final int REPS = 7; // max number of repeitions packet if the message is not responded


	String path;
	DatagramSocket soc;
	ExecutorService executor;
	Thread th;



	public DirectoryServer(String path, int port){
		this.path = path;
		soc = new DatagramSocket(port);
		soc.setBroadcast(true);
	}

	public void answerRequest(DatagramPacket pack){
		Set<String> file_names = DirectoryScanner.getFiles(path);
		byte[] buf = Tool.serialize(file_names);
		pack.setData(buf);
		soc.send(pack);
	}

	public void runAnswerRequest(DatagramPacket pack){
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


	public void listen() throws IOException{
		while(true){
			DatagramPacket pack = new DatagramPacket(new byte[PACK_SIZE], PACK_SIZE );
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
		}
		th = new Thread(task);
		th.start();
	}

	public static  Set<String> requestFileNames(String ip, int port){
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
				soc.receive(npack);
			}catch(IOException e){
				count--;
				continue;
			}
			byte[] buf = npack.getData();
			file_names = Tool.deSerialize(buf);
			break;
		}
		return file_names;

	}

}