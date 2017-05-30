import java.net.*;
import java.util.*;
import java.io.*;


public class Tool {

    public static final long SECOND = 1000;
    public static final long MINUTE = 60*SECOND;
    public static final long HOUR = 60*MINUTE;
    public static final long DAY = 24*HOUR;

    public static byte[] getBytes(int n) {
        byte[] b = {0, 0, 0, 0};
        for (int i = 0; i < 4; i++) {
            b[3 - i] = (byte) (((255 << (8 * i)) & n) >>> (8 * i));
        }
        return b;
    }

    public static byte[] getBytes(long n) {
        byte[] b = {0, 0, 0, 0, 0, 0, 0, 0};
        for (int i = 0; i < 8; i++) {
            b[7 - i] = (byte) (((255 << (8 * i)) & n) >>> (8 * i));
        }
        return b;
    }

    public static byte[] hexEncoder(byte[] m) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < m.length; i++) {
            sb.append(String.format("%02X", m[i]));
        }
        return sb.toString().getBytes();
    }

    public static byte[] hexDecoder(byte[] h) {
        String s = new String(h);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i += 2) {
            int n = Integer.valueOf(s.substring(i, i + 2), 16);
            bos.write((byte) n);
        }
        byte[] b = bos.toByteArray();
        return b;
    }

    public static byte[] serialize(Serializable o) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(o);
        oos.flush();
        byte[] b = bos.toByteArray();
        oos.close();
        bos.close();
        return b;
    }

    public static Object deSerialize(byte[] b) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(b);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object o = ois.readObject();
        ois.close();
        bis.close();
        return o;
    }

    public static int intValue(byte[] b) {
        int n = 0;
        for (int i = 0; i < 4; i++) {
            n = (n << 8) | (b[i] & (int) 255);
        }
        return n;
    }

    public static long longValue(byte[] b) {
        long n = 0;
        for (int i = 0; i < 8; i++) {
            n = (n << 8) | (b[i] & (int) 255);
        }
        return n;
    }

    public static void save(byte[] b, File f) throws IOException {
        FileOutputStream out = new FileOutputStream(f);
        out.write(b);
        out.close();
    }

    public static void save(Serializable o, File f) throws IOException{
        Tool.save(serialize(o), f);
    }

    public static void save(String s, File f) throws IOException{
        save(s.getBytes(),f);
    }

    public static Object loadObject(File f) throws IOException, ClassNotFoundException {
        return deSerialize(loadBytes(f));
    }

    public static byte[] loadBytes(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        byte[] b = new byte[1024*1024*16];
        int bytes = in.read(b);
        b = Arrays.copyOfRange(b, 0, bytes);
        in.close();
        return b;
    }

    public static String loadString(File f) throws IOException{
        return new String(loadBytes(f));
    }

    public static void flow(InputStream in, OutputStream out, long n) throws IOException {
        byte[] buf = new byte[64 * 1024];
        int b;
        while (true) {
            b = in.read(buf, 0, (int) Math.min((long) buf.length, n));
            if (b > 0) {
                out.write(buf, 0, b);
                out.flush();
                n -= b;
            }
            if (n >= 0)
                break;
        }
        out.flush();
    }

    public static void flow(InputStream in, OutputStream out) throws IOException{
        byte[] buf = new byte[64 * 1024];
        int b;
        while (  (b = in.read(buf) ) >= 0) {
            out.write(buf,0,b);
        }
        out.flush();
    }

    public static byte[] extract(InputStream in, long n) throws IOException{
        ByteArrayOutputStream out = new ByteArrayOutputStream((int)n);
        flow(in, out, n);
        byte[] b = out.toByteArray();
        out.close();
        return b;
    }

    public static byte[] extract(InputStream in) throws IOException{
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        flow(in, out);
        byte[] b = out.toByteArray();
        out.close();
        return b;
    }

    public static void write(int n, OutputStream out) throws IOException{
        byte[] b = getBytes(n);
        out.write(b);
        out.flush();
    }

    public static void write(long n, OutputStream out) throws IOException{
        byte[] b = getBytes(n);
        out.write(b);
        out.flush();
    }

    public static void write(byte[] b, OutputStream out) throws IOException {
        int size = b.length;
        write(size, out);
        out.write(b);
        out.flush();
    }

    public static void write(String s, OutputStream out) throws IOException{
        byte[] b = s.getBytes();
        write(b, out);
    }

    public static void write(Serializable o, OutputStream out) throws IOException{
        byte[] b = serialize(o);
        write(b, out);
    }

    public static void write(File f, OutputStream out) throws IOException{
        String name = f.getName();
        long size = f.length();
        FileInputStream in = new FileInputStream(f);
        write(name,out);
        write(size,out);
        flow(in, out, size);
        in.close();
    }

    public static void write(File[] files, OutputStream out) throws IOException{
        int fs = files.length;
        write(fs, out);
        for(File f: files){
            write(f, out);
        }
    }

    public static int readInt(InputStream in) throws IOException{
        return intValue(extract(in,4));
    }

    public static long readLong(InputStream in) throws IOException{
        return longValue(extract(in,8));
    }

    public static byte[] readBytes(InputStream in) throws IOException{
        int size = readInt(in);
        byte[] b = extract(in, size);
        return b;
    }

    public static String readString(InputStream in) throws IOException{
        byte[] b = readBytes(in);
        return new String(b);
    }

    public static Object readObject(InputStream in) throws IOException, ClassNotFoundException{
        byte[] b = readBytes(in);
        return deSerialize(b);
    }

    public static void readFile(File path, InputStream in) throws IOException{
        String name = readString(in);
        long size = readLong(in);
        File f = new File(path, name);
        FileOutputStream out = new FileOutputStream(f);
        flow(in, out, size);
        out.close();
    }

    public static void readFiles(File path, InputStream in) throws IOException{
        int fs = readInt(in);
        for(int i=0;i<fs;i++)
            readFile(path,in);
    }

    public static byte[] requestBytes(String url) throws IOException {
        URL u = new URL(url);
        InputStream in = u.openStream();
        byte[] b = extract(in);
        in.close();
        return b;
    }

    public static String requestString(String url) throws IOException {
        byte[] b = requestBytes(url);
        String s = new String(b);
        return s;
    }

    public static byte[] bits(byte b){
        byte[] bits = new byte[8];
        for(int i=0;i<8;i++)
            bits[7 - i] = (byte) (( 0 != ( (1<<i)&b) ) ? 1 : 0);
        return bits;
    }

    public static byte bits(byte[] bits){
        byte b = 0;
        for(int i=0;i<8;i++)
            if(bits[i]==1)
                b = (byte) (b & (1<<(7-i)));
        return b;
    }

    public static Thread execute(Runnable task){
        Thread thread = new Thread(task);
        thread.start();
        return thread;
    }

    public static void send(int n, Socket socket) throws IOException {
        write(n, socket.getOutputStream());
    }

    public static void send(long n, Socket socket) throws IOException {
        write(n, socket.getOutputStream());
    }

    public static void send(byte[] b, Socket socket) throws IOException {
        write(b, socket.getOutputStream());
    }

    public static void send(String s, Socket socket) throws IOException {
        write(s, socket.getOutputStream());
    }

    public static void send(Serializable o, Socket socket) throws IOException {
        write(o, socket.getOutputStream());
    }

    public static void send(File f, Socket socket) throws IOException {
        write(f, socket.getOutputStream());
    }

    public static void send(File[] files, Socket socket) throws IOException {
        write(files, socket.getOutputStream());
    }

    public static int receiveInt(Socket soc) throws IOException {
        return readInt(soc.getInputStream());
    }

    public static long receiveLong(Socket soc) throws IOException {
        return readLong(soc.getInputStream());
    }

    public static byte[] receiveBytes(Socket soc) throws IOException {
        return readBytes(soc.getInputStream());
    }

    public static String receiveString(Socket soc) throws IOException {
        return readString(soc.getInputStream());
    }

    public static Object receiveObject(Socket soc) throws IOException, ClassNotFoundException {
        return readObject(soc.getInputStream());
    }

    public static void receiveFile(File path, Socket soc) throws IOException {
        readFile(path, soc.getInputStream());
    }

    public static void receiveFiles(File path, Socket soc) throws IOException {
        readFiles(path, soc.getInputStream());
    }


}   