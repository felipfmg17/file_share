import java.net.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.*;



public class Kazaa{
	public static final String CONF = "conf.txt";
	public static final int TIMEOUT = 1000; // used for SoTimeout of Datagramsocket
	public static final int PACK_SIZE  = 1024;
	public static final int DIRECTORY_SCANNER_WAIT_TIME = 3000;
	public static final int FILES_FINDER_WAIT_TIME  = 1000*60*1;

	public static final int PORT_FILE_MANAGER = 4000;
	public static final int PORT_DIRECTORY_SERVER  = 4001;
	public static final int PORT_NOTIFICATION  = 4002;

	public static final int DELETE_FILE = 0;
	public static final int CREATE_FILE = 1;
	public static final int NAME_SIZE = 512;

	final String broad_cast_ip;
	final String path;
	DirectoryServer directory_server;
	DirectoryScanner directory_scanner;
	FileManager file_manager;
	ExecutorService executor;

	public static void main(String[] args){
		try{
			Scanner sc = new Scanner(new FileInputStream(new File(CONF)));
			Kazaa kazaa = new Kazaa(sc.next(),sc.next());
			kazaa.start();
			while(true){}
		
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public Kazaa(String broadcast_ip, String path) throws SocketException, IOException{
		this.path = path;
		broad_cast_ip = broadcast_ip;
	}

	public void start()  throws SocketException, IOException {
		startServices();
	}

	private void startServices() throws SocketException, IOException {
		executor = Executors.newCachedThreadPool();

		file_manager = new FileManager(new File(path), PORT_FILE_MANAGER );
		directory_server = new DirectoryServer(path, PORT_DIRECTORY_SERVER);


		System.out.println("Iniciando FileManager ... ");
		file_manager.start();
		System.out.println("Iniciando DirectoryServer ...");
		directory_server.start();
		System.out.println("Iniciando NotificationService ...");
		runStartNotificationService();
		System.out.println("Iniciando DirectoryScannerService ...");
		runStartDirectoryScannerService();
		System.out.println("Iniciando DirectoryServiceFinder ...");
		runStartDirectoryServerServiceFinder();

	}

	private void runEraseFile(String file_name){
		Runnable task = new Runnable(){
			public void run(){
				try{
					System.out.println("NotificationService: Borrando archivo con nombre: " + file_name);
					FileManager.eraseFile(path,file_name);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		};
		executor.submit(task);
	}

	private void runRequestFile(String file_name, String server_ip){
		Runnable task = new Runnable(){
			public void run(){
				try{
					System.out.println("NotificationService: Requesting file with name " + file_name + " from : " + server_ip );
					FileManager.requestFile(server_ip,PORT_FILE_MANAGER,path,file_name);
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		};
		executor.submit(task);
	}

	private void sendFileNotification(String file_name, int code) throws IOException {
		System.out.println("DirectoryScannerService: enviando notificacion broadcast del archivo " + file_name + " codigo: " + code );
		DatagramSocket soc = new DatagramSocket();
		soc.setBroadcast(true);
		UpdateMessage msg = new UpdateMessage(file_name.getBytes(), file_name.length(), code );
		byte[] buf = msg.getBytes();
		DatagramPacket pack = new DatagramPacket(buf,buf.length,InetAddress.getByName(broad_cast_ip),PORT_NOTIFICATION );
		soc.send(pack);
		soc.close();
	}

	private void runSendFileNotification(String file_name, int code){
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

	private void startNotificationService() throws IOException {
		DatagramSocket soc = new DatagramSocket(PORT_NOTIFICATION);
		soc.setBroadcast(true);
		System.out.println("NotificationService: iniciado con exito ");
		while(true){
			DatagramPacket pack = new DatagramPacket(new byte[PACK_SIZE], PACK_SIZE);
			System.out.println("NotificationService: esperando peticion ...");
			soc.receive(pack);	
			runAnswerNotification(pack);
		}
	}

	private void runAnswerNotification(DatagramPacket pack){
		Runnable task = new Runnable(){
			public void run(){
				try{
					answerNotification(pack);
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		};
		executor.submit(task);
	}

	private void answerNotification(DatagramPacket pack) throws IOException {
		System.out.println("NotificacionService: notificacion recibida");
		UpdateMessage msg = new UpdateMessage(pack.getData());
		String file_name = new String(msg.name,0,msg.name_size);
		int code = msg.code;
		System.out.println("NotificationService: respondiendo file_name: " + file_name + " , code : " + code );
		if(code==CREATE_FILE){
			runRequestFile(file_name,pack.getAddress().toString().substring(1) );
		}else if(code==DELETE_FILE){
			runEraseFile(file_name);
		}
	}

	private void runStartNotificationService(){
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

	private void startDirectoryScannerService() throws InterruptedException {
		DirectoryScanner directory_scanner  = new DirectoryScanner(new File(path));
		System.out.println("DirectoryScannerService: iniciado con exito ");
		while(true){
			System.out.println("DirectoryScannerService: buscando cambios en directorio");
			DirectoryScanner.ChangeList changes = directory_scanner.update();
			String[] names = changes.names;
			Integer[] vals = changes.vals;
			System.out.println("DirectoryScannerService: " + names.length + " archivos encontrados " );
			for(int i=0;i<names.length;i++){
				runSendFileNotification(names[i],vals[i]);
			}
			System.out.println("DirectoryScannerService: esperando ...");
			Thread.sleep(DIRECTORY_SCANNER_WAIT_TIME);
		}
	}

	private void runStartDirectoryScannerService(){
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

	private void requestSeveralFiles(Set<String> files_names, String ip ){
		for( String name: files_names ){	
			runRequestFile(name,ip);
		}
	}

	private void waitAnswerforDirectoryService(DatagramSocket soc){
		try{
			DatagramPacket npack = new DatagramPacket(new byte[DirectoryServer.PACK_SIZE], PACK_SIZE);
			soc.receive(npack);
			Set<String> files_names = null;
			files_names = (Set<String>)Tool.deSerialize(npack.getData());
			System.out.println("DirectoryServerFinder: Se obtuvo respuesta de " + npack.getAddress().toString().substring(1) + " se encontraron  " + files_names.size() + " archivos ");
			requestSeveralFiles(files_names,npack.getAddress().toString().substring(1));
		}catch(SocketTimeoutException e ){
			System.out.println("DirectoryServerFinder: No hubo respuesta ");
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private void runWaitAnswerforDirectoryService(DatagramSocket soc){
		Runnable task = new Runnable(){
			public void run(){
				waitAnswerforDirectoryService(soc);
			}
		};
		executor.submit(task);
	}

/* Sirve para pedir todos los archivos que ya existen en algun directorio 
se llamara al inicio y despues cada minuto */
	private void startDirectoryServerServiceFinder() throws IOException, SocketException, ClassNotFoundException, InterruptedException {
		DatagramSocket soc = new DatagramSocket();
		soc.setBroadcast(true);
		soc.setSoTimeout(7*TIMEOUT);
		System.out.println("DirectoryServerFinder: iniciado con exito ");
		while(true){
			System.out.println("DirectoryServerFinder: Pidiendo en broadcast set con todos los archivos");
			DatagramPacket pack = new DatagramPacket(new byte[0],0, InetAddress.getByName(broad_cast_ip), PORT_DIRECTORY_SERVER);
			soc.send(pack);		
			runWaitAnswerforDirectoryService(soc);
			runWaitAnswerforDirectoryService(soc);
			System.out.println("DirectoryServerFinder: esperando ...");
			Thread.sleep(FILES_FINDER_WAIT_TIME);
		}
	}

	private void runStartDirectoryServerServiceFinder() {
		Runnable task = new Runnable(){
			public void run(){
				try{
					startDirectoryServerServiceFinder();
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

		public UpdateMessage(byte[] bytes)   throws IOException {
			int offset = 0;
			name = Arrays.copyOfRange(bytes,offset,offset+NAME_SIZE);
			offset += NAME_SIZE;
			name_size = Tool.intValue(Arrays.copyOfRange(bytes,offset,offset+4));
			offset += 4;
			code = Tool.intValue(Arrays.copyOfRange(bytes,offset,offset+4));
		}

		public byte[] getBytes() throws IOException {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			bos.write(name);
			bos.write(Tool.getBytes(name_size));
			bos.write(Tool.getBytes(code));
			return bos.toByteArray();
		}


	}
}