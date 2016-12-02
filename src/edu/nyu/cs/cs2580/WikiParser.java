package edu.nyu.cs.cs2580;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Created by Praneeth on 10/25/2016.
 */
public class WikiParser {

  public static void main(String[] args) throws IOException {
    /*File dir = new File("C:\\Users\\Praneeth\\Documents\\Git\\websearchenginesnyu\\data\\wiki");
    File[] directoryListing = dir.listFiles();
    System.out.println(directoryListing[0].getAbsolutePath());
    WikiParser htmlParser = new WikiParser(directoryListing[0]);
    Vector<String> tokens = htmlParser.ParseTokens();
    for(String token: tokens) {
      System.out.println(token);
    }*/
    // writing string to a file encoded as modified UTF-8
    DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("C:\\Users\\Praneeth\\Desktop\\dos")));
    dataOut.writeUTF("hello");
    for(int i = 0; i < 1024*1024*1024; i++) {
      dataOut.writeByte((byte)100);
    }
    dataOut.flush();
    dataOut.close();

    // Reading data from the same file
    DataInputStream dataIn = new DataInputStream(new FileInputStream("C:\\Users\\Praneeth\\Desktop\\dos"));
    dataIn.close();
  }

  private org.jsoup.nodes.Document _htmlDocument;

  private String _url;

  private String _title;

  public WikiParser(File htmlDocument) throws IOException, IllegalArgumentException {
    _htmlDocument = org.jsoup.Jsoup.parse(htmlDocument, "UTF-8");
    InitiateParser();
  }

  public WikiParser(File url, boolean h) throws IOException {
    _htmlDocument = org.jsoup.Jsoup.parse(url, "UTF-8");
    _title = _htmlDocument.title();
    _url = url.toString();
  }

  private void InitiateParser() {
    _title = _htmlDocument.title();
    if (_title == null || _title.equals("")) {

      // Not a well formed wiki article
      throw new IllegalArgumentException();
    }

    _url = _htmlDocument.select("div.printFooter a").first().ownText();

    int wikiIndex = _title.indexOf("- Wikipedia");
    if (wikiIndex != -1) {
      _title = _title.substring(0, wikiIndex).trim();
    }
  }

  public String getTitle() {
    return _title;
  }

  public String getUrl() {
    return _url;
  }

  public Vector<String> ParseGeneralTokens() {
    Vector<String> tokens = new Vector<>();
    Element contentElement = _htmlDocument.body();
    DFSGeneralParse(contentElement, tokens);
    return tokens;
  }

  public Vector<String> ParseTokens() {
    Vector<String> tokens = new Vector<>();
    Parse(tokens);
    return tokens;
  }

  public Set<String> ParseTokensNoDuplicates() {
    HashSet<String> tokens = new HashSet<>();
    Parse(tokens);
    return tokens;
  }

  private void Parse(Collection<String> tokens) {
    Element contentElement = _htmlDocument.select("div#content").first();
    DFSWikiParse(contentElement, tokens);
  }

  private void DFSWikiParse(Element element, Collection<String> tokens) {
    Element elementParent = element.parent();

    if(elementParent.attr("class").equals("references")) {
      return;
    }

    if(elementParent.attr("id").equals("siteSub")) {
      return;
    }

    Parse(element, tokens);

    for(Element childrenElements: element.children()) {
      DFSWikiParse(childrenElements, tokens);
    }
  }

  private void Parse(Element element, Collection<String> tokens) {
    String[] tokensArr = element.ownText().toLowerCase().split("[\\p{Punct}\\s]+");
    for(String token : tokensArr) {
      String trimmedToken = token.trim();
      if(!trimmedToken.equals("") && trimmedToken.length() > 1) {
        String finalToken = Stemmer.StemToken(trimmedToken);
        if(!finalToken.equals("") && finalToken.matches("^[a-zA-Z0-9]*$")) {
          tokens.add(finalToken);
        }
      }
    }
  }

  private void DFSGeneralParse(Element element, Collection<String> tokens) {
    Parse(element, tokens);
    for(Element childrenElements: element.children()) {
      DFSGeneralParse(childrenElements, tokens);
    }
  }
}