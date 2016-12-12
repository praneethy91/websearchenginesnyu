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
        private LinkedList<String> pagesToVisit = new LinkedList<String>();


    public int search(URL urlFormatted, int j, Set<String> visitedURLs) throws IOException {
        String url = urlFormatted.toString();
        String hostName = urlFormatted.getHost();
        this.pagesToVisit.addFirst(url);


        while (pagesVisited.size() < MAX_PAGES_TO_SEARCH ) {
            String currentUrl;
            int flag = 0;

            try {
                do {

                        currentUrl = this.pagesToVisit.remove(0);


                } while ((this.pagesVisited.contains(currentUrl)));


                if (!visitedURLs.contains(currentUrl)) {

                    LinksCollector leg = new LinksCollector();
                    boolean success = leg.crawl(currentUrl, j, hostName);
                    if (success) {
                        j++;
                        this.pagesToVisit.addAll(leg.getLinks());
                        this.pagesVisited.add(currentUrl);
                        visitedURLs.add(currentUrl);

                    }

                }
                }catch(Exception e){
                System.out.print("Some exception");
            }
        }

        System.out.println("\nDone: Visited " + this.pagesVisited.size() + " web page(s) per website.");
        return j;
    }
}
