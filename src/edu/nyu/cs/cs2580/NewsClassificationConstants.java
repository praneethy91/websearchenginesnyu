package edu.nyu.cs.cs2580;

/**
 * Created by Praneeth on 12/11/2016.
 */
public class NewsClassificationConstants {
  public static final String modelDir = "data/model";
  public static final String filesToTrainModelDir =  "data/news";
  public static final String filesToRankDir =  "data/HTMLDocs";
  public static final String totalDocsKey = "totalDocsInTrainingCorpus";
  public static final String termToIntFile = modelDir + "/termToInt";
  public static final String termToNumDocsFile = modelDir + "/termToNumDocs";
  public static final String newsFileToURLFile = modelDir + "/newsFileToURL";

  public static final String[] newsCategories = {"travel", "entertainment", "business", "food", "health", "politics", "science", "sports", "technology"};
  public static final String[] newsCompanies = {"fox", "nytimes"};

  public static final String _corpusFilePrefix = "File-";
}