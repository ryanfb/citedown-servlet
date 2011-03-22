package info.papyri.xsugar.standalone;

import java.io.*;
import java.net.URL;
import java.util.Hashtable;

import javax.servlet.*;
import javax.servlet.http.*;

import info.papyri.xsugar.standalone.XSugarStandaloneTransformer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;

public class XSugarStandaloneServlet extends HttpServlet
{
  private Hashtable transformers = null;

  private static String[] known_grammars = {"epidoc"};

  public void init(ServletConfig config)
    throws ServletException
  {
    super.init(config);

    transformers = new Hashtable();

    System.out.println("Initializing known-grammars...");
    for (String grammar : known_grammars) {
      System.out.println(grammar);
      initTransformer(grammar);
    }
    System.out.println("Done.");
  }

  private XSugarStandaloneTransformer initTransformer(String transformer_name)
  {
    XSugarStandaloneTransformer transformer = null;
    URL url = this.getClass().getClassLoader().getResource("/" + transformer_name + ".xsg");
    StringWriter url_writer = new StringWriter();

    try {
      IOUtils.copy(url.openStream(), url_writer, java.nio.charset.Charset.forName("UTF-8").name());

      transformer = new XSugarStandaloneTransformer(url_writer.toString());
      transformers.put(transformer_name, transformer);
    }
    catch (IOException e) {
    }
    catch (Throwable t) {
    }

    return transformer;
  }

  private XSugarStandaloneTransformer getTransformer(String transformer_name)
  {
    XSugarStandaloneTransformer transformer = 
      (XSugarStandaloneTransformer)transformers.get(transformer_name);
    if (transformer == null) {
      System.out.println("Cache miss for " + transformer_name);

      transformer = initTransformer(transformer_name);
    }
    return transformer;
  }

  private String doTransform(String content, String transform_type, String direction)
  {
    String result = null;
    XSugarStandaloneTransformer transformer = getTransformer(transform_type);

    try {
      if (direction.equals("xml2nonxml"))
      {
        result = transformer.XMLToNonXML(content);
      }
      else if (direction.equals("nonxml2xml"))
      {
        result = transformer.nonXMLToXML(StringEscapeUtils.unescapeHtml(content));
      }
      else {
        result = "Bad direction " + direction;
      }
    }
    catch (dk.brics.grammar.parser.ParseException e) {
      System.out.println(e.getMessage());
      // System.out.println(e.getLocation().getLine() + "," + e.getLocation().getColumn());
      e.printStackTrace();
    }
    catch (Throwable t) {
      System.out.println("Error! " + t.getClass().getName());
      t.printStackTrace();
    }

    return result;
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    response.setContentType("text/html;charset=UTF-8");
    response.setStatus(HttpServletResponse.SC_OK);
    PrintWriter out = response.getWriter();

    out.println("<html>");
    out.println("<head><title>XSugarStandaloneServlet</title></head>");
    out.println("<body>");
    out.println("<h1>XSugarStandaloneServlet</h1>");
    out.println("<form method=\"POST\" action=\"/\"/>");
    out.println("<textarea name=\"content\" rows=\"20\" cols=\"80\"></textarea>");
    out.println("<select name=\"type\">");
    for (String grammar : known_grammars) {
      out.println("<option value=\"" + grammar + "\">" + grammar + "</option>");
    }
    out.println("</select>");
    out.println("<input type=\"radio\" name=\"direction\" value=\"xml2nonxml\" checked />XML->Non-XML&nbsp;&nbsp");
    out.println("<input type=\"radio\" name=\"direction\" value=\"nonxml2xml\" />Non-XML->XML<br />");
    out.println("<input type=\"submit\" value=\"Submit\" />");
    out.println("</form>");
    out.println("session=" + request.getSession(true).getId());
    out.println("</body>");
    out.println("</html>");
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    String param_content = request.getParameter("content");
    String param_type = request.getParameter("type");
    String param_direction = request.getParameter("direction");

    String result = doTransform(param_content, param_type, param_direction);

    PrintWriter out = response.getWriter();

    response.setContentType("text/html;charset=UTF-8");
    response.setStatus(HttpServletResponse.SC_OK);

    out.println("<html>");
    out.println("<body>");
    out.println("You entered: <br /><pre><code>" + StringEscapeUtils.escapeHtml(param_content) + "</code></pre><br />");
    out.println("Grammar: " + param_type + "<br />");
    out.println("Direction: " + param_direction + "<br />");
    out.println("Result: <br /><pre><code>" + StringEscapeUtils.escapeHtml(result) + "</code></pre>");
    out.println("</body>");
    out.println("</html>");
  }
}