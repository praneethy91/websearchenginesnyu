package edu.nyu.cs.cs2580;

/**
 * Created by Praneeth on 10/27/2016.
 */
public class QueryToken {
  private boolean _isPhrase;
  private String _token;
  public QueryToken(boolean isPhrase, String token) {
    this._isPhrase = isPhrase;
    this._token = token;
  }

  public boolean isPhrase() {
    return _isPhrase;
  }

  public String getToken() {
    return _token;
  }
}