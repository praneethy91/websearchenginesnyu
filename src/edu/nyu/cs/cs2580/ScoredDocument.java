package edu.nyu.cs.cs2580;

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

  public ScoredDocument(Document doc, double score) {
    _doc = doc;
    _score = score;
    _url = doc.getUrl();
  }

  public String asTextResult() {
    StringBuffer buf = new StringBuffer();
    buf.append(_doc._docid).append("\t");
    buf.append(_doc.getTitle()).append("\t");
    buf.append(_score).append("\t");
    buf.append(_doc.getUrl());
    return buf.toString();
  }

  public double getScore() {
    return _score;
  }

  public void setScore(double score) {
    _score = score;
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
}