/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.terracottatech.fastrestartablestore.mock;

import com.terracottatech.fastrestartablestore.ChunkFactory;
import com.terracottatech.fastrestartablestore.IOManager;
import com.terracottatech.fastrestartablestore.LogManager;
import com.terracottatech.fastrestartablestore.messages.LogRecord;
import com.terracottatech.fastrestartablestore.messages.LogRegion;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author cdennis
 */
class MockLogManager implements LogManager {

  private final IOManager ioManager;
  
  public MockLogManager(IOManager ioManager) {
    this.ioManager = ioManager;
  }

  public Future<Void> append(LogRecord record) {
    return ioManager.append(new MockLogRegion(record));
  }

  public Iterator<LogRecord> reader() {
    return ioManager.reader(new MockChunkFactory());
  }
  
}

class MockChunkFactory implements ChunkFactory<LogRecord> {

  public MockChunkFactory() {
  }

  public LogRecord construct(InputStream chunk) throws IOException {
    ObjectInput in = new ObjectInputStream(chunk);
    try {
      return (LogRecord) in.readObject();
    } catch (ClassNotFoundException ex) {
      throw new IOException(ex);
    }
  }
}

