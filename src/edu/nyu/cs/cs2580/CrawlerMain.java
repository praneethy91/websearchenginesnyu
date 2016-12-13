package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Created by mansivirani on 30/11/16.
 */
public class CrawlerMain {
  public static void main(String[] args) throws IOException
  {
    final String NewsLinks = "conf/newslinks.txt";
    File websitesFile = new File(NewsLinks);
    FileReader fr = new FileReader(websitesFile);
    BufferedReader br = new BufferedReader(fr);
    String newsWebsite;
    Set<String> visitedURLs = new HashSet<String>();
    int j = 0;
      int toCrawl = j;
    try {
        while ((newsWebsite = br.readLine()) != null) {
            toCrawl += (toCrawl - j) + Crawler.MAX_PAGES_TO_SEARCH;
            Crawler craw = new Crawler();
            System.out.println(newsWebsite + " started");
            if(!visitedURLs.contains((newsWebsite)))
                j = craw.search(new ch.sentric.URL(newsWebsite), j, visitedURLs, toCrawl);
            System.out.println(newsWebsite + " ended");

        }
    }catch (Exception e){
        System.out.print("Some problem");
    }
  }
}
