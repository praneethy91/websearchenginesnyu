package edu.nyu.cs.cs2580;

/**
 * Created by sankethpurwar on 10/13/16.
 */

public enum LabelGainEnum{

    PERFECT(10), EXCELLENT(7), GOOD(5), FAIR(1), BAD(0);
    int gain;

    private LabelGainEnum(int gain) {
        this.gain = gain;
    }
}
