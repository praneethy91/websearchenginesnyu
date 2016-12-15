package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sankethpurwar on 12/15/16.
 */
public class TopicReader {

    String topicIndexFile = "/file_topics";

    List<TopicInfo> getTopicInfo(SearchEngine.Options options, String filename) {
        List<TopicInfo> topics = new ArrayList<>();


        // Open the file
        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream(options._indexPrefix + topicIndexFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        String strLine;

        try {
            while ((strLine = br.readLine()) != null) {
                String[] tokens = strLine.split(";");
                if(tokens.length > 0 && tokens[0].equalsIgnoreCase(filename)){
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
