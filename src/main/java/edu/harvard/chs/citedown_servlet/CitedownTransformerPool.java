package edu.harvard.chs.citedown_servlet;

import java.io.*;

import edu.harvard.chs.citedown_servlet.CitedownTransformerFactory;

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
