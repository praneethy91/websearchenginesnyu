package edu.nyu.cs.cs2580;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Created by mansivirani on 30/11/16.
 */
public class CrawlerMain {
  public static void main(String[] args) throws IOException
  {
    BufferedWriter writerURL =
            new BufferedWriter(new FileWriter(NewsClassificationConstants.newsFileToURLFile, true));
    HashMap<String, String> urlLocator = new HashMap<>();
    final String NewsLinks = "conf/newslinks.txt";
      final String crawlStatistics = NewsClassificationConstants.modelDir + "/" + "crawlStatistics";
      File crawlStatisticsFile = new File(crawlStatistics);
      BufferedWriter bw = new BufferedWriter(new FileWriter(crawlStatisticsFile, false));
    File websitesFile = new File(NewsLinks);
    FileReader fr = new FileReader(websitesFile);
    BufferedReader br = new BufferedReader(fr);
    String newsWebsite;
    Set<String> visitedURLs = new HashSet<String>();
    int j = 0;
      int prevJ = j;
      int toCrawl = j;
    try {
        while ((newsWebsite = br.readLine()) != null) {
            bw.write(newsWebsite + " ");
            bw.write(NewsClassificationConstants._corpusFilePrefix + j + " to ");
            int debt = toCrawl - (j - prevJ);
            prevJ = j;
            toCrawl = debt + Crawler.MAX_PAGES_TO_SEARCH;
            Crawler craw = new Crawler();
            //System.out.println(newsWebsite + " started");
            if(!visitedURLs.contains((newsWebsite)))
                j = craw.search(new ch.sentric.URL(newsWebsite), j, visitedURLs, toCrawl, writerURL);
            bw.write(NewsClassificationConstants._corpusFilePrefix + (j - 1));
            bw.write("\n");
            //System.out.println(newsWebsite + " ended");

        }
    }catch (Exception e){
        //System.out.print("Some problem");
    }

      writerURL.close();
      br.close();
      bw.close();
  }
}
