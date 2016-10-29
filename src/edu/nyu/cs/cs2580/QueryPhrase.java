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

  /*public static void main(String[] args) {
    QueryPhrase q = new QueryPhrase("CHoRus \"Hello World\"WHat");
    q.processQuery();
    for(QueryToken qt: q._tokens) {
      System.out.println(qt.getToken());
    }
  }*/

  public QueryPhrase(String query) {
    super(query);
  }

  @Override
  public void processQuery() {
    if(_query == null) {
      _query = "";
      return;
    }
    _query = _query.trim().toLowerCase();
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
      String token = s.next();
      NormalizeToken(token);
    }

    s.close();

    Pattern p = Pattern.compile("\"([^\"]*)\"");
    Matcher m = p.matcher(_query);
    while (m.find()) {
      String token = m.group(1);
      NormalizeToken(token);
    }
  }

  private void NormalizeToken(String token) {
    String[] trimmedTokens = token.split("[\\p{Punct}\\s]+");
    for(String trimmedToken : trimmedTokens) {
      trimmedToken = trimmedToken.trim();
      if (!trimmedToken.equals("") && trimmedToken.length() > 1) {
        String finalToken = Stemmer.StemToken(trimmedToken);
        if (!finalToken.equals("")) {
          _tokens.add(new QueryToken(false, finalToken));
        }
      }
    }
  }


}
