package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class LogMinerNumviews extends LogMiner {

  public LogMinerNumviews(Options options) throws IOException {
    super(options);
  }

  /**
   * This function processes the logs within the log directory as specified by
   * the _options. The logs are obtained from Wikipedia dumps and have
   * the following format per line: [language]<space>[article]<space>[#views].
   * Those view information are to be extracted for documents in our corpus and
   * stored somewhere to be used during indexing.
   *
   * Note that the log contains view information for all articles in Wikipedia
   * and it is necessary to locate the information about articles within our
   * corpus.
   *
   * @throws IOException
   */
  @Override
  public void compute() throws IOException {
    System.out.println("Computing using " + this.getClass().getName());

    HashMap<String, Integer> docNames = new HashMap<>();

    File f;
    f = new File(_options._corpusPrefix);

    File[] files;
    files = f.listFiles();
    String fileName;

    //Store all document names present in the corpus to the map and set their numviews to 0
    for (File file : files) {

      if (!file.isDirectory() && !file.isHidden()) {
        fileName = file.getAbsoluteFile().getName();
        docNames.put(fileName, 0);

      }
    }

    File log = new File(_options._logPrefix);
    File[] logFiles;
    logFiles = log.listFiles();

    File newFile = new File(_options._indexPrefix+ "/numViewsIndex.tsv");

    // if file doesn't exist, then create it
    if (!newFile.exists()) {
      newFile.createNewFile();
    }

    FileWriter fw = new FileWriter(newFile.getAbsoluteFile());
    BufferedWriter bw = new BufferedWriter(fw);



    for (File file: logFiles) {

        if (!file.isDirectory() && !file.isHidden()) {

          BufferedReader br2 = new BufferedReader(new FileReader(file));
          String line;
          String[] tokens;
          String integerRegex = "([0-9]{0,9})";

          while ((line = br2.readLine()) != null) {
            tokens = line.split(" ");

            //Checking if line is formatted properly
            if(tokens.length < 3) {
              continue;
            }

            //Checking if the number of views is an Integer
            String article = tokens[1];
            int numViews;
            try {
               numViews = Integer.parseInt(tokens[2]);
            }
            catch(NumberFormatException ex) {
              continue;
            }

            article = article.replaceAll("\\+", "%2B");
            article = article.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
            article = java.net.URLDecoder.decode(article, "UTF-8");
            article = article.trim();

            if (docNames.containsKey(article))
              docNames.put(article, docNames.get(article) + numViews);
          }
        }
      }

    for (String docname: docNames.keySet()) {

      if(docNames.get(docname) != 0)
        bw.write(docname + "\t" + docNames.get(docname).toString() + "\n");

    }

    bw.close();

  }


  /**
   * During indexing mode, this function loads the NumViews values computed
   * during mining mode to be used by the indexer.
   * 
   * @throws IOException
   */
  @Override
  public Object load() throws IOException {
    System.out.println("Loading using " + this.getClass().getName());
    BufferedReader br = new BufferedReader(new FileReader(_options._indexPrefix+ "/numViewsIndex.tsv"));

    HashMap<String, Integer> numViewIndex = new HashMap<>();
    String line;
    String tokens[];

    while ((line = br.readLine()) != null){
      tokens = line.split("\t");
      numViewIndex.put(tokens[0], Integer.parseInt(tokens[1]));

    }

    return numViewIndex;

    //Load all values from the new file and put into a hashmap back in a way that indexer can use it
  }


}
