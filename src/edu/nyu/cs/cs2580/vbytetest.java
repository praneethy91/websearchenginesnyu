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
public class vbytetest {

    public static void main(String[] args) throws IOException {



        File fileOutWithBytes = new File("outvbytetest.txt");
        FileOutputStream fosToByte = new FileOutputStream(fileOutWithBytes.getAbsolutePath());
        File fileInWithInt = new File("/Users/mansivirani/websearchenginesnyu/src/edu/nyu/cs/cs2580/forvbytetest");


        File fileOutToOriginal = new File("backToOriginal.txt");
        FileWriter fostoint = new FileWriter(fileOutToOriginal);
        File fileinwithbytes = new File("/Users/mansivirani/websearchenginesnyu/outvbytetest.txt");

        DataInputStream dis = new DataInputStream(fosToByte);

        if (!fileOutWithBytes.exists()) {
            fileOutWithBytes.createNewFile();
        }

        if (!fileOutToOriginal.exists()) {
            fileOutToOriginal.createNewFile();
        }

        //FileWriter frw = new FileWriter(fileInWithInt, false);

        FileReader fr = new FileReader(fileInWithInt);

        FileReader fr2 = new FileReader(fileinwithbytes);


        Scanner br = new Scanner(fr);
       // int numcount = 0;
        //int strcount;


        while(br.hasNext()) {

          //  strcount = 0;
            //boolean isInteger = false;

            if (br.hasNextInt()) {
            //    numcount++;

                fosToByte.write(GetByteOfNum(br.next()));
            }

            else {

                fosToByte.writeUTF(br.next());

            }
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

    public void indexLoader() throws IOException{


    }

    // Load file from disk and then compress it

    public static byte[] GetByteOfNum(String num1) throws IOException {


        LinkedList<Byte> bytePos = new LinkedList<>();

        if(isitInteger == true) {

            int num = Integer.parseInt(num1);

            int x = ((1 << 7) | (num & ((1 << 7) - 1)));
            bytePos.addFirst((byte) x);
            num = num >> 7;


            while (num != 0) {
                bytePos.addFirst((byte) (num & ((1 << 7) - 1)));
                num = num >> 7;
            }

        }

        else{

            for (char num: num1.toCharArray()) {

                Byte c = (byte)num;

                int x = ((1 << 7) | ((c & ((1 << 7) - 1))));
                bytePos.addFirst((byte) x);
                int y = (c >> 7);

                bytePos.addFirst((byte) (((byte)y & ((1 << 7) - 1))));

                }
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
        char a;

        while ((curr = br2.read()) != -1) {


                if ((curr >> 7 == 1)) {
                    partialResult = (partialResult << 7 | ((1 << 7) ^ curr));
                    a = (char)((byte)partialResult);
                    fosint.write(a);
                    fosint.write(" ");
                    partialResult = 0;
                } else {
                    partialResult = (partialResult << 7) | curr;
                }

        }
    }
}
