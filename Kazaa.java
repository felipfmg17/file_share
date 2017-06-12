import java.net.*;
import java.util.*;
import java.io.*;



public class Kazaa{
	public static final int TIMEOUT = 1000; // used for SoTimeout of Datagramsocket
	public static final int PACK_SIZE  = 1024;
	public static final int DIRECTORY_SCANNER_WAIT_TIME = 2000;

	public static final int PORT_FILE_MANAGER = 4000;
	public static final int PORT_IP_MANAGER  = 4001;
	public static final int PORT_NOTIFICATION  = 4002;

	public static final int DELETE_FILE = 0;
	public static final int CREATE_FILE = 1;
	public static final int NAME_SIZE = 512;

	final String broad_cast_ip;
	final String path;
	IPManager ip_manager;
	DirectoryScanner directory_scanner;
	FileManager file_manager;
	ExecutorService executor;


	public void runEraseFile(String file_name){
		Runnable task = new Runnable(){
			public void run(){
				try{
					FileManager.eraseFile(path,file_name);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}

	public void runRequestFile(String file_name, String server_ip){
		Runnable task = new Runnable(){
			public void run(){
				try{
					System.out.println("Requesting file: " + file_name + " from : " + server_ip );
					FileManager.requestFile(server_ip,PORT_FILE_MANAGER,path,file_name);
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		};
		executor.submit(task);
	}

	public void sendFileNotification(String file_name, int code) throws IOException {
		DatagramSocket soc = new DatagramSocket();
		soc.setBroadcast(true);
		UpdateMessage msg = new UpdateMessage(file_name.getBytes(), file_name.size(), CREATE_FILE );
		byte[] buf = msg.getBytes();
		DatagramPacket pack = new DatagramPacket(buf,buf.size(),InetAddress.getByName(broad_cast_ip),PORT_NOTIFICATION );
		soc.send(pack);
		soc.close();
	}

	public void runSendFileNotification(String file_name, int code){
		Runnable task = new Runnable(){
			public void run(){
				try{
					sendFileNotification(file_name, code);
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		};
		executor.submit(task);
	}

	public void startNotificationService() throws IOExcetion {
		DatagramSocket soc = new DatagramSocket(PORT_NOTIFICATION);
		soc.setBroadcast(true);
		while(true){
			DatagramPacket pack = new DatagramPacket(new byte[PACK_SIZE], PACK_SIZE);
			soc.receive(pack);
			UpdateMessage msg = new UpdateMessage(pack.getData());
			String file_name = new String(msg.name,0,msg.name_size);
			int code = msg.code;
			if(code==CREATE_FILE){
				runRequestFile(file_name,pack.getAddress().toString() );
			}else if(code==DELETE_FILE){
				runEraseFile(file_name);
			}
		}
	}

	public void runStartNotificationService(){
		Runnable task = new Runnable(){
			public void run(){
				try{
					startNotificationService();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		};
		executor.submit(task);
	}

	public void startDirectoryScannerService(){
		DirectoryScanner directory_scanner  = new DirectoryScanner(path);
		while(true){
			DirectoryScanner.ChangeList changes = directory_scanner.update();
			String[] names = changes.names;
			Integer[] vals = changes.vals;
			for(int i=0;i<names.length;i++){
				runSendFileNotification(name[i],vals[i]);
			}
			Thread.sleep(DIRECTORY_SCANNER_WAIT_TIME);
		}
	}

	public void runStartDirectoryScannerService(){
		Runnable task = new Runnable(){
			public void run(){
				try{
					startDirectoryScannerService();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		};
		executor.submit(task);
	}






	class UpdateMessage{
		byte[] name;
		int name_size;
		int code;


		public UpdateMessage(byte[] name, int name_size, int code){
			this.name = Arrays.copyOf(name,NAME_SIZE);
			this.name_size = name_size;
			this.code = code;
		}

		public UpdateMessage(byte[] bytes){
			int offset = 0;
			name = Arrays.copyOfRange(bytes,offset,offset+NAME_SIZE);
			offset += NAME_SIZE;
			name_size = Tool.intValue(Arrays.copyOfRange(bytes,offset,offset+4));
			offset += 4;
			code = Tool.intValue(Arrays.copyOfRange(bytes,offset,offset+4));
		}

		public byte[] getBytes(){
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			bos.write(name);
			bos.write(Tool.getBytes(name_size));
			bos.write(Tool.getBytes(code));
		}


	}
}