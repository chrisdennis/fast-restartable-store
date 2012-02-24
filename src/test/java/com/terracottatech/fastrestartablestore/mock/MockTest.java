/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.terracottatech.fastrestartablestore.mock;

import com.terracottatech.fastrestartablestore.IOManager;
import com.terracottatech.fastrestartablestore.RestartStore;
import com.terracottatech.fastrestartablestore.TransactionContext;
import com.terracottatech.fastrestartablestore.spi.ObjectManager;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 *
 * @author cdennis
 */
public class MockTest {
  
  @Test
  public void testMock() {
    IOManager ioManager = new MockIOManager();
    
    Map<String, String> outsideWorld = new HashMap<String, String>();
    RestartStore mock = MockRestartStore.create(new MockObjectManager(outsideWorld), ioManager);
    
    TransactionContext<String, String> context = mock.createTransaction();
    context.put("foo", "bar");
    outsideWorld.put("foo", "bar");
    context.commit();
    
    context = mock.createTransaction();
    context.remove("foo");
    outsideWorld.remove("foo");
    context.commit();

    context = mock.createTransaction();
    context.put("bar", "baz");
    outsideWorld.put("bar", "baz");
    context.commit();
    
    context = mock.createTransaction();
    context.put("foo", "baz");
    outsideWorld.put("foo", "baz");

    context = mock.createTransaction();
    context.remove("bar");
    outsideWorld.remove("bar");
    
    System.out.println("XXXXX CRASHING HERE XXXXX");
    
    //crash here - all knowledge lost - except IOManager
    
    outsideWorld = new HashMap<String, String>();
    RestartStore recovered = MockRestartStore.create(new MockObjectManager(outsideWorld), ioManager);
    System.out.println(outsideWorld);
  }
}
