package edu.nyu.cs.cs2580;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 * information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document implements Cloneable {
  private static final long serialVersionUID = 9184892508124423115L;

  Map<QueryToken, Integer> quertTokenCount = new HashMap<>();

  Integer totalNumberOfTokensInDoc = 0;

  public DocumentIndexed(int docid) {
    super(docid);
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    DocumentIndexed cloned = (DocumentIndexed)super.clone();
    cloned.quertTokenCount = new HashMap<>();
    for(Map.Entry<QueryToken, Integer> entry : this.quertTokenCount.entrySet()) {
      cloned.quertTokenCount.put(new QueryToken(entry.getKey().isPhrase(), entry.getKey().getToken()), entry.getValue());
    }
    return cloned;
  }
}