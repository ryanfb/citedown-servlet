package info.papyri.xsugar.standalone;

import java.io.*;

import info.papyri.xsugar.standalone.CitedownStandaloneTransformer;

import org.apache.commons.pool.*;
import org.apache.commons.pool.impl.*;

public class CitedownTransformerFactory implements PoolableObjectFactory {
  private String extension = null;

  public CitedownTransformerFactory(String input_extension) {
    extension = input_extension;
  }
  
  public Object makeObject() {
    if(extension == null) {
      return new CitedownStandaloneTransformer();
    }
    else {
      try {
        return new CitedownStandaloneTransformer(extension);
      }
      catch (Throwable t) {
        return new CitedownStandaloneTransformer();
      }
    }
  }

  public void passivateObject(Object obj) {
  }

  public void activateObject(Object obj) {
    if(extension != null) {
      if(obj instanceof CitedownStandaloneTransformer) {
        CitedownStandaloneTransformer transformer = (CitedownStandaloneTransformer) obj;
        try {
          transformer.initializeTransformer(extension);
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
