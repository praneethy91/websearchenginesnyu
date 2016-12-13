package edu.nyu.cs.cs2580; /**
 * Created by mansivirani on 30/11/16.
 */


import ch.sentric.URL;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class LinksCollector {
// We'll use a fake USER_AGENT so the web server thinks the robot is a normal web browser.
private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";
private List<URL> links = new LinkedList<URL>();
private Document htmlDocument;

public boolean crawl(URL url, int j, String hostName) throws IOException
{
  try {
    Connection connection = Jsoup.connect(url.toString()).userAgent(USER_AGENT);
    Document htmlDocument = connection.get();
    this.htmlDocument = htmlDocument;
    Connection.Response response = connection.response();

    if (response.statusCode() == 200) // 200 is the HTTP OK status code
      try {
        String host = response.url().getHost();
        if (!connection.response().contentType().contains("text/html") || connection.response().contentType().equals(null)) {
          //System.out.print("This is not an HTML Doc");
          return false;
        }
        else if (!host.equals(hostName)) {
          //System.out.println("Failure: Retrieved irrelevant URL " + url);
          return false;
        }
        else {
          System.out.println("\nVisiting: " + url);
          String docBodyText = this.htmlDocument.html();
          File newFile = new File(NewsClassificationConstants.filesToRankDir + "/" + NewsClassificationConstants._corpusFilePrefix + j);
          FileWriter fw = new FileWriter(newFile.getAbsoluteFile(), false);
          BufferedWriter bw = new BufferedWriter(fw);
          bw.write(docBodyText);
          bw.close();
        }
      }
      catch (Exception e){
        e.printStackTrace();
      }
  Elements linksOnPage = htmlDocument.select("a[href]");
  System.out.println("Found (" + linksOnPage.size() + ") links");
  for(Element link : linksOnPage) {
    try {
      this.links.add(new URL(link.absUrl("href")));
    }
    catch(Exception e) {
      //Ignore malformed URL's
    }
  }

  return true;
  } catch(IOException ioe) {
    // We were not successful in our HTTP request
    return false;
  } catch(IllegalArgumentException iae) {
    // We were not successful in our HTTP request
    return false;
  }
}
  public List<URL> getLinks() {
        return this.links;
  }
}
