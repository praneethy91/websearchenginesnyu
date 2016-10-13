package edu.nyu.cs.cs2580;

/**
 * Document with score.
 * 
 * @author fdiaz
 * @author congyu
 */
class ScoredDocument implements Comparable<ScoredDocument> {
  private String _query;
  private Document _doc;
  private double _score;

  public ScoredDocument(String query, Document doc, double score) {
    _query = query;
    _doc = doc;
    _score = score;
  }

  public String asTextResult() {
    StringBuffer buf = new StringBuffer();
    buf.append(_query).append("\t");
    buf.append(_doc._docid).append("\t");
    buf.append(_doc.getTitle()).append("\t");
    buf.append(_score);
    return buf.toString();
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
