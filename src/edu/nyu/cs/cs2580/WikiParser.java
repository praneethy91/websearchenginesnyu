package edu.nyu.cs.cs2580;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

/**
 * Created by Praneeth on 10/25/2016.
 */
public class WikiParser {

  /*public static void main(String[] args) throws IOException {
    File dir = new File("C:\\Users\\Praneeth\\Documents\\Git\\websearchenginesnyu\\data\\wiki");
    File[] directoryListing = dir.listFiles();
    System.out.println(directoryListing[0].getAbsolutePath());
    WikiParser htmlParser = new WikiParser(directoryListing[0]);
    Vector<String> tokens = htmlParser.ParseTokens();
    for(String token: tokens) {
      System.out.println(token);
    }
  }*/

  private org.jsoup.nodes.Document _htmlDocument;

  private static String _link;

  private static String _title;

  public WikiParser(File htmlDocument) throws IOException {
    _htmlDocument = Jsoup.parse(htmlDocument, "UTF-8");
    _title = _htmlDocument.title();
    _title = _title.substring(0, _title.indexOf("- Wikipedia")).trim();
  }

  public String getTitle() {
    return _title;
  }

  public String getLink() {
    return _link;
  }

  public Vector<String> ParseTokens() {
    Vector<String> tokens = new Vector<>();
    Elements elements = _htmlDocument.select("*");

    for (Element element : elements) {
      Element elementParent = element.parent();
      if(elementParent != null && elementParent.attr("class").equals("printfooter")) {
        _link = element.ownText();
      }

      String[] tokensArr = element.ownText().toLowerCase().split("[\\p{Punct}\\s]+");
      for(String token : tokensArr) {
        String trimToken = token.trim();
        if(!trimToken.equals("")) {
          tokens.add(trimToken);
        }
      }
    }

    return tokens;
  }
}