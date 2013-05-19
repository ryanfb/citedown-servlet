package info.papyri.xsugar.standalone;

import java.io.*;

import info.papyri.xsugar.standalone.CitedownTransformerFactory;

import org.apache.commons.pool.*;
import org.apache.commons.pool.impl.*;

public class CitedownTransformerPool extends GenericObjectPool {
  public CitedownTransformerPool(CitedownTransformerFactory objFactory) {
    super(objFactory);
    this.setMaxActive(64);
  }

  public CitedownTransformerPool(CitedownTransformerFactory objFactory, GenericObjectPool.Config config) {
    super(objFactory, config);
  }
}
