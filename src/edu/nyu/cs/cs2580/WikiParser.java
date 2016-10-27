package edu.nyu.cs.cs2580;
import com.sun.javaws.exceptions.InvalidArgumentException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
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

  private String _url;

  private String _title;

  private HashSet<String> _tokens;

  public WikiParser(File htmlDocument) throws IOException, IllegalArgumentException {
    _htmlDocument = Jsoup.parse(htmlDocument, "UTF-8");
    _title = _htmlDocument.title();
    if(_title == null || _title.equals("")) {

      // Not a well formed wiki article
      throw new IllegalArgumentException();
    }
    int wikiIndex = _title.indexOf("- Wikipedia");
    if(wikiIndex != -1) {
      _title = _title.substring(0, wikiIndex).trim();
    }
    _tokens = new HashSet<>();
  }

  public String getTitle() {
    return _title;
  }

  public String getUrl() {
    return _url;
  }

  public Vector<String> ParseTokens() {
    Elements elements = _htmlDocument.select("*");

    for (Element element : elements) {
      Element elementParent = element.parent();
      if(elementParent != null && elementParent.attr("class").equals("printfooter")) {
        _url = element.ownText();
      }

      String[] tokensArr = element.ownText().toLowerCase().split("[\\p{Punct}\\s]+");
      for(String token : tokensArr) {
        String trimToken = token.trim();
        if(!trimToken.equals("")) {
          _tokens.add(trimToken);
        }
      }

    }

    return new Vector<>(_tokens);
  }
}