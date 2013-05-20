package info.papyri.xsugar.standalone;

import java.io.*;

import org.xml.sax.SAXParseException;

import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;

import com.twmacinta.util.MD5;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.harvard.chs.citedown.*;

import info.papyri.xsugar.standalone.TransformResult;

/**
 * Holds an instance of a Citedown transformer, with methods for performing transforms using it.
 */
public class CitedownStandaloneTransformer
{
  private PegDownProcessor processor;

  private static PrintWriter out = new PrintWriter(java.lang.System.out, true);
  private static String charset = java.nio.charset.Charset.forName("UTF-8").name();
  
  private int extension_hash = 0;
  private JCS cache = null;

  // We use a class-shared initialization lock because it seems that some things in
  // the Citedown initialization can get into race conditions/deadlock if multiple threads
  // call them at the same time.
  private static final Lock initializationLock = new ReentrantLock(true);

  /**
   * Initialize an empty transformer.
   */
  public CitedownStandaloneTransformer()
  {
  }
  
  /**
   * Initialize a transformer for a given supported Citedown extension set.
   */ 
  public CitedownStandaloneTransformer(String extension)
    throws IOException, InstantiationException,	IllegalAccessException, ClassNotFoundException
  {
    this.initializeTransformer(extension);
  }

  public synchronized void initializeTransformer(String extension)
    throws IOException, InstantiationException,	IllegalAccessException, ClassNotFoundException
  {
    // This lock could probably be finer-grained, but this seems to solve the problem.
    initializationLock.lock();

    if(extension_hash == 0) {
      extension_hash = extension.hashCode();
      System.out.println("Hash: " + extension_hash);
      
      try {
        if(extension == "citedown") {
          processor = new PegDownProcessor(Extensions.CITE);
        }
        else if(extension == "markdown") {
          processor = new PegDownProcessor();
        }
      }
      catch (Throwable t) {
        System.out.println("Error initializing transformer for " + extension);
        extension_hash = 0;
        initializationLock.unlock();
      }

      
      try {
        cache = JCS.getInstance("default");
      }
      catch (CacheException e) {
        System.out.println("Error initializing cache!");
      }
    }

    initializationLock.unlock();
  }

  /**
   * Generate the cache key for this transformer based on the input text.
   *
   * Uses an MD5 of the input text, and generates keys in the form:
   *   extension_hash:inputmd5
   * So all of the entries for a extension hash can be invalidated at once. 
   * See: http://jakarta.apache.org/jcs/faq.html#hierarchical-removal  
   */
  public String cacheKey(String text) {
    MD5 md5 = new MD5();
    
    try {
      md5.Update(text.getBytes(charset));
      return new String(extension_hash + ":" + md5.asHex());
    }
    catch (java.io.UnsupportedEncodingException e) {
      return new String(extension_hash + ":" + text);
    }
  }
 
  /**
   * Store a given transform result in the cache, rescuing cache exceptions. 
   */
  private void cachePut(String key, TransformResult result) {
    try {
      cache.put(key,result);
    }
    catch (CacheException e) {
      System.out.println("Problem caching!");
    }
  }

  /**
   * Use this transformer to convert Markdown to HTML.
   */ 
  public String transformToHtml(String markdown)
    throws Exception
  {
    String result;
    String key = cacheKey(markdown);

    TransformResult cache_result = (TransformResult)cache.get(key);
    if (cache_result == null) {
      try {    
        result = processor.markdownToHtml(markdown);
      }
      catch (/* dk.brics.grammar.parser.ParseException */ Exception e) {
        System.out.println("Transform parse exception Markdown text = " + markdown);
        // comment to disable caching erroneous errors
        // cachePut(key, new TransformResult(e));
        throw e;
      }
      
      cachePut(key, new TransformResult(result));
    }
    else {
      //if (cache_result.isException()) {
      //  throw cache_result.exception;
      //}
      //else {
        result = cache_result.content;
      //}
    }

    return result;
  }
}
