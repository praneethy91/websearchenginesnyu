package edu.nyu.cs.cs2580;

import java.util.Scanner;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * Representation of a user query.
 * 
 * In HW1: instructors provide this simple implementation.
 * 
 * In HW2: students must implement {@link QueryPhrase} to handle phrases.
 * 
 * @author congyu
 * @auhtor fdiaz
 */
public class Query {
  public String _query = null;
  public Vector<String> _tokens = new Vector<String>();

  public Query(String query) {
    _query = query;
  }

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

    if (_query.equals("")) {
      return;
    }

    Scanner s = new Scanner(sb.toString());
    while (s.hasNext()) {
      _tokens.add(s.next());
    }

    s.close();
  }
}
