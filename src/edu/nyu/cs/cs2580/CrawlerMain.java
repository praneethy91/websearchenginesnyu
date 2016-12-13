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
    HashMap<String, String> urlLocator = new HashMap<>();
    final String NewsLinks = "conf/newslinks.txt";
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
            int debt = toCrawl - (j - prevJ);
            prevJ = j;
            toCrawl = debt + Crawler.MAX_PAGES_TO_SEARCH;
            Crawler craw = new Crawler();
            System.out.println(newsWebsite + " started");
            String[] tokens = newsWebsite.split("#");
            if(!visitedURLs.contains((tokens[2])))
                j = craw.search(new ch.sentric.URL(tokens[2]), j, visitedURLs, toCrawl, urlLocator, tokens[0],tokens[1],  tokens[2],tokens[3]);
            System.out.println(newsWebsite + " ended");


        }
    }catch (Exception e){
        System.out.print("Some problem");
    }

      ObjectOutputStream writer =
              new ObjectOutputStream(new FileOutputStream(NewsClassificationConstants.newsFileToURLFile, false));
      writer.writeObject(urlLocator);
      writer.close();
  }
}
