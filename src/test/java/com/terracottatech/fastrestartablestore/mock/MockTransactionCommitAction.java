/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.terracottatech.fastrestartablestore.mock;

import com.terracottatech.fastrestartablestore.messages.Action;
import com.terracottatech.fastrestartablestore.spi.ObjectManager;
import java.io.Serializable;
import java.util.Set;

/**
 *
 * @author cdennis
 */
class MockTransactionCommitAction implements Action<Void, Void>, Serializable {

  private final long id;
  
  public MockTransactionCommitAction(long id) {
    this.id = id;
  }

  public boolean hasKey() {
    return false;
  }

  public Void getKey() {
    throw new UnsupportedOperationException("No keys on txn commit");
  }

  public String toString() {
    return "Action: commitTransaction(" + id + ")";
  }

  public boolean replay(ObjectManager<Void, Void> objManager, Set<Long> validTxnIds, long lsn) {
    validTxnIds.add(id);
    return true;
  }
}
