package edu.nyu.cs.cs2580;

import com.sun.org.apache.regexp.internal.RE;
import com.sun.org.apache.xml.internal.serializer.utils.SystemIDResolver;

import java.io.*;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedList;
import java.util.*;

/**
 * Created by mansivirani and Praneeth Y on 25/10/16.
 */
public class Vbyte {

    public static void main(String[] args) throws IOException {

        File fileOutWithBytes = new File("out.txt");
        FileOutputStream fosToByte = new FileOutputStream(fileOutWithBytes.getAbsolutePath());
        File fileInWithInt = new File("/Users/mansivirani/websearchenginesnyu/src/edu/nyu/cs/cs2580/inp1.txt");

        File fileoutwithint = new File("backtoint.txt");
        FileWriter fostoint = new FileWriter(fileoutwithint);
        File fileinwithbytes = new File("/Users/mansivirani/websearchenginesnyu/out.txt");

        if (!fileOutWithBytes.exists()) {
            fileOutWithBytes.createNewFile();
        }

        FileWriter frw = new FileWriter(fileInWithInt, false);

        FileReader fr = new FileReader(fileInWithInt);

        FileReader fr2 = new FileReader(fileinwithbytes);

        for (int i = 0; i < 1000000; i++) {
            frw.write(String.valueOf(i));
            frw.write(" ");
        }

        frw.close();

        Scanner br = new Scanner(fr);

        while (br.hasNext()) {

            int num = Integer.parseInt(br.next());

            fosToByte.write(Get(num));

        }
        fr.close();
        br.close();

        FileInputStream br2 = new FileInputStream(fileinwithbytes);

        WriteToIntFile(fostoint, br2);

        fostoint.close();
        fr2.close();
        br2.close();
        fosToByte.close();
    }

    // Load file from disk and then compress it

    public static byte[] Get(int num) throws IOException {

        LinkedList<Byte> bytePos = new LinkedList<>();
        int x = ((1 << 7) | (num & ((1 << 7) - 1)));
        bytePos.addFirst((byte) x);
        num = num >> 7;

        while (num != 0) {
            bytePos.addFirst((byte) (num & ((1 << 7) - 1)));
            num = num >> 7;
        }

        byte[] arr = new byte[bytePos.size()];
        int i = 0;
        for (Byte by : bytePos) {
            arr[i++] = by;
        }
        return arr;

    }

    public static void WriteToIntFile(FileWriter fosint, FileInputStream br2) throws IOException {

        int partialResult = 0;
        int curr;
        while ((curr = br2.read()) != -1) {

            if ((curr >> 7 == 1)) {
                partialResult = (partialResult << 7 | ((1 << 7) ^ curr));
                fosint.write(String.valueOf(partialResult));
                fosint.write(" ");
                partialResult = 0;
            } else {
                partialResult = (partialResult << 7) | curr;
            }
        }
    }
}
