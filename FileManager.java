import java.net.*;
import java.util.*;
import java.io.*;


public class FileManager{
	public static final int MAX_SIZE=1300;
	public static final int FILE_GET = 0;
	public static final int FILE_DOESNT_EXIST = 1;
	public static final int FILE_GET_ANSWER = 2;
	public static final int BUF_SIZE = 1024;
	public static final int NAME_SIZE = 64;
	public static final int TIMEOUT = 500;
	public static final int REPS = 7;

	DatagramSocket soc;
	File path;

	public FileManager(File path, int port) throws SocketException {
		soc = new DatagramSocket(port);
		this.path = path;
	}

	public void listen() throws IOException {
		DatagramPacket pack = new DatagramPacket(new byte[MAX_SIZE],MAX_SIZE);
		while(true){
			soc.receive(pack);
			Message msg = new Message(pack.getData());
			if(msg.code==FILE_GET){
				File f = new File(path, new String(msg.name,0,msg.name_n));
				Message nmsg;
				if(f.exists()){
					FileInputStream in = new FileInputStream(f);
					Tool.extract(in,msg.offset);
					byte[] ans = Tool.extract(in,BUF_SIZE);
					nmsg = Message.answerMessage(ans);
					in.close();
				}else{
					nmsg = Message.errorMessage();
				}
				pack.setData(nmsg.getBytes());
				soc.send(pack);
			}
		}
	}

	public static int requestFileBytes(String ip, int port, String file_name, FileOutputStream out, int offset, DatagramSocket soc) throws IOException {
		Message msg = Message.requestMessage(file_name, offset);
		byte[] buf = msg.getBytes();
		DatagramPacket pack = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), port); // paquete con la peticion
		DatagramPacket npack = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE); // paquete para la respuesta 
		int count = REPS;
		while(count>0){
			soc.send(pack);
			try{
				soc.receive(npack);
			}catch(SocketTimeoutException e){
				count--;
				continue;
			}
			Message nmsg = new Message(npack.getData());
			byte[] data = nmsg.buf;
			out.write(data,0,nmsg.size);
			return nmsg.size;
		}
		return -1;
	}

	public static boolean requestFile(String ip, int port, String path, String file_name) throws IOException {
		DatagramSocket soc = new DatagramSocket();
		soc.setSoTimeout(TIMEOUT);
		File f = new File(path, file_name);
		FileOutputStream out = new FileOutputStream(f);
		int offset = 0;
		while(true){
			int ans = requestFileBytes(ip,port,file_name,out,offset,soc);
			if(ans>0){
				offset += ans;
			}
			else if(ans==0){
				out.close();
				return true;
			}else{
				out.close();
				f.delete();
				break;
			}
		}
		return false;
	}

	static class Message{
		int code;
		int name_n;
		byte[] name;
		int size;
		byte[] buf;
		int offset;

		public static Message requestMessage(String name, int offset){
			return new Message(FILE_GET,name.getBytes(),offset, new byte[0] );
		}

		public static Message answerMessage(byte[] buf){
			return new Message(FILE_GET_ANSWER, new byte[0], 0, buf);
		}

		public static Message errorMessage(){
			return new Message(FILE_DOESNT_EXIST, new byte[0], 0, new byte[0] );
		}


		public Message(int code, byte[] name, int offset, byte[] buf){
			this.code = code;
			this.name_n = name.length;
			this.name = Arrays.copyOf(name,NAME_SIZE);
			this.size = buf.length;
			this.buf = Arrays.copyOf(buf,BUF_SIZE);
			this.offset = offset;
		}

		public Message(byte[] buf) throws IOException {
			int off=0;
			code=Tool.intValue(Arrays.copyOfRange(buf,off,4));
			off+=4;
			name_n=Tool.intValue(Arrays.copyOfRange(buf,off,off+4));
			off+=4;
			name=Arrays.copyOfRange(buf,off,off+NAME_SIZE);
			off+=NAME_SIZE;
			size=Tool.intValue(Arrays.copyOfRange(buf,off,off+4));
			off+=4;
			this.buf=Arrays.copyOfRange(buf,off,off+BUF_SIZE);
			off+=BUF_SIZE;
			offset=Tool.intValue(Arrays.copyOfRange(buf,off,off+4));
		}

		byte[] getBytes() throws IOException {
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			bos.write(Tool.getBytes(code));
			bos.write(Tool.getBytes(name_n));
			bos.write(name);
			bos.write(Tool.getBytes(size));
			bos.write(buf);
			bos.write(Tool.getBytes(offset));
			return bos.toByteArray();
		}
	}
}



/*public class FileManager{
	public static void main(String args[]) throws IOException {
		byte[] data = {101,102,103};
		Message msg = new Message(17,"felipe".getBytes(),0,data);
		byte[] buf = msg.toBytes();
		System.out.println(buf.length);
		Message msg2 = new Message(buf);
		System.out.println(msg2.name_n);
		System.out.println(msg2.code+" "+ new String(msg2.name,0,msg2.name_n) + " " + msg2.size+" "+msg2.buf.length);
		System.out.println(msg2.buf[0]+" "+msg.buf[1]+" "+msg.buf[2]);
	}
}*/
