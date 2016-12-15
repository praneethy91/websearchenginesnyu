package edu.nyu.cs.cs2580;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.io.File;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Created by Praneeth on 10/16/2016.
 */
public class HtmlFormatter {

  private static final String ContainerID = "containerID";
  private String _docTemplate = "<div class=\"row\"style = \"color:black\"id=\"%s\"><div class=\"col-md-offset-2 col-md-8\">" +
          "<a href=\"%s\">%s</a></div>" +
          "<div class=\"col-md-offset-2 col-md-2\" style=\"background-color:%s;\">%s</div>" +
          "<div class=\"col-md-offset-1 col-md-2\" style=\"background-color:%s;\">%s</div>" +
          "<div class=\"col-md-offset-1 col-md-2\" style=\"background-color:%s;\">%s</div></div>" +
          "<div class=\"row\"><br class=\"col-md-offset-2 col-md-8\"></div>";
  private String _categoryTemplate = "<div class=\"row\" id=\"%s\"><h2 class=\"col-md-offset-2 col-md-8\">%s</h2><hr class=\"col-md-offset-2 col-md-8\"></div>";
  private Document _htmlDocument = null;
  private int _docCount = 0;
  private HashMap<String, Vector<ScoredDocument>> categoryMap = new HashMap<String, Vector<ScoredDocument>>();
  public HtmlFormatter(){
    _htmlDocument = Document.createShell("");

    //Google fonts
    _htmlDocument.head().append("<link href=\"https://fonts.googleapis.com/css?family=Lato:100,300\" rel=\"stylesheet\">");

    //Viewport setting
    _htmlDocument.head().append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");

    //Adding overall font-style and horizontal rule styles
    _htmlDocument.head().append("<style>" +
            "body {" +
            "    font-family: 'Lato', sans-serif;\n" +
            "    color: #676658;\n" +
            "    font-weight: 300;}" +
            ".text-thin {\n" +
            "    font-weight: 100;\n" +
            "}" +
            "hr {\n" +
            "    height: 1px;\n" +
            "    color: #676658;\n" +
            "    background-color: #676658;\n" +
            "</style>");

    //Bootstrap minified CSS
    _htmlDocument.head().append("<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\" integrity=\"sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u\" crossorigin=\"anonymous\">");

    //Jquery 1.9.1
    _htmlDocument.head().append("<script\n" +
            "  src=\"https://code.jquery.com/jquery-1.9.1.min.js\"\n" +
            "  integrity=\"sha256-wS9gmOZBqsqWxgIVgA8Y9WcQOa7PgSIX+rPA0VL2rbQ=\"\n" +
            "  crossorigin=\"anonymous\"></script>");

    //Bootstrap minified JS
    _htmlDocument.head().append("<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\" integrity=\"sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa\" crossorigin=\"anonymous\"></script>\n");
  }

  public void AddTable(Vector<ScoredDocument> scoredDocumentVector, QueryHandler.CgiArguments.RankerType rankerType) throws MalformedURLException {

    Element el = new Element(Tag.valueOf("div"), "");
    el.attr("class", "container");
    el.attr("id", ContainerID);
    _htmlDocument.body().appendChild(el);

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
      category = category.substring(0, 1).toUpperCase() + category.substring(1);
      String categoryTemplateInstance =  getCategoryTemplate(category);
      _htmlDocument.body().select("#" + ContainerID).append(categoryTemplateInstance);
      Element categoryDiv = _htmlDocument.select("#" + category).first();
      for(ScoredDocument scoredDocument: entry.getValue()) {
        categoryDiv.append(getDocTemplate(scoredDocument));
        _docCount++;
      }

      _htmlDocument.body().append("<div class=\"row\"><br class=\"col-md-offset-2 col-md-8\"></div>");
      _htmlDocument.body().append("<div class=\"row\"><br class=\"col-md-offset-2 col-md-8\"></div>");
    }

  }

  public String asHtmlString() {
    return _htmlDocument.html();
  }

  private String getDocTemplate(ScoredDocument scoredDocument) throws MalformedURLException {

    List<TopicInfo> topics = scoredDocument.getTopics();
    //TODO: Add the actual link
    TopicInfo topicInfo1 = topics.get(0);
    TopicInfo topicInfo2 = topics.get(1);
    TopicInfo topicInfo3 = topics.get(2);
    return String.format(_docTemplate, _docCount, "http://www.google.com", scoredDocument.getTitle(), topicInfo1.getRGB(), topicInfo1, topicInfo2.getRGB(), topicInfo2, topicInfo3.getRGB(), topicInfo3);
  }

  private String getCategoryTemplate(String category) {
    return String.format(_categoryTemplate, category, category);
  }
}
