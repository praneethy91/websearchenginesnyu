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

  public String getRGB() {
    RedGreenBlue rgb = getColor();
    return String.format("rgb(%s,%s,%s)", rgb.red, rgb.green, rgb.blue);
  }

  @Override
  public String toString() {
    if(_topic.equals("")) {
      return "";
    }

    return String.format("%s: %.2f", getTopic(), getPolarity());
  }

  private RedGreenBlue getColor() {
    /*RedGreenBlue rgb = new RedGreenBlue();
    rgb.red = 255;
    rgb.green = 255;
    rgb.blue = 0;
    if(_polarity >= 0 && _polarity <= 0.5) {
      int redToDecrease = (int)Math.floor((_polarity/0.5) *255);
      rgb.red = rgb.red - redToDecrease;
    }
    else if(_polarity < 0 && _polarity >=-0.5) {
      int greenToDecrease = (int)Math.floor((-_polarity/0.5) *255);
      rgb.green = rgb.green - greenToDecrease;
    }
    else if(_polarity > 0.5) {
      rgb = new RedGreenBlue();
      rgb.red = 0;
      rgb.green = 255;
      rgb.blue = 0;
    }
    else {
      rgb = new RedGreenBlue();
      rgb.red = 255;
      rgb.green = 0;
      rgb.blue = 0;
    }

    return rgb;*/

    RedGreenBlue rgb = new RedGreenBlue();
    rgb.red = 255;
    rgb.green = 255;
    rgb.blue = 0;
    if(_polarity >= 0.5 && _polarity <= 1) {
      int redToDecrease = (int)Math.floor((_polarity - 0.5)/0.5*255);
      rgb.red = rgb.red - redToDecrease;
    }
    else if(_polarity < 0.5 && _polarity >=0) {
      int greenToDecrease = (int)Math.floor((0.5-_polarity)/0.5*255);
      rgb.green = rgb.green - greenToDecrease;
    }
    else if(_polarity > 1) {
      rgb = new RedGreenBlue();
      rgb.red = 0;
      rgb.green = 255;
      rgb.blue = 0;
    }
    else {
      rgb = new RedGreenBlue();
      rgb.red = 255;
      rgb.green = 0;
      rgb.blue = 0;
    }

    return rgb;
  }
}
