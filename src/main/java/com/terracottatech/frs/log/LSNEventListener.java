/*
 * All content copyright (c) 2012 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.terracottatech.frs.log;

/**
 *
 * @author mscott
 */
public interface LSNEventListener {
    
    public void record(long lsn);
    
}