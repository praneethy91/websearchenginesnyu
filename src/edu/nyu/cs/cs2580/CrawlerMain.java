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
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(NewsClassificationConstants.newsFileToURLFile, true)));
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
      int toCrawl = 0;
    try {
        while ((newsWebsite = br.readLine()) != null) {
            bw.append(newsWebsite + " ");
            bw.append(NewsClassificationConstants._corpusFilePrefix + j + " to ");
            int debt = toCrawl - (j - prevJ);
            prevJ = j;
            toCrawl = debt + Crawler.MAX_PAGES_TO_SEARCH;
            Crawler craw = new Crawler();
            //System.out.println(newsWebsite + " started");
            if(!visitedURLs.contains((newsWebsite)))
                j = craw.search(new ch.sentric.URL(newsWebsite), j, visitedURLs, toCrawl, writer);
            bw.append(NewsClassificationConstants._corpusFilePrefix + (j - 1));
            bw.append("\n");
            bw.flush();
            //System.out.println(newsWebsite + " ended");

        }
    }catch (Exception e){
        //System.out.print("Some problem");
    }
      writer.close();
      br.close();
      bw.close();
  }
}
