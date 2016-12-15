package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Document with score.
 *
 * @author fdiaz
 * @author congyu
 */
class ScoredDocument implements Comparable<ScoredDocument> {
  private Document _doc;
  private double _score;
  private String _url;
  private String _internetURL;
  private Collection<String> _categories;
  private double _pageRank = 0.0;
  private double _numViews = 0.0;

  public Collection<TopicInfo> _topics;

  public ScoredDocument(Document doc, double score) {
    _doc = doc;
    _score = score;
    _url = doc.getUrl();
  }

  public String asTextResult() {
    StringBuffer buf = new StringBuffer();
    buf.append(_doc._docid).append("\t");
    buf.append("(").append(Join(",", _categories)).append(")").append("\t");
    buf.append(_doc.getTitle()).append("\t");
    buf.append(_score).append("\t");
    buf.append(_pageRank).append("\t");
    buf.append(_numViews).append("\t");
    buf.append(_doc.getUrl());
    return buf.toString();
  }

  public double getScore() {
    return _score;
  }

  public void setScore(double score) {
    _score = score;
  }

  public double getPageRank() {
    return _pageRank;
  }

  public void setPageRank(double pageRank) {
    _pageRank = pageRank;
  }

  public double getNumViews() {
    return _numViews;
  }

  public void setNumViews(double numViews) {
    _numViews = numViews;
  }

  public String getTitle() {
    return _doc.getTitle();
  }

  public int getID() {
    return _doc._docid;
  }

  public String getUrl() {
    return _url;
  }

  public void setInternetUrl(String internetURL) {
     _internetURL = internetURL;
  }

  public String getInternetUrl() {
    return _internetURL;
  }

  public void setCategories(Collection<String> categories) {
    _categories = categories;
  }

  public Collection<String> getCategories() {
    return _categories;
  }

  public List<TopicInfo> getTopics(){
    TopicInfo topicInfo1 = new TopicInfo();
    topicInfo1.setPolarity(0.3);
    topicInfo1.setTopic("Topic1");

    TopicInfo topicInfo2 = new TopicInfo();
    topicInfo2.setPolarity(0.4);
    topicInfo2.setTopic("Topic2");

    TopicInfo topicInfo3 = new TopicInfo();
    topicInfo3.setPolarity(0.05);
    topicInfo3.setTopic("Topic3");

    List<TopicInfo> topicInfos = new ArrayList<TopicInfo>();
    topicInfos.add(topicInfo1);
    topicInfos.add(topicInfo2);
    topicInfos.add(topicInfo3);

    return topicInfos;
    // TODO: add the below code back
    // return _topics;
  }

  public void setTopics(Collection<TopicInfo> topics) {
    _topics = topics;
  }

  /**
   * @CS2580: Student should implement {@code asHtmlResult} for final project.
   */
  public String asHtmlResult() {
    return "";
  }

  @Override
  public int compareTo(ScoredDocument o) {
    if (this._score == o._score) {
      return 0;
    }
    return (this._score > o._score) ? 1 : -1;
  }

  private String Join(String seperator, Collection<String> strings) {
    StringBuilder sb = new StringBuilder();
    for(String string: strings) {
      sb.append(string);
      sb.append(seperator);
    }
    sb.setLength(sb.length() -1);
    return sb.toString();
  }
}