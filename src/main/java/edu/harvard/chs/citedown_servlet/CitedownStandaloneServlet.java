package edu.harvard.chs.citedown_servlet;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import edu.harvard.chs.citedown.*;

public class CitedownStandaloneServlet extends HttpServlet
{
  private HashMap<String,CitedownTransformerPool> transformers = null;
  private ConcurrentHashMap<String,ReentrantLock> transformationLocks = null;

  private static String[] known_extensions = {"citedown", "markdown"};

  /**
   * Servlet init, called upon first request.
   *
   * Initializes all known grammars.
   */
  @Override
  public void init(ServletConfig config)
    throws ServletException
  {
    super.init(config);

    transformers = new HashMap<String,CitedownTransformerPool>();
    transformationLocks = new ConcurrentHashMap<String,ReentrantLock>();

    System.out.println("Initializing known-extensions...");
    for (String extension : known_extensions) {
      System.out.println(extension);
      initTransformerPool(extension);
    }
    System.out.println("Done.");
  }

  /**
   * Initialize a transformer pool based on a string with the name (used to access a resource).
   */
  private CitedownTransformerPool initTransformerPool(String transformer_name)
  {
    CitedownTransformerPool transformer = null;

    try {
      transformer = new CitedownTransformerPool(new CitedownTransformerFactory(transformer_name));
      transformers.put(transformer_name, transformer);
    }
    catch (Throwable t) {
    }

    return transformer;
  }

  /**
   * Get the transformer pool for a given name, optionally initializing it if not already present.
   */
  private CitedownTransformerPool getTransformerPool(String transformer_name)
  {
    CitedownTransformerPool transformer = transformers.get(transformer_name);
    if (transformer == null) {
      System.out.println("Cache miss for " + transformer_name);

      transformer = initTransformerPool(transformer_name);
    }
    return transformer;
  }

  /**
   * Perform a Citedown transform.
   *
   * @param content content to be transformed
   * @param transform_type type of transform (name of extension)
   * @return string containing the result of running the transform
   */
  private String doTransform(String content, String transform_type)
    throws java.lang.Exception, java.io.IOException
  {
    String result = null;
    CitedownTransformerPool pool = getTransformerPool(transform_type);
    CitedownStandaloneTransformer transformer = (CitedownStandaloneTransformer) pool.borrowObject();
    String key = transformer.cacheKey(content);
    pool.returnObject(transformer);

    // We use ConcurrentHashMap's atomic putIfAbsent here because otherwise multiple threads
    // might obtain multiple locks.
    // If the hash of locks winds up growing too large (since we never prune it, but GC compacts it significantly
    // in my testing), we could swap out ReentrantLock for a CountDownLatch that calls await if we got
    // the latch from the map, or executes normally with countDown at the end if it doesn't (i.e.
    // we were the first thread to execute). Then at the end remove the CountDownLatch from the map.
    Lock transformationLock = (Lock) transformationLocks.putIfAbsent(key, new ReentrantLock(true));

    if (transformationLock == null) {
      transformationLock = (Lock)transformationLocks.get(key);
    }

    transformationLock.lock();
    System.out.println("Acquired lock for " + key);
    transformer = (CitedownStandaloneTransformer) pool.borrowObject();

    try {
      result = transformer.transformToHtml(content); //nonXMLToXML(StringEscapeUtils.unescapeHtml(content));
    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println("Released lock for " + key);
      // System.out.println(e.getLocation().getLine() + "," + e.getLocation().getColumn());
      // e.printStackTrace();
      throw e;
    }
    finally {
      pool.returnObject(transformer);
      transformationLock.unlock();
    }

    System.out.println("Released lock for " + key);

    return result;
  }

  /**
   * Handle a servlet GET request (serves an HTML form for making a POST request for a transform).
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    response.setContentType("text/html;charset=UTF-8");
    response.setStatus(HttpServletResponse.SC_OK);
    PrintWriter out = response.getWriter();

    out.println("<html>");
    out.println("<head><title>CitedownStandaloneServlet</title></head>");
    out.println("<body>");
    out.println("<h1>CitedownStandaloneServlet</h1>");
    out.println("<form method=\"POST\" action=\"\"/>");
    out.println("<textarea name=\"content\" rows=\"20\" cols=\"80\"></textarea>");
    out.println("<select name=\"type\">");
    for (String extension : known_extensions) {
      out.println("<option value=\"" + extension + "\">" + extension + "</option>");
    }
    out.println("</select>");
    out.println("<input type=\"submit\" value=\"Submit\" />");
    out.println("</form>");
    out.println("session=" + request.getSession(true).getId());
    out.println("</body>");
    out.println("</html>");
  }

  /**
   * Handle a servlet POST request (serves JSON result of running Citedown transform).
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    String param_content = request.getParameter("content");
    String param_type = request.getParameter("type");
    
    boolean parse_exception = false;
    String result = null;
    String cause = null;
    int line = 0;
    int column = 0;
    
    PrintWriter out = response.getWriter();

    response.setContentType("application/json;charset=UTF-8");
    
    try {
      result = doTransform(param_content, param_type);
      response.setStatus(HttpServletResponse.SC_OK);
    }
    catch (/* dk.brics.grammar.parser.ParseException */ Exception e) {
      response.setStatus(HttpServletResponse.SC_OK);
      parse_exception = true;
      cause = "no cause"; // e.getMessage();
      line = 0; // e.getLocation().getLine();
      column = 0; // e.getLocation().getColumn();
    }
    catch (Throwable t) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      parse_exception = true;
      cause = "Unhandled error performing conversion. This is likely due to a large file containing a parse error, but due to the length of the file we are unable to fully parse it to indicate the position of the error.";
    }

    out.println("{");
    if (!parse_exception) {
      out.println("\"content\": \"" + StringEscapeUtils.escapeJavaScript(result) + "\"");
    }
    else {
      out.println("\"content\": \"" + StringEscapeUtils.escapeJavaScript(param_content) + "\",");
      out.println("\"exception\":");
        out.println("{");
          out.println("\"cause\": \"" + StringEscapeUtils.escapeJavaScript(cause) + "\",");
          out.println("\"line\": " + line + ",");
          out.println("\"column\": " + column);
        out.println("}");
    }
    out.println("}");
  }
}
