/*
 * All content copyright (c) 2012 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.terracottatech.frs.mock.transaction;

import com.terracottatech.frs.transaction.TransactionLockProvider;
import com.terracottatech.frs.action.Action;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.Lock;

/**
 *
 * @author cdennis
 */
public class MockTransactionCommitAction implements Action, Serializable {

  private final long id;
  
  public MockTransactionCommitAction(long id) {
    this.id = id;
  }

  @Override
  public long getPreviousLsn() {
    return -1;
  }

  @Override
  public void record(long lsn) {
    //
  }

  @Override
  public void replay(long lsn) {
    throw new AssertionError();
  }

  @Override
  public String toString() {
    return "Action: commitTransaction(" + id + ")";
  }

  public long getId() {
   return id;
  }

  @Override
  public Collection<Lock> lock(TransactionLockProvider locks) {
    return Collections.emptyList();
  }
}