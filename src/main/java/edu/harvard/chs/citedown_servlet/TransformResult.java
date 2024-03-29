package edu.harvard.chs.citedown_servlet;

import java.io.Serializable;

public class TransformResult implements Serializable
{
  public String content = null;
  // public Exception; // = null; // dk.brics.grammar.parser.ParseException exception = null;
  
  public TransformResult() {}
 
  /**
   * Initialize transform result from a String.
   */ 
  public TransformResult(String set_content) {
    content = set_content;
  }
 
 /**
  * Initialize transform result from an exception.
  */ 
 /*
  public TransformResult(Exception set_exception) {
    exception = set_exception;
  }
  */
 
  /**
   * Initialize transform result from a string and an exception.
   */
  /* 
  public TransformResult(String set_content, Exception set_exception) {
    content = set_content;
    exception = set_exception;
  }
  (/)
 
  /**
   * Check if this transform result is an exception.
   */ 
  public boolean isException() {
    return false; // exception != null;
  }
}
