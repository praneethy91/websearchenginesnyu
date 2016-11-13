package edu.nyu.cs.cs2580;

/**
 * Created by Praneeth on 11/12/2016.
 */
public class TermProbability {
  private String _term;
  private double _probability;
  public TermProbability(String term, double probability) {
    _term = term;
    _probability = probability;
  }

  public String getTerm() {
    return _term;
  }

  public double getProbability() {
    return _probability;
  }

  public void setProbability(double probability) {
    _probability = probability;
  }

  public String asTextResult() {
    return _term + "\t" + _probability;
  }
}
