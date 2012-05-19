/*
 * All content copyright (c) 2012 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.terracottatech.frs.io.nio;

import com.terracottatech.frs.io.*;
import com.terracottatech.frs.util.ByteBufferUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author mscott
 */
public class ChunkedReadbackStrategy extends AbstractReadbackStrategy {
    
    FileBuffer buffer;
    BufferSource source;
    List<Chunk>  chunkList;
    ListIterator<Chunk>  chunks;
    int size;
    Direction    direction;

    public ChunkedReadbackStrategy(FileBuffer buffer, BufferSource pool) throws IOException {
        this.buffer = buffer;
        this.source = pool;
        prepare();
    }
    
     
    @Override
    public Iterator<Chunk> iterator() {
        return chunkList.iterator();
    }
    
    private void prep(Direction dir) {
        if ( direction == null ) direction = dir;
        else return;
        
        if ( dir == Direction.REVERSE ) {
            Collections.reverse(chunkList);
        }
        chunks = chunkList.listIterator();
    }
    
    @Override
    public boolean hasMore(Direction dir) throws IOException {
        prep(dir);
        if ( dir == direction && chunks.hasNext() ) return true;
        if ( dir != direction && chunks.hasPrevious() ) return true;
        return false;
    }
    
 
    @Override
    public Chunk iterate(Direction dir) throws IOException {
        prep(dir);
        if ( dir == direction && chunks.hasNext() ) return chunks.next();
        if ( dir != direction && chunks.hasPrevious() ) return chunks.previous();
        return null;
    }     

    private void prepare() throws IOException {
        List<Long> jumps = readJumpList();
        
        if ( jumps == null ) {
            throw new IOException("unable to read jump list");
        }
        
        ArrayList<ByteBuffer> readIn = new ArrayList<ByteBuffer>(jumps.size()-1);
        
        long last = NIOSegmentImpl.FILE_HEADER_SIZE;
        for ( Long jump : jumps ) {
            readIn.add(source.getBuffer((int)(jump-last)));
            last = jump;
        }
        
        buffer.position(0);
        buffer.partition(NIOSegmentImpl.FILE_HEADER_SIZE);
        buffer.insert(readIn.toArray(new ByteBuffer[readIn.size()]), 1, true);
        buffer.read(readIn.size() + 1);
        buffer.truncate();
        buffer.skip(NIOSegmentImpl.FILE_HEADER_SIZE);
        chunkList = new ArrayList<Chunk>(jumps.size()-1);
        for ( ByteBuffer c : readIn ) {
            SegmentHeaders.CHUNK_START.validate(c.getInt());
            long amt = c.getLong();
            long check = c.getLong((int)amt);
            SegmentHeaders.FILE_CHUNK.validate(c.getInt((int)(amt + ByteBufferUtils.LONG_SIZE + ByteBufferUtils.LONG_SIZE)));
            c.limit(c.limit() - (ByteBufferUtils.LONG_SIZE + ByteBufferUtils.LONG_SIZE + ByteBufferUtils.INT_SIZE));
            chunkList.add(new WrappingChunk(c));
        }
    }
    
    private ArrayList<Long> readJumpList() throws IOException {
        buffer.clear();
        if ( buffer.size() < buffer.remaining() ) {
            buffer.limit(buffer.size());
        }
        buffer.position(buffer.size()-buffer.remaining());
        buffer.read(1);
        int jump = buffer.getInt(buffer.remaining()-4);
        if ( SegmentHeaders.JUMP_LIST.validate(jump) ) {
            short size = buffer.getShort(buffer.remaining()-6);
            if ( size < 0 ) return null;
            
            int reach = size * ByteBufferUtils.LONG_SIZE;
            long close = buffer.remaining()-reach-10;
            buffer.skip(close);
            int cfm = buffer.getInt();
            if ( SegmentHeaders.CLOSE_FILE.validate(cfm) ) {
                ArrayList<Long> jumps = new ArrayList<Long>();
                for (int x=0;x<size;x++) {
                    jumps.add(buffer.getLong());
                }
                return jumps;
            }
        }
        return null;
    }
    
    
    
}
