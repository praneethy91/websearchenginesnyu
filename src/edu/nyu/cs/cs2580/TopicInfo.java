package edu.nyu.cs.cs2580;

/**
 * Created by Praneeth on 12/14/2016.
 */
public class TopicInfo {
  private String _topic;
  private double _polarity;

  public String getTopic() {
    return _topic;
  }

  public double getPolarity() {
    return _polarity;
  }

  public void setTopic(String topic) {
    _topic = topic;
  }

  public void setPolarity(double polarity) {
    _polarity = polarity;
  }
}
