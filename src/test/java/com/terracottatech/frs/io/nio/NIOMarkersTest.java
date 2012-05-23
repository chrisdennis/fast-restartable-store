/*
 * All content copyright (c) 2012 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.terracottatech.frs.io.nio;

import com.terracottatech.frs.io.Chunk;
import com.terracottatech.frs.io.Direction;
import com.terracottatech.frs.io.WrappingChunk;
import com.terracottatech.frs.util.JUnitTestFolder;

import java.io.File;
import java.nio.ByteBuffer;
import org.junit.*;

/**
 *
 * @author mscott
 */
public class NIOMarkersTest {
    NIOStreamImpl stream;
    File workArea;
    NIOManager manager;
    long current;
    long min;
    long max;
    
    @Rule
    public JUnitTestFolder folder = new JUnitTestFolder();    
    
    public NIOMarkersTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() throws Exception {
            workArea = folder.newFolder();
            System.out.println(workArea.getAbsolutePath());
            manager = new NIOManager(workArea.getAbsolutePath(), 1 * 1024 * 1024, 10 * 1024 * 1024);
   //  create a 10k lsn window
            for(int x=0;x<1000;x++) {
                writeChunkWithMarkers(10);
            }
            min = 10000;
   //  create a 10k lsn window
            for(int x=0;x<1000;x++) {
                writeChunkWithMarkers(10);
            }            
    }
    
    @Test
    public void testClean() throws Exception {
        manager.clean(0);
        manager.close();
        manager = new NIOManager(workArea.getAbsolutePath(), 1 * 1024 * 1024, 10 * 1024 * 1024);
        manager.seek(0);
        Chunk c = manager.read(Direction.FORWARD);
        int count = 0;
        while ( c != null ) {
            count++;
            c = manager.read(Direction.FORWARD);
        }
        assert(manager.getMinimumMarker() == 10000);
        assert(manager.getMaximumMarker() == 20000);
        System.out.println("chunks after clean " + count);
        assert(count < 2000);
    }
    
    private void writeChunkWithMarkers(int size) throws Exception {
        manager.setCurrentMarker(current);
        manager.setMinimumMarker(min);
        manager.setMaximumMarker(current+=size);
        manager.write(new WrappingChunk(ByteBuffer.allocate(10 * 1024)));
    }
    
    @After
    public void tearDown() throws Exception {
        manager.close();
        manager = null;
        System.gc();
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
}
