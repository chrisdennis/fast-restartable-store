/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.terracottatech.frs;

import com.terracottatech.frs.action.ActionCodec;
import com.terracottatech.frs.action.ActionCodecImpl;
import com.terracottatech.frs.action.ActionManager;
import com.terracottatech.frs.action.ActionManagerImpl;
import com.terracottatech.frs.io.IOManager;
import com.terracottatech.frs.io.NIOManager;
import com.terracottatech.frs.log.*;
import com.terracottatech.frs.mock.object.MockObjectManager;
import com.terracottatech.frs.object.ObjectManager;
import com.terracottatech.frs.recovery.RecoveryManager;
import com.terracottatech.frs.recovery.RecoveryManagerImpl;
import com.terracottatech.frs.transaction.TransactionActions;
import com.terracottatech.frs.transaction.TransactionManager;
import com.terracottatech.frs.transaction.TransactionManagerImpl;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author mscott
 */
public class OnHeapTest {

    RestartStore store;
    SimpleLogManager   logMgr;
    ActionManager actionMgr;
    ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectMgr;
    Map<ByteBuffer, Map<ByteBuffer, ByteBuffer>> external;
        
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    public OnHeapTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        external = Collections.synchronizedMap(new LinkedHashMap<ByteBuffer, Map<ByteBuffer, ByteBuffer>>());
        ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectMgr = new MockObjectManager<ByteBuffer, ByteBuffer, ByteBuffer>(external);
        IOManager ioMgr = new NIOManager(folder.getRoot().getAbsolutePath(), (1 * 1024));
        logMgr = new SimpleLogManager(ioMgr);
        ActionCodec codec = new ActionCodecImpl(objectMgr);
        TransactionActions.registerActions(0, codec);
        MapActions.registerActions(1, codec);
        actionMgr = new ActionManagerImpl(logMgr, objectMgr, codec, new MasterLogRecordFactory());
        TransactionManager transactionMgr = new TransactionManagerImpl(actionMgr, true);
        store = new RestartStoreImpl(objectMgr, transactionMgr);
    }
    
    private void addTransaction(int count, RestartStore store) throws Exception {
        String[] r = {"foo","bar","baz","boo","tim","sar","myr","chr"};

        Transaction<ByteBuffer, ByteBuffer, ByteBuffer> transaction = store.beginTransaction();
        ByteBuffer i = (ByteBuffer)ByteBuffer.allocate(4).putInt(1).flip();
        String sk = r[(int)(Math.random()*r.length)%r.length] + count;
        String sv = r[(int)(Math.random()*r.length)%r.length];
        System.out.format("insert: %s=%s\n",sk,sv);
        ByteBuffer k = (ByteBuffer)ByteBuffer.allocate(25).put(sk.getBytes()).flip();
        ByteBuffer v = (ByteBuffer)ByteBuffer.allocate(3).put(sv.getBytes()).flip();
        transaction.put(i,k,v);
        transaction.commit();
    }

    @Test
    public void testIt() throws Exception {
        logMgr.startup();
        int count = 0;
        for (int x=0;x<10;x++) addTransaction(x,store);           
        logMgr.shutdown();
        count += 10;

        external.clear();
        RecoveryManager recoverMgr = new RecoveryManagerImpl(logMgr, actionMgr);
        recoverMgr.recover();
        System.out.format("recovered pulled: %d pushed: %d\n",logMgr.getRecoveryExchanger().returned(),logMgr.getRecoveryExchanger().count());
        
        
        for ( Map.Entry<ByteBuffer, Map<ByteBuffer,ByteBuffer>> ids : external.entrySet() ) {
            int id = ids.getKey().getInt(0);
            Map<ByteBuffer, ByteBuffer> map = ids.getValue();
            for ( Map.Entry<ByteBuffer,ByteBuffer> entry : map.entrySet() ) {
                byte[] g = new byte[entry.getKey().remaining()];
                entry.getKey().mark();
                entry.getKey().get(g);
                String skey = new String(g);
                entry.getKey().reset();
                ByteBuffer val = entry.getValue();
                g = new byte[val.remaining()];
                val.get(g);
                String sval = new String(g);
                System.out.println(id + " " + skey + " " + sval);
            }
        }
        
        
        System.out.println("=========");
        
        logMgr.startup();
        for (int x=0;x<10;x++) addTransaction(count + x, store);           
        logMgr.shutdown();
        
        external.clear();
        recoverMgr = new RecoveryManagerImpl(logMgr, actionMgr);
        recoverMgr.recover();
        System.out.format("recovered pulled: %d pushed: %d\n",logMgr.getRecoveryExchanger().returned(),logMgr.getRecoveryExchanger().count());
        
        for ( Map.Entry<ByteBuffer, Map<ByteBuffer,ByteBuffer>> ids : external.entrySet() ) {
            int id = ids.getKey().getInt(0);
            Map<ByteBuffer, ByteBuffer> map = ids.getValue();
            for ( ByteBuffer key : map.keySet() ) {
                byte[] g = new byte[key.remaining()];
                key.mark();
                key.get(g);
                String skey = new String(g);
                key.reset();
                ByteBuffer val = map.get(key);
                g = new byte[val.remaining()];
                val.get(g);
                String sval = new String(g);
                System.out.println(id + " " + skey + " " + sval);
            }
        }
    }

    @After
    public void tearDown() {
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
}