package com.terracottatech.frs.io.nio;

import com.terracottatech.frs.io.*;
import com.terracottatech.frs.log.BufferListWrapper;
import com.terracottatech.frs.log.LogRecord;
import com.terracottatech.frs.log.LogRegionPacker;
import com.terracottatech.frs.log.Signature;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author mscott
 */
public class NIOStreamImplTest {
  private static final long CHUNK_OVERHEAD   = 24;
  private static final long MAX_SEGMENT_SIZE = 10 * 1024 * 1024;
  NIOStreamImpl stream;
  File          workArea;
  Random        r;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    workArea = folder.newFolder();
    stream = new NIOStreamImpl(workArea, MAX_SEGMENT_SIZE);
    long seed = System.currentTimeMillis();
    System.out.println("Seed " + seed);
    r = new Random(seed);
  }

  @After
  public void tearDown() throws Exception {
    stream.close();
  }

  /**
   * Test of append method, of class NIOStreamImpl.
   */
  @Test
  public void testAppend() throws Exception {
    assertThat(stream.append(newChunk(1)), is(CHUNK_OVERHEAD + 1));
    assertThat(listFiles().length, is(1));

    assertThat(stream.append(newChunk(MAX_SEGMENT_SIZE - 2)),
               is(CHUNK_OVERHEAD + MAX_SEGMENT_SIZE - 2));
    assertThat(listFiles().length, is(1));

    assertThat(stream.append(newChunk(1)), is(CHUNK_OVERHEAD + 1));
    assertThat(listFiles().length, is(2));
  }

  @Test
  public void testRead() throws Exception {
    long size = 30 * 1024 * 1024;
    int numChunks = 0;
    while (size > 0) {
      int s = r.nextInt((int) (size + 1));
      stream.append(newChunk(s));
      size -= s;
      numChunks++;
    }
    stream.close();

    NIOStreamImpl nioStream = new NIOStreamImpl(workArea, MAX_SEGMENT_SIZE);
    nioStream.seek(-1);
    int foundChunks = 0;
    while (nioStream.read(Direction.REVERSE) != null) {
      foundChunks++;
    }
    assertThat(foundChunks, is(numChunks));
  }

  private File[] listFiles() {
    return workArea.listFiles(NIOStreamImpl.SEGMENT_FILENAME_FILTER);
  }

  private Chunk newChunk(long size) {
    List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
    while (size > 0) {
      int s = r.nextInt((int) (size + 1));
      buffers.add(ByteBuffer.allocate(s));
      size -= s;
    }
    return new BufferListWrapper(buffers);
  }

  /**
   * Test of sync method, of class NIOStreamImpl.
   */
  @Test
  @Ignore
  public void testSync() throws Exception {
    System.out.println("sync");
    stream.append(new LogRegionPacker(Signature.ADLER32).pack(
            new DummyLogRegion(Arrays.asList(new LogRecord[]{new RandomLogRecord()}))));
    stream.sync();
    File lock = new File(workArea.getAbsolutePath() + "/FRS.lck");
    assertTrue(lock.exists());
    FileBuffer chunk = new FileBuffer(new File(workArea.getAbsolutePath() + "/FRS.lck"));
    System.out.format("uuid: %s segment: %d position: %d",
                      new UUID(chunk.getLong(), chunk.getLong()).toString(),
                      chunk.getInt(), chunk.getLong());
  }
}