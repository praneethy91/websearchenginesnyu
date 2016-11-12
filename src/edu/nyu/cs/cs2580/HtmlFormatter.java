package edu.nyu.cs.cs2580;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Vector;

/**
 * Created by Praneeth on 10/16/2016.
 */
public class HtmlFormatter {
  String _htmltemplate = "<table id=\"%s\" border=\"1\" cellpadding=\"5\" cellspacing=\"5\">" +
                          "<tr>" +
                            "<th>Rank</th>" +
                            "<th>Query</th>" +
                            "<th>Doc ID</th>" +
                            "<th>Doc Title</th>" +
                            "<th>Doc Score</th>" +
                            "<th>Doc URL</th>" +
                          "</tr>" +
                        "</table>";
  private Document _htmlDocument = null;
  private int _tableCount = 1;
  public HtmlFormatter(){
    _htmlDocument = Document.createShell("");
  }

  public void AddTable(Query query, Vector<ScoredDocument> scoredDocumentVector, QueryHandler.CgiArguments.RankerType rankerType) {
    _htmlDocument.body().appendElement("h1").text(rankerType.getDesc());
    _htmlDocument.body().append(getHtmlTemplate());
    Elements table = _htmlDocument.select("#" + _tableCount);
    int rank = 1;
    for(ScoredDocument scoredDocument : scoredDocumentVector) {
      table.append("<tr><td>" + rank + "</td><td>" + query._query + "</td><td>" + scoredDocument.getID() + "</td><td>" + scoredDocument.getTitle() + "</td><td>" + scoredDocument.getScore() + "</td><td>" + scoredDocument.getUrl() + "</td></tr>");
      rank++;
    }
    _tableCount++;
    _htmlDocument.body().append("<hr>");
    _htmlDocument.body().append("<br>");
  }

  public String asHtmlString() {
    return _htmlDocument.html();
  }

  private String getHtmlTemplate() {
    return String.format(_htmltemplate, _tableCount);
  }
}
