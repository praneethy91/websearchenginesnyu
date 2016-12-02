package edu.nyu.cs.cs2580; /**
 * Created by mansivirani on 30/11/16.
 */


import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class LinksCollector {
    // We'll use a fake USER_AGENT so the web server thinks the robot is a normal web browser.
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";
    private List<String> links = new LinkedList<String>();
    private Document htmlDocument;


    /**
     * This performs all the work. It makes an HTTP request, checks the response, and then gathers
     * up all the links on the page. Perform a searchForWord after the successful crawl
     *
     * @param url
     *            - The URL to visit
     * @return whether or not the crawl was successful
     */
    public boolean crawl(String url, int j, String hostName) throws IOException
    {

        try {

            Connection connection = Jsoup.connect(url).userAgent(USER_AGENT);
            Document htmlDocument = connection.get();
            this.htmlDocument = htmlDocument;
            Connection.Response response = connection.response();


            if (response.statusCode() == 200) // 200 is the HTTP OK status code
                // indicating that everything is great.
                try {

                        String host = response.url().getHost();

                        if (!connection.response().contentType().contains("text/html") || connection.response().contentType().equals(null)) {
                            //System.out.print("This is not an HTML Doc");
                            return false;
                        }
                        else if (!host.equals(hostName)) {
                            //System.out.println("Failure: Retrieved irrelevant URL " + url);
                            return false;
                        }
                        else {


                            System.out.println("\nVisiting: " + url);
                            String docBodyText = this.htmlDocument.html();


                            String pathToHTMLDocs = "data/HTMLDocs/";
                            File newFile = new File(pathToHTMLDocs+"File-" + j);
                            FileWriter fw = new FileWriter(newFile.getAbsoluteFile(), false);
                            BufferedWriter bw = new BufferedWriter(fw);
                            bw.write(docBodyText);
                            bw.close();
                        }

                } catch (Exception e){
                    System.out.print("Some null excemption when content type is null");
                }



            Elements linksOnPage = htmlDocument.select("a[href]");
            System.out.println("Found (" + linksOnPage.size() + ") links");
            for(Element link : linksOnPage)
            {
                this.links.add(link.absUrl("href"));
            }
            return true;
        }
        catch(IOException ioe)
        {
            // We were not successful in our HTTP request
            return false;
        }
        catch(IllegalArgumentException iae)
        {
            // We were not successful in our HTTP request
            return false;
        }
    }



    public List<String> getLinks()
    {
        return this.links;
    }

}
