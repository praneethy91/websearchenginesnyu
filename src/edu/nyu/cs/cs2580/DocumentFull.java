package edu.nyu.cs.cs2580;

import java.util.HashMap;
import java.util.Vector;

/**
 * The full representation of the Document, which depends on the
 * {@link IndexerFullScan}. In addition to the basic information inside
 * {@link Document}, we maintain the title and body token vectors.
 *
 * @author fdiaz
 * @author congyu
 */
public class DocumentFull extends Document {
  private static final long serialVersionUID = -4093365505663362577L;

  private IndexerFullScan _indexer = null;

  private Vector<Integer> _titleTokens = new Vector<Integer>();
  private Vector<Integer> _bodyTokens = new Vector<Integer>();
  private double _normalizationFactorTfIdf = 0.0;
  private HashMap<String, Double> _tokenCountMap = new HashMap<>();

  public DocumentFull(int docid, IndexerFullScan indexer) {
    super(docid);
    _indexer = indexer;
  }

  public void setTokenCountMap(HashMap<String, Double> tokenCountMap) {
    this._tokenCountMap = tokenCountMap;
  }

  public HashMap<String, Double> getTokenCountMap() {
    return this._tokenCountMap;
  }

  public void setNormalizationFactorTfIdf(double _normalizationFactorTfIdf) {
    this._normalizationFactorTfIdf = _normalizationFactorTfIdf;
  }

  public double getNormalizationFactorTfIdf() {
    return this._normalizationFactorTfIdf;
  }

  public void setTitleTokens(Vector<Integer> titleTokens) {
    _titleTokens = titleTokens;
  }

  public Vector<Integer> getTitleTokens() {
    return _titleTokens;
  }

  public Vector<String> getConvertedTitleTokens() {
    return _indexer.getTermVector(_titleTokens);
  }

  public void setBodyTokens(Vector<Integer> bodyTokens) {
    _bodyTokens = bodyTokens;
  }

  public Vector<Integer> getBodyTokens() {
    return _bodyTokens;
  }

  public Vector<String> getConvertedBodyTokens() {
    return _indexer.getTermVector(_bodyTokens);
  }
}
