/*
 * All content copyright (c) 2012 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.terracottatech.frs.transaction;

import com.terracottatech.frs.TransactionException;
import com.terracottatech.frs.action.Action;

/**
 * @author tim
 */
public class NullTransactionManager implements TransactionManager {
  @Override
  public TransactionHandle begin() {
    return null;
  }

  @Override
  public void commit(TransactionHandle handle) throws InterruptedException,
          TransactionException {
  }

  @Override
  public void happened(TransactionHandle handle, Action action) {
  }

  @Override
  public void happened(Action action) throws InterruptedException, TransactionException {
  }
}