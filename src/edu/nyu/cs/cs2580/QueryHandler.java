package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.Vector;

import com.sun.javaws.exceptions.InvalidArgumentException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * Handles each incoming query, students do not need to change this class except
 * to provide more query time CGI arguments and the HTML output.
 *
 * N.B. This class is not thread-safe.
 *
 * @author congyu
 * @author fdiaz
 */
class QueryHandler implements HttpHandler {

  /**
   * CGI arguments provided by the user through the URL. This will determine
   * which Ranker to use and what output format to adopt. For simplicity, all
   * arguments are publicly accessible.
   */
  public static class CgiArguments {
    // The raw user query
    public String _query = "";

    //
    public File _queryFile = null;
    // How many results to return
    private int _numResults = 10;
    private int _numDocs = -1;
    private int _numTerms = -1;

    // The type of the ranker we will be using.
    public enum RankerType {
      NONE("Unidentified Ranker"),
      CONJUNCTIVE("Conjunctive Ranker"),
      FAVORITE("Favorite Ranker"),
      COMPREHENSIVE("Comprehensive Ranker"),
      FULLSCAN("Naive Ranker"),
      COSINE("Cosine Similarity Ranker"),
      QL("Query Likelihood Ranker"),
      PHRASE("Phrase Ranker"),
      NUMVIEWS("Numviews Ranker"),  // This is a query-independent ranking signal
      LINEAR("Simple Linear Ranking Model");

      private String desc;
      RankerType(String desc) {
        this.desc=desc;
      }

      public String getDesc() {
        return desc;
      }
    }
    public RankerType _rankerType = RankerType.NONE;

    // The output format.
    public enum OutputFormat {
      TEXT,
      HTML,
    }

    public enum OutputType {
      HTTP,
      FILE,
    }

    public OutputFormat _outputFormat = OutputFormat.TEXT;
    public OutputType _outputType = OutputType.HTTP;

    public CgiArguments(String uriQuery) {
      String[] params = uriQuery.split("&");
      for (String param : params) {
        String[] keyval = param.split("=", 2);
        if (keyval.length < 2) {
          continue;
        }
        String key = keyval[0].toLowerCase();
        String val = keyval[1];
        if (key.equals("query")) {
          _query = val;
        } else if (key.equals("queryfile")) {
          try {
            _queryFile = new File(val);
          }
          catch(NullPointerException e) {
            //Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("num")) {
          try {
            if(val.toLowerCase().equals("all")) {
              _numResults = _indexer._numDocs;
            }
            else {
              _numResults = Integer.parseInt(val);
            }
          } catch (NumberFormatException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }

        } else if (key.equals("numdocs")) {
          try {
            _numDocs = Integer.parseInt(val);
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("numterms")) {
          try {
            _numTerms = Integer.parseInt(val);
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("ranker")) {
          try {
            _rankerType = RankerType.valueOf(val.toUpperCase());
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("format")) {
          try {
            _outputFormat = OutputFormat.valueOf(val.toUpperCase());
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("output")) {
          try {
            _outputType = OutputType.valueOf(val.toUpperCase());
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        }
      }  // End of iterating over params
    }
  }

  // For accessing the underlying documents to be used by the Ranker. Since
  // we are not worried about thread-safety here, the Indexer class must take
  // care of thread-safety.
  private static Indexer _indexer;

  public QueryHandler(Options options, Indexer indexer) {
    _indexer = indexer;
  }

  private void respondWithMsg(HttpExchange exchange, final String message)
          throws IOException {
    respond(exchange, message, "text/plain");
  }

  private void respondWithHtml(HttpExchange exchange, final String html)
          throws IOException {
    respond(exchange, html, "text/html");
  }

  private void respond(HttpExchange exchange, final String data, String dataType)
          throws IOException {
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", dataType);
    exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
    OutputStream responseBody = exchange.getResponseBody();
    responseBody.write(data.getBytes());
    responseBody.close();
  }

  private void writeToResultsFile(HttpExchange exchange,
                                  final String message,
                                  CgiArguments.RankerType rankerType) throws IOException  {
    String fileName = "";
    switch (rankerType) {
      case COSINE:
        fileName = "hw1.1-vsm.tsv";
        break;
      case QL:
        fileName = "hw1.1-ql.tsv";
        break;
      case PHRASE:
        fileName = "hw1.1-phrase.tsv";
        break;
      case NUMVIEWS:
        fileName = "hw1.1-numviews.tsv";
        break;
      case LINEAR:
        fileName = "hw1.2-linear.tsv";
        break;
      case FULLSCAN:
        fileName = "hw1.1-fs.tsv";
        break;
      default:
        // nothing
    }
    String path = "results" + File.separator + fileName;
    File file = new File(path);
    File parent = file.getParentFile();
    if(!parent.exists() && !parent.mkdirs()){
      throw new IllegalStateException("Couldn't create dir: " + parent);
    }

    try(PrintWriter out = new PrintWriter(new FileOutputStream(file, false))){
      out.println(message);
    }
    catch (FileNotFoundException e) {
      respondWithMsg(exchange, "Cannot write to results file:" + path);
    }

    respondWithMsg(exchange, "Results written to results file:" + path);
  }

  private void constructTextOutput(
          final Vector<ScoredDocument> docs, StringBuffer response) {
    for (ScoredDocument doc : docs) {
      response.append(response.length() > 0 ? "\n" : "");
      response.append(doc.asTextResult());
    }
    response.append(response.length() > 0 ? "\n" : "No result returned!");
  }

  private void constructTextOutputForQueryRepresentation(
          final Vector<TermProbability> termProbabilities, StringBuffer response) {
    for (TermProbability tp : termProbabilities) {
      response.append(tp.asTextResult());
      response.append("\n");
    }
    if(response.length() == 0) {
      throw new UnsupportedOperationException("No term probabilities found for Query representation");
    }
  }

  public void handle(HttpExchange exchange) throws IOException {
    String requestMethod = exchange.getRequestMethod();
    if (!requestMethod.equalsIgnoreCase("GET")) { // GET requests only.
      return;
    }

    // Print the user request header.
    Headers requestHeaders = exchange.getRequestHeaders();
    System.out.print("Incoming request: ");
    for (String key : requestHeaders.keySet()) {
      System.out.print(key + ":" + requestHeaders.get(key) + "; ");
    }
    System.out.println();

    // Validate the incoming request.
    String uriQuery = exchange.getRequestURI().getQuery();
    String uriPath = exchange.getRequestURI().getPath();
    if (uriPath == null || uriQuery == null) {
      respondWithMsg(exchange, "Something wrong with the URI!");
    }
    if (!uriPath.equals("/search") && !uriPath.equals("/prf")) {
      respondWithMsg(exchange, "Only /search and /prf is handled!");
    }
    System.out.println("Query: " + uriQuery);

    // Process the CGI arguments.
    CgiArguments cgiArgs = new CgiArguments(uriQuery);
    if (uriPath.equals("/search") && cgiArgs._query.isEmpty() && cgiArgs._queryFile == null) {
      respondWithMsg(exchange, "No query or query file is given to search!");
    }

    if(uriPath.equals("/prf")) {
      if(cgiArgs._query.isEmpty()) {
        respondWithMsg(exchange, "No 'query' is given for computing query representation");
      }
      if(cgiArgs._numDocs == -1) {
        respondWithMsg(exchange, "No 'numdocs' is given for computing query representation");
      }
      if(cgiArgs._numTerms == -1) {
        respondWithMsg(exchange, "No 'numterms' is given for computing query representation");
      }
    }

    // Create the ranker.
    Ranker ranker = Ranker.Factory.getRankerByArguments(
            cgiArgs, SearchEngine.OPTIONS, _indexer);
    if (ranker == null) {
      respondWithMsg(exchange,
              "Ranker " + cgiArgs._rankerType.toString() + " is not valid!");
    }

    Vector<String> queries = new Vector<>();
    BufferedReader reader = null;

    //Processing the query from query file if exists and valid only on /search path
    if(uriPath.equals("/search") && cgiArgs._queryFile != null) {
      try {
        reader = new BufferedReader(new FileReader(cgiArgs._queryFile));
        String query = null;
        while ((query = reader.readLine()) != null && !query.trim().isEmpty()) {
          queries.add(query);
        }
      } finally {
        if(reader != null) {
          reader.close();
        }
      }
    }

    // Adding the query from query cgi argument if exists
    if(cgiArgs._query != null && !cgiArgs._query.trim().isEmpty()) {
      queries.add(cgiArgs._query);
    }

    Vector<Query> processedQueries = new Vector<>();
    for(String query: queries) {
      Query processedQuery = new QueryPhrase(query);
      processedQuery.processQuery();
      processedQueries.add(processedQuery);
    }

    StringBuffer response = new StringBuffer();

    // Query representation for the /prf path
    if(uriPath.equals("/prf")) {
      for(Query query : processedQueries) {
        Vector<TermProbability> termProbabilities =
                ranker.querySimilarity(query, cgiArgs._numDocs, cgiArgs._numTerms);
        constructTextOutputForQueryRepresentation(termProbabilities, response);
        respondWithMsg(exchange, response.toString());
      }

      System.out.println("Finished computing the Query representation");
      return;
    }

    // Ranking for the /search path.
    switch (cgiArgs._outputFormat) {
      case TEXT:
        for(Query query : processedQueries) {
          Vector<ScoredDocument> scoredDocs =
                  ranker.runQuery(query, cgiArgs._numResults);
          constructTextOutput(scoredDocs, response);
        }
        break;
      case HTML:
        HtmlFormatter formatter = new HtmlFormatter();
        for(Query query : processedQueries) {
          Vector<ScoredDocument> scoredDocs =
                  ranker.runQuery(query, cgiArgs._numResults);
          formatter.AddTable(query, scoredDocs, cgiArgs._rankerType);
        }
        response.append(formatter.asHtmlString());
        break;
      default:
        // nothing
    }

    if(cgiArgs._outputType == CgiArguments.OutputType.HTTP)
    {
      if(cgiArgs._outputFormat == CgiArguments.OutputFormat.TEXT) {
        respondWithMsg(exchange, response.toString());
      }
      else if(cgiArgs._outputFormat == CgiArguments.OutputFormat.HTML) {
        respondWithHtml(exchange, response.toString());
      }
    }
    else if(cgiArgs._outputType == CgiArguments.OutputType.FILE){
      writeToResultsFile(exchange, response.toString(), cgiArgs._rankerType);
    }
    System.out.println("Finished processing all queries");
  }
}
