package edu.nyu.cs.cs2580;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 * ["new york city"], the presence of the phrase "new york city" must be
 * recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

  public QueryPhrase(String query) {
    super(query);
  }

  @Override
  public void processQuery() {
    if(_query == null) {
      _query = "";
      return;
    }
    Pattern p = Pattern.compile("\"([^\"]*)\"");
    Matcher m = p.matcher(_query);
    while (m.find()) {
      _tokens.add(m.group(1));
    }
  }
}
