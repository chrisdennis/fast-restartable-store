/*
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.terracottatech.frs.action;

import com.terracottatech.frs.object.ObjectManager;
import com.terracottatech.frs.util.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.terracottatech.frs.util.ByteBufferUtils.concatenate;
import static com.terracottatech.frs.util.ByteBufferUtils.getInt;

/**
 * @author tim
 */
public final class ActionCodecImpl implements ActionCodec<ByteBuffer, ByteBuffer, ByteBuffer> {
  /* ActionCodecImpl.encode
  4 bytes - ActionID.collection
  4 bytes - ActionID.action
  */
  public static final long ACTION_HEADER_OVERHEAD = 8L;

  private static final ActionID NULL_ACTION_ID = new ActionID(-1, -1);

  private final Map<Class<? extends Action>, ActionID> classToId =
          new ConcurrentHashMap<>();
  private final Map<ActionID, ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, ? extends Action>> idToSubCodec =
          new ConcurrentHashMap<>();
  private final Map<ActionID, Class<? extends Action>> idToClass = new ConcurrentHashMap<>();
  private final ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager;

  public ActionCodecImpl(ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager) {
    this.objectManager = objectManager;
    registerAction(NULL_ACTION_ID, NullAction.class, NullAction.subCodec());
  }

  private synchronized <T extends Action> void registerAction(ActionID id, Class<T> actionClass, 
                                                              ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, T> actionSubCodec) {
    if (classToId.containsKey(actionClass)) {
      throw new IllegalArgumentException(
          "Action class " + actionClass + " already registered to id " + classToId.get(
              actionClass));
    }
    if (idToSubCodec.containsKey(id)) {
      throw new IllegalArgumentException(
          "Id " + id + " already registered to action SubCodec " + idToSubCodec.get(id));
    }
    if (idToClass.containsKey(id)) {
      throw new IllegalArgumentException(
          "Id " + id + " already registered to action class " + idToClass.get(id));
    }
    classToId.put(actionClass, id);
    idToClass.put(id, actionClass);
    @SuppressWarnings("unchecked")
    ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, ? extends Action> genericSubCodec = 
        (ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, ? extends Action>) actionSubCodec;
    idToSubCodec.put(id, genericSubCodec);
  }

  @Override
  public synchronized <T extends Action> void registerAction(int collectionId, int actionId, 
                                                             Class<T> actionClass, 
                                                             ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, T> actionSubCodec) {
    registerAction(new ActionID(collectionId, actionId), actionClass, actionSubCodec);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Action> ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, T> getSubCodec(Class<? extends Action> actionClass) {
    if (!classToId.containsKey(actionClass)) {
      throw new IllegalArgumentException("No SubCodec found for: " + actionClass);
    }
    return (ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, T>) idToSubCodec.get(classToId.get(actionClass));
  }

  @Override
  public Class<? extends Action> getActionClass(ByteBuffer[] buffers) {
    ActionID id = ActionID.withByteBuffers(buffers);
    Class<? extends Action> actionClass = idToClass.get(id);
    if (actionClass == null) {
      throw new IllegalArgumentException("Unknown Action type id= " + id);
    }
    return actionClass;
  }

  @Override
  public Action decode(ByteBuffer[] buffers) {
    ActionID id = ActionID.withByteBuffers(buffers);
    ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, ? extends Action> subCodec = idToSubCodec.get(id);
    if (subCodec == null) {
      throw new IllegalArgumentException("Unknown Action type id= " + id);
    }
    return subCodec.decode(objectManager, this, buffers);
  }

  @Override
  public ByteBuffer[] encode(Action action) {
    System.out.println("Hello " + action.getClass());
    if (!classToId.containsKey(action.getClass()))
      throw new IllegalArgumentException("Unknown action class " + action.getClass());
    
    ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, ? extends Action> subCodec = 
        idToSubCodec.get(classToId.get(action.getClass()));
    if (subCodec == null) {
      throw new IllegalStateException("No SubCodec found for " + action.getClass());
    }
    @SuppressWarnings("unchecked")
    ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, Action> boundSubCodec = 
        (ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, Action>) subCodec;
    return concatenate(headerBuffer(action), boundSubCodec.encode(action, this));
  }

  @Override
  public ByteBuffer getHeader(Action action) {
    return headerBuffer(action);
  }

  private ByteBuffer headerBuffer(Action action) {
    return classToId.get(action.getClass()).toByteBuffer();
  }

  private static class ActionID {
    private final int collection;
    private final int action;

    private ActionID(int collection, int action) {
      this.collection = collection;
      this.action = action;
    }

    static ActionID withByteBuffers(ByteBuffer[] buffers) {
      if (buffers.length == 0) {
        return NULL_ACTION_ID;
      } else {
        return new ActionID(getInt(buffers), getInt(buffers));
      }
    }

    ByteBuffer toByteBuffer() {
      ByteBuffer buffer = ByteBuffer.allocate(ByteBufferUtils.INT_SIZE * 2);
      buffer.putInt(collection).putInt(action).flip();
      return buffer;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ActionID actionID = (ActionID) o;

      return action == actionID.action && collection == actionID.collection;
    }

    @Override
    public int hashCode() {
      int result = collection;
      result = 31 * result + action;
      return result;
    }

    @Override
    public String toString() {
      return "ActionID{" +
              "collection=" + collection +
              ", action=" + action +
              '}';
    }
  }
}
