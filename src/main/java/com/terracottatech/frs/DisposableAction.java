/*
 * All content copyright (c) 2012 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.terracottatech.frs;

import com.terracottatech.frs.action.Action;
import com.terracottatech.frs.action.ActionCodec;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author mscott
 */
public class DisposableAction implements Action, Disposable {
    
    private final Action delegate;
    private final Disposable disposable;

    public DisposableAction(Action a, Disposable dispose) {
        delegate = a;
        disposable = dispose;
    }

    @Override
    public void dispose() {
        disposable.dispose();
    }

    @Override
    public void close() throws IOException {
        disposable.dispose();
    }

    @Override
    public void record(long lsn) {
        delegate.record(lsn);
    }

    @Override
    public void replay(long lsn) {
        delegate.replay(lsn);
    }

    @Override
    public ByteBuffer[] getPayload(ActionCodec codec) {
        return delegate.getPayload(codec);
    }
    
    protected Action getAction() {
        return delegate;
    }
    
}