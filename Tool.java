import java.net.*;
import java.util.*;
import java.io.*;

public class Tool{

	/* Converts Integer to an array of four bytes */

    public static byte[] getBytes(int n) {
        byte[] b = {0, 0, 0, 0};
        for (int i = 0; i < 4; i++) {
            b[3 - i] = (byte) (((255 << (8 * i)) & n) >>> (8 * i));
        }
        return b;
    }

    /* Convert an array of 4 bytes into an integer */
	public static int intValue(byte[] b) {
        int n = 0;
        for (int i = 0; i < 4; i++) {
            n = (n << 8) | (b[i] & (int) 255);
        }
        return n;
    }

    public static void flow(InputStream in, OutputStream out, long n) throws IOException {
        byte[] buf = new byte[64 * 1024];
        int b;
        while (true) {
            b = in.read(buf, 0, (int)Math.min( (long)buf.length,n ));
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
}