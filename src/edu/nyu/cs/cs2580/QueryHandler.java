package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.Vector;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import edu.nyu.cs.cs2580.SearchEngine.Options;

import javax.xml.ws.http.HTTPException;

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
    
    // The type of the ranker we will be using.
    public enum RankerType {
      NONE,
      FULLSCAN,
      COSINE,
      QL,
      PHRASE,
      NUMVIEWS,  // This is a query-independent ranking signal
      LINEAR,
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
            _numResults = Integer.parseInt(val);
          } catch (NumberFormatException e) {
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
  private Indexer _indexer;

  public QueryHandler(Options options, Indexer indexer) {
    _indexer = indexer;
  }

  private void respondWithMsg(HttpExchange exchange, final String message)
      throws IOException {
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/plain");
    exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
    OutputStream responseBody = exchange.getResponseBody();
    responseBody.write(message.getBytes());
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
    if (!uriPath.equals("/search")) {
      respondWithMsg(exchange, "Only /search is handled!");
    }
    System.out.println("Query: " + uriQuery);

    // Process the CGI arguments.
    CgiArguments cgiArgs = new CgiArguments(uriQuery);
    if (cgiArgs._query.isEmpty() && cgiArgs._queryFile == null) {
      respondWithMsg(exchange, "No query or query file is given to search!");
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

    //Processing the query from query file if exists and valid
    if(cgiArgs._queryFile != null) {
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
      Query processedQuery = new Query(query);
      processedQuery.processQuery();
      processedQueries.add(processedQuery);
    }

    // Ranking.
    Vector<ScoredDocument> scoredDocs =
        ranker.runQuery(processedQueries, cgiArgs._numResults);
    StringBuffer response = new StringBuffer();
    switch (cgiArgs._outputFormat) {
    case TEXT:
      constructTextOutput(scoredDocs, response);
      break;
    case HTML:
      // @CS2580: Plug in your HTML output
      break;
    default:
      // nothing
    }

    if(cgiArgs._outputType == CgiArguments.OutputType.HTTP)
    {
      respondWithMsg(exchange, response.toString());
    }
    else if(cgiArgs._outputType == CgiArguments.OutputType.FILE){
      writeToResultsFile(exchange, response.toString(), cgiArgs._rankerType);
    }
    System.out.println("Finished processing all queries");
  }
}

