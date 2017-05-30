import java.net.*;
import java.util.*;
import java.io.*;

public class Main{

	static void client(String ip, int port) throws IOException {
		DatagramSocket soc=new DatagramSocket();
		byte[] buf=new byte[2];
		Random rand=new Random();
		buf[0]=(byte)rand.nextInt();
		buf[1]=(byte)rand.nextInt();
		DatagramPacket pack = new DatagramPacket(buf,buf.length,InetAddress.getByName(ip),port);
		soc.send(pack);
		pack.setData(new byte[16]);
		soc.receive(pack);
		System.out.println(pack.getData()[0]);
	}

	static void server(int port) throws IOException {
		DatagramSocket soc=new DatagramSocket(port);
		soc.setSoTimeout(500);
		while(true){
			DatagramPacket pack=new DatagramPacket(new byte[2], 2);	
			try{ soc.receive(pack); } catch(SocketTimeoutException e){ System.out.println("No hay preguntas"); continue;}
			byte[] buf=pack.getData();
			System.out.println("numeros: "+buf[0]+" "+buf[1]);
			buf[0]+=buf[1];
			pack.setData(buf);
			soc.send(pack);
		}
	}

	public static void main(String argv[] ) throws IOException {
		int op = Integer.parseInt(argv[0]);
		if(op==0) server(Integer.parseInt(argv[1]) );
		else client(argv[1],Integer.parseInt(argv[2]));
	}

}