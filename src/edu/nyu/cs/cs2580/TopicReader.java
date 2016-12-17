package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by sankethpurwar on 12/15/16.
 */
public class TopicReader {

    String topicIndexFile = "/file_topics_sentiment";
    HashMap<String, List<TopicInfo>> sentiment_index = new HashMap<>();

    TopicReader(){


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
            while ((strLine = br.readLine()) != null ) {
                String[] tokens = strLine.split(";");
                if(tokens.length > 0){
                    List<TopicInfo> topics = new ArrayList<>();

                    int i = 1;
                    while(i < tokens.length){
                        String[] polarity = tokens[i].split(":");
                        if(polarity.length > 0) {
                            TopicInfo topicInfo = new TopicInfo();
                            topicInfo.setTopic(polarity[0]);
                            topicInfo.setPolarity((new Double(polarity[1] ) * 2.5 + 1)/2);
                            topics.add(topicInfo);
                        }
                        i++;
                    }
                    sentiment_index.put(tokens[0], topics);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    List<TopicInfo> getTopicInfo(String filename) {
        return sentiment_index.get(filename);
    }
}
