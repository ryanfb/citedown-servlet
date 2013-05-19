package info.papyri.xsugar.standalone;

import java.io.*;

import info.papyri.xsugar.standalone.CitedownStandaloneTransformer;

import org.apache.commons.pool.*;
import org.apache.commons.pool.impl.*;

public class CitedownTransformerFactory implements PoolableObjectFactory {
  private String grammar = null;

  public CitedownTransformerFactory(String input_grammar) {
    grammar = input_grammar;
  }
  
  public Object makeObject() {
    if(grammar == null) {
      return new CitedownStandaloneTransformer();
    }
    else {
      try {
        return new CitedownStandaloneTransformer(grammar);
      }
      catch (Throwable t) {
        return new CitedownStandaloneTransformer();
      }
    }
  }

  public void passivateObject(Object obj) {
  }

  public void activateObject(Object obj) {
    if(grammar != null) {
      if(obj instanceof CitedownStandaloneTransformer) {
        CitedownStandaloneTransformer transformer = (CitedownStandaloneTransformer) obj;
        try {
          transformer.initializeTransformer(grammar);
        }
        catch (Throwable t) {
        }
      }
    }
  }

  public boolean validateObject(Object obj) {
    return true;
  }

  public void destroyObject(Object obj) {
  }
}
