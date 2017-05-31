
import java.net.*;
import java.util.*;
import java.io.*;

class FileManagerTest1{

	public static void serverTest(String path, int port) throws IOException {
		FileManager fm = new FileManager(new File(path), port);
		fm.listen();
	}

	public static void clientTest(String ip, int port, String path, String file_name) throws IOException {
		boolean ans = FileManager.requestFile(ip,port,path,file_name);
		System.out.println(ans);
	}

	public static void main(String[] args) throws IOException {
		if( Integer.parseInt(args[0])==0 ){
			serverTest(args[1], Integer.parseInt(args[2]));
		}else{
			clientTest(args[1], Integer.parseInt(args[2]), args[3], args[4] );
		}
	}
}


/*

Server Excecution: java FileManagerTest1 0 C:\Users\Usuario\Desktop\file_share\TestOrigin 4000

Client Execution:  java FileManagerTest1 1 localhost 4000  C:\Users\Usuario\Desktop\file_share\TestDestination  hola.txt

*/