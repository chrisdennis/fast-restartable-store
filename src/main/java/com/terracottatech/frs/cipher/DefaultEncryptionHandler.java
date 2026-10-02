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
package com.terracottatech.frs.cipher;

import com.terracottatech.frs.DeleteAction;
import com.terracottatech.frs.PutAction;
import com.terracottatech.frs.RemoveAction;
import com.terracottatech.frs.action.Action;
import com.terracottatech.frs.action.ActionCodec;
import com.terracottatech.frs.action.ActionSubCodec;
import com.terracottatech.frs.action.NullAction;
import com.terracottatech.frs.compaction.CompactionAction;
import com.terracottatech.frs.object.ObjectManager;
import com.terracottatech.frs.transaction.TransactionCommitAction;
import com.terracottatech.frs.transaction.TransactionalAction;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.terracottatech.frs.util.ByteBufferUtils.concatenate;

public class DefaultEncryptionHandler implements EncryptionHandler<ByteBuffer, ByteBuffer, ByteBuffer> {

  private final CipherManager cipherManager;
  private final ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager;
  private final ActionCodec<ByteBuffer, ByteBuffer, ByteBuffer> actionCodec;

  private final Map<Class<? extends Action>, Function<ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, ? extends Action>,
      ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, ? extends Action>>> handlers = new HashMap<>();

  public DefaultEncryptionHandler(ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager,
                                  ActionCodec<ByteBuffer, ByteBuffer, ByteBuffer> actionCodec,
                                  Map<String, byte[]> tokenToKeyMap, String currentToken) {
    cipherManager = new AESCipherManager(tokenToKeyMap, currentToken);
    this.objectManager = objectManager;
    this.actionCodec = actionCodec;

    ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, PutAction> encryptedPutSubCodec =
        new EncryptedPutActionSubCodec(this.cipherManager, actionCodec.getSubCodec(PutAction.class));

    ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, CompactionAction> encryptedCompactionSubCodec =
        new ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, CompactionAction>() {
          @Override
          public ByteBuffer[] encode(CompactionAction action, ActionCodec<ByteBuffer, ByteBuffer, ByteBuffer> codec) {
            return encryptedPutSubCodec.encode(action, codec);   // CompactionAction IS-A PutAction
          }

          @Override
          public Action decode(ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> om,
                               ActionCodec<ByteBuffer, ByteBuffer, ByteBuffer> codec, ByteBuffer[] buffers) {
            return encryptedPutSubCodec.decode(om, codec, buffers);
          }
        };

    handlers.put(NullAction.class, subCodec -> subCodec);
    handlers.put(RemoveAction.class, subCodec -> subCodec);
    handlers.put(DeleteAction.class, subCodec -> subCodec);
    handlers.put(TransactionalAction.class, subCodec -> subCodec);
    handlers.put(TransactionCommitAction.class, subCodec -> subCodec);
    handlers.put(PutAction.class, subCodec -> encryptedPutSubCodec);
    handlers.put(CompactionAction.class, subCodec -> encryptedCompactionSubCodec);
  }

  @Override
  public String getCurrToken() {
    return cipherManager.getCurrentToken();
  }

  @Override
  public List<String> getPreviousTokens() {
    return cipherManager.getPreviousTokens();
  }

  @Override
  public boolean isUsingEncKey(String token) {
    return cipherManager.isUsingEncKey(token);
  }

  @Override
  public void add(String token, byte[] key) {
    cipherManager.add(token, key);
  }

  @Override
  public void remove(List<String> tokens) {
    cipherManager.remove(tokens);
  }


  @Override
  public <T extends Action> void registerAction(int collectionId, int actionId, Class<T> actionClass,
                                                ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, T> actionSubCodec) {
    actionCodec.registerAction(collectionId, actionId, actionClass, actionSubCodec);
  }

  @Override
  public <T extends Action> ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, T> getSubCodec(
      Class<? extends Action> actionClass) {
    return actionCodec.getSubCodec(actionClass);
  }

  @Override
  public Class<? extends Action> getActionClass(ByteBuffer[] buffers) {
    return actionCodec.getActionClass(buffers);
  }

  @Override
  public Action decode(ByteBuffer[] buffers) {
    Class<? extends Action> actionClass = actionCodec.getActionClass(buffers);
    return invokeDecodeHelper(actionClass, buffers, this);
  }

  @Override
  public ByteBuffer[] encode(Action action) {
    return concatenate(actionCodec.getHeader(action), invokeEncodeHelper(action, this));
  }

  @Override
  public ByteBuffer getHeader(Action action) {
    return actionCodec.getHeader(action);
  }

  @SuppressWarnings("unchecked")
  private <T extends Action> ByteBuffer[] invokeEncodeHelper(T action,
                                                             ActionCodec<ByteBuffer, ByteBuffer, ByteBuffer> actionCodec) {
    if (!handlers.containsKey(action.getClass())) {
      throw new IllegalArgumentException("No handler found for action: " + action.getClass());
    }

    ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, ? extends Action> baseSubCodec =
        actionCodec.getSubCodec(action.getClass());
    ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, ? extends Action> interceptedSubCodec =
        handlers.get(action.getClass()).apply(baseSubCodec);
    ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, T> typedSubCodec =
        (ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, T>) interceptedSubCodec;
    return typedSubCodec.encode(action, this);
  }

  @SuppressWarnings("unchecked")
  private <T extends Action> Action invokeDecodeHelper(Class<T> actionClass, ByteBuffer[] buffers,
                                                       ActionCodec<ByteBuffer, ByteBuffer, ByteBuffer> codec) {
    if (!handlers.containsKey(actionClass)) {
      throw new IllegalArgumentException("No handler found for action: " + actionClass);
    }

    ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, ? extends Action> baseSubCodec = codec.getSubCodec(actionClass);
    ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, ? extends Action> interceptedSubCodec =
        handlers.get(actionClass).apply(baseSubCodec);
    ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, T> typedSubCodec =
        (ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, T>) interceptedSubCodec;
    return typedSubCodec.decode(objectManager, this, buffers);
  }
}
