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
public abstract class Query {
  public String _query = null;

  public Vector<QueryToken> _tokens = new Vector<>();

  public Query(String query) {
    _query = query;
  }

  public abstract void processQuery();
}
