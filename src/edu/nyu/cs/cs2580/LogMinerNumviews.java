package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;

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
    File[] files;

    f = new File("/Users/mansivirani/websearchenginesnyu/data/wiki");
    files = f.listFiles();
    String fileName;

    //Store all document names in the corpus to the map and set their numviews to 0
    for (File file : files) {

      if (!file.isDirectory() && !file.isHidden()) {
        fileName = file.getAbsoluteFile().getName();
        docNames.put(fileName, 0);

      }
    }


    FileInputStream fis = new FileInputStream("/Users/mansivirani/websearchenginesnyu/data/log/20160601-160000.log");
    Scanner sc = new Scanner(fis);



    //Write the doc name and count to file repeat above step.

    /*
    ** Run through the files in the log, decode them and for each file check if it's name exists in the map,
    ** if it exists, then increment its numviews to the count from the log
    */

    while (sc.hasNextLine()) {

      String line = sc.nextLine();
      String[] tokens = line.split(" ");

      try {

        String article = tokens[1];
        article = article.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
        article = article.replaceAll("\\+", "%2B");
        article = java.net.URLDecoder.decode(article, "UTF-8");

        //article = article.trim();
        //article = article.replaceAll("\\s", "_");

        int numViews = Integer.parseInt(tokens[2]);

        if(docNames.containsKey(article))
          docNames.put(article, docNames.get(article) + numViews);

      } catch (Exception e2) {
        e2.printStackTrace();

      }
    }

    File newFile = new File("/Users/mansivirani/websearchenginesnyu/data/docsandnumviews");

    // if file doesn't exist, then create it
    if (!newFile.exists()) {
      newFile.createNewFile();
    }

    FileWriter fw = new FileWriter(newFile.getAbsoluteFile());
    BufferedWriter bw = new BufferedWriter(fw);

    //writing filenames with numviews other than 0 to the new file
    for (String docname : docNames.keySet()) {
      if(docNames.get(docname) != 0)
        bw.write(docname + "\t" + docNames.get(docname).toString() + "\n");

    }

    fis.close();
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


    return null;

    //Load all values from the new file and put into a hashmap back in a way that indexer can use it
  }
}
