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
package com.terracottatech.frs.compaction;

import com.terracottatech.frs.PutActionSubCodec;
import com.terracottatech.frs.action.Action;
import com.terracottatech.frs.action.ActionCodec;
import com.terracottatech.frs.action.ActionSubCodec;
import com.terracottatech.frs.object.ObjectManager;

import java.nio.ByteBuffer;

public class CompactionActionSubCodec implements ActionSubCodec<ByteBuffer, ByteBuffer, ByteBuffer, CompactionAction> {
  private final PutActionSubCodec delegate;
  
  public CompactionActionSubCodec(PutActionSubCodec delegate) {
    this.delegate = delegate;  
  } 
  
  @Override
  public ByteBuffer[] encode(CompactionAction action, ActionCodec<ByteBuffer, ByteBuffer, ByteBuffer> codec) {
    return delegate.encode(action, codec);
  }

  @Override
  public Action decode(ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager, ActionCodec<ByteBuffer, ByteBuffer, ByteBuffer> codec, ByteBuffer[] buffers) {
    return delegate.decode(objectManager, codec, buffers);
  }
}
