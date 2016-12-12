package edu.nyu.cs.cs2580;

/**
 * Created by Praneeth on 12/11/2016.
 */
public class NewsClassificationConstants {
  public static final String modelDir = "data\\model";
  public static final String filesToTrainModelDir =  "data\\HTMLDocs";
  public static final String totalDocsKey = "totalDocsInTrainingCorpus";
  public static final String termToIntFile = "data\\model\\termToInt";
  public static final String termToNumDocsFile = "data\\model\\termToNumDocs";

  public static final String[] newsCategories = {"arts", "business", "food", "health", "politics", "science", "technology", "travel"};
  public static final String[] newsCompanies = {"fox", "nytimes"};
}
