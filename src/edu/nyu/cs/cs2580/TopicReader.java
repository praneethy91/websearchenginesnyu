package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sankethpurwar on 12/15/16.
 */
public class TopicReader {

    String topicIndexFile = "/file_topics_sentiment";

    List<TopicInfo> getTopicInfo(String filename) {
        List<TopicInfo> topics = new ArrayList<>();
        boolean found = false;

        // Open the file
        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream("data" + topicIndexFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        String strLine;

        try {
            while ((strLine = br.readLine()) != null && !found) {
                String[] tokens = strLine.split(";");
                if(tokens.length > 0 && tokens[0].equalsIgnoreCase(filename)){
                    found = true;
                    int i = 1;
                    while(i < tokens.length){
                        String[] polarity = tokens[i].split(":");
                        if(polarity.length > 0) {
                            TopicInfo topicInfo = new TopicInfo();
                            topicInfo.setTopic(polarity[0]);
                            topicInfo.setPolarity(new Double(polarity[1]));
                            topics.add(topicInfo);
                        }
                        i++;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Close the input stream
        try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return topics;
    }
}
