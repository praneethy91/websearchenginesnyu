package edu.nyu.cs.cs2580;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Created by Praneeth on 10/16/2016.
 */
public class HtmlFormatter {

  private String _docTemplate = "<div style = \"color:lightgrey\"id=\"%s\"><a href=\"%s\">%s</a></div>";
  private String _categoryTemplate = "<div id=\"%s\"><h2>%s</h2><hr></div>";
  private Document _htmlDocument = null;
  private int _docCount = 0;
  private HashMap<String, Vector<ScoredDocument>> categoryMap = new HashMap<String, Vector<ScoredDocument>>();
  public HtmlFormatter(){
    _htmlDocument = Document.createShell("");
  }

  public void AddTable(Vector<ScoredDocument> scoredDocumentVector, QueryHandler.CgiArguments.RankerType rankerType) {
    _htmlDocument.body().appendElement("h1").text("Top results..");

    for(ScoredDocument scoredDocument : scoredDocumentVector) {
      for(String category : scoredDocument.getCategories()) {
        Vector<ScoredDocument> scoredDocuments;
        if(!categoryMap.containsKey(category)) {
           scoredDocuments = new Vector<ScoredDocument>();
        }
        else {
          scoredDocuments = categoryMap.get(category);
        }

        scoredDocuments.add(scoredDocument);
        categoryMap.put(category, scoredDocuments);
      }
    }

    for(Map.Entry<String, Vector<ScoredDocument>> entry : categoryMap.entrySet()){
      String category = entry.getKey();
      String categoryTemplateInstance =  getCategoryTemplate(category);
      _htmlDocument.body().append(categoryTemplateInstance);
      Element categoryDiv = _htmlDocument.select("#" + category).first();
      for(ScoredDocument scoredDocument: entry.getValue()) {
        categoryDiv.append(getDocTemplate(scoredDocument));
        _docCount++;
      }

      _htmlDocument.body().append("<hr style=height:2px;>");
      _htmlDocument.body().append("<br>");
    }

  }

  public String asHtmlString() {
    return _htmlDocument.html();
  }

  private String getDocTemplate(ScoredDocument scoredDocument) {
    return String.format(_docTemplate, _docCount, scoredDocument.getInternetUrl(), scoredDocument.getTitle());
  }

  private String getCategoryTemplate(String category) {
    return String.format(_categoryTemplate, category, category);
  }

}
