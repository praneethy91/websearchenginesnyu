package edu.nyu.cs.cs2580; /**
 * Created by mansivirani on 30/11/16.
 */
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class Crawler
{
  private static final int MAX_PAGES_TO_SEARCH = 3;
  private Set<String> pagesVisited = new HashSet<String>();
  private LinkedList<ch.sentric.URL> pagesToVisit = new LinkedList<ch.sentric.URL>();

  public int search(ch.sentric.URL urlFormatted, int j, Set<String> visitedURLs) throws IOException {
  String hostName = urlFormatted.getAuthority().getHostName().getAsString();
  this.pagesToVisit.addFirst(urlFormatted);

  while (pagesVisited.size() < MAX_PAGES_TO_SEARCH ) {
    ch.sentric.URL currentUrl;

    try {
      do{
        currentUrl = this.pagesToVisit.remove(0);
      }while((visitedURLs.contains(currentUrl.getNormalizedUrl())));

      if(!visitedURLs.contains(currentUrl.getNormalizedUrl())) {
        LinksCollector leg = new LinksCollector();
        boolean success = leg.crawl(currentUrl, j, hostName);
        if (success) {
          j++;
          this.pagesToVisit.addAll(leg.getLinks());
          this.pagesVisited.add(currentUrl.getNormalizedUrl());
          visitedURLs.add(currentUrl.getNormalizedUrl());
        }
      }
      }catch(Exception e){
      e.printStackTrace();
    }
  }

  System.out.println("\nDone: Visited " + this.pagesVisited.size() + " web page(s) per website.");
  return j;
  }
}
