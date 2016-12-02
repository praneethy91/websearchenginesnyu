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
        private static final int MAX_PAGES_TO_SEARCH = 5;
        private Set<String> pagesVisited = new HashSet<String>();
        private LinkedList<String> pagesToVisit = new LinkedList<String>();


        /**
         * Our main launching point for the Spider's functionality. Internally it creates spider legs
         * that make an HTTP request and parse the response (the web page).
         *
         * @param
         *            - The starting point of the spider
         */

    public int search(URL urlFormatted, int j) throws IOException {
        String url = urlFormatted.toString();
        String hostName = urlFormatted.getHost();
        this.pagesToVisit.addFirst(url);


        while (pagesVisited.size() < MAX_PAGES_TO_SEARCH) {
            String currentUrl;

            try {
                do {
                    currentUrl = this.pagesToVisit.remove(0);
                } while (this.pagesVisited.contains(currentUrl));


                LinksCollector leg = new LinksCollector();
                boolean success = leg.crawl(currentUrl, j, hostName); // Lots of stuff happening here. Look at the crawl method in
                if (success) {
                    j++;
                    this.pagesToVisit.addAll(leg.getLinks());
                    this.pagesVisited.add(currentUrl);
                }

            }catch (Exception e){
                System.out.print("Some exception");
            }
        }

        System.out.println("\nDone: Visited " + this.pagesVisited.size() + " web page(s) per website.");
        return j;
    }
}
