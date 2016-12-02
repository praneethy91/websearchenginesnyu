package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

/**
 * Created by mansivirani on 30/11/16.
 */
public class CrawlerMain {


        /**
         * This is our test. It creates a spider (which creates spider legs) and crawls the web.
         *
         * @param args
         *            - not used
         */
        public static void main(String[] args) throws IOException
        {
            final String NewsLinks = "conf/newslinks.txt";
            File websitesFile = new File(NewsLinks);
            FileReader fr = new FileReader(websitesFile);
            BufferedReader br = new BufferedReader(fr);
            String newsWebsite;
            int j = 10000000;
            //For

            try {
                while ((newsWebsite = br.readLine()) != null) {
                    Crawler craw = new Crawler();
                    j = craw.search(new URL(newsWebsite), j);
                }
            }catch (Exception e){
                System.out.print("Some problem");
            }
            //end of loop
        }

}
