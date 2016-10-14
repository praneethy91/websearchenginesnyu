package edu.nyu.cs.cs2580;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

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
  public Map<String, Integer> _tokenCountMap = new HashMap<>();

  public Query(String query) {
    _query = query;
  }

  public void processQuery() {
    if (_query == null) {
      return;
    }
    Scanner s = new Scanner(_query);
    while (s.hasNext()) {
      String token = s.next();
      _tokens.add(token);
      if(_tokenCountMap.containsKey(token)) {
        _tokenCountMap.put(token, _tokenCountMap.get(token) + 1);
      }
      else {
        _tokenCountMap.put(token, 1);
      }
    }
    s.close();
  }
}
