package edu.nyu.cs.cs2580;

import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 * ["new york city"], the presence of the phrase "new york city" must be
 * recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

  public Vector<QueryToken> _tokens = new Vector<>();

  public QueryPhrase(String query) {
    super(query);
  }

  @Override
  public void processQuery() {
    if(_query == null) {
      _query = "";
      return;
    }
    _query = _query.trim();
    StringBuilder sb = new StringBuilder();
    boolean ignore = false;
    for(int i = 0; i < _query.length(); i++) {
      if(_query.charAt(i) == '"') {
        sb.append(' ');
        ignore = !ignore;
      }
      else if(!ignore) {
        sb.append(_query.charAt(i));
      }
    }

    if(ignore) {
      sb.append(_query.substring(_query.lastIndexOf('"') + 1, _query.length()));
    }

    Scanner s = new Scanner(sb.toString());
    while (s.hasNext()) {
      _tokens.add(new QueryToken(false, s.next()));
    }

    s.close();

    Pattern p = Pattern.compile("\"([^\"]*)\"");
    Matcher m = p.matcher(_query);
    while (m.find()) {
      _tokens.add(new QueryToken(true, m.group(1)));
    }
  }
}
