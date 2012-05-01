/*
 * All content copyright (c) 2012 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.terracottatech.frs.io;

import java.nio.ByteBuffer;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author mscott
 */
public class RotatingBufferSourceTest {
    
    RotatingBufferSource rotate = new RotatingBufferSource();
    
    public RotatingBufferSourceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getBuffer method, of class RotatingBufferSource.
     */
    @Test
    public void testGetBuffer() throws Exception {
        int size = 1024;
        while ( size < 10 *1024 *1024 ) {
            rotate.getBuffer(size*=2);
        }
        System.gc();
        while ( size > 1024 ) {
            assert(rotate.getBuffer(size/=2) != null);
            Thread.sleep(1000);
        }
    }


}