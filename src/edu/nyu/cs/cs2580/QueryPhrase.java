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

  private StopWords stopWords;

  @Override
  public void processQuery() {
    stopWords = new StopWords();
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
      NormalizeToken(token, false);
    }

    s.close();

    Pattern p = Pattern.compile("\"([^\"]*)\"");
    Matcher m = p.matcher(_query);
    while (m.find()) {
      String token = m.group(1);
      NormalizeToken(token, true);
    }
  }

  private void NormalizeToken(String token, boolean combine) {
    String[] trimmedTokens = token.split("[\\p{Punct}\\s]+");
    for(int i = 0; i < trimmedTokens.length; i++) {
      String trimmedToken = trimmedTokens[i].trim();
      if (!stopWords.contains(trimmedToken) && !trimmedToken.equals("") && trimmedToken.length() > 1) {
        String finalToken = Stemmer.StemToken(trimmedToken);
        if (!finalToken.equals("") && finalToken.matches("^[a-zA-Z0-9]*$")) {
          if(!combine) {
            _tokens.add(new QueryToken(false, finalToken));
          }
          else {
            trimmedTokens[i] = finalToken;
          }
        }
      }
      else {
        trimmedTokens[i] = "";
      }
    }
    if(combine) {
      StringBuilder sb = new StringBuilder();
      for(String trimmedToken: trimmedTokens) {
        if(!trimmedToken.equals("")) {
          sb.append(trimmedToken).append(" ");
        }
      }

      if(sb.length() > 1) {
        sb.setLength(sb.length() - 1);
        _tokens.add(new QueryToken(true, sb.toString()));
      }
    }
  }


}