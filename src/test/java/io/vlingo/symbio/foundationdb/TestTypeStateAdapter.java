// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import io.vlingo.common.serialization.JsonSerialization;
import io.vlingo.symbio.Metadata;
import io.vlingo.symbio.State.BinaryState;
import io.vlingo.symbio.StateAdapter;

public class TestTypeStateAdapter implements StateAdapter<TestType,BinaryState> {

  @Override
  public int typeVersion() {
    return 1;
  }

  @Override
  public TestType fromRawState(final BinaryState raw) {
    return JsonSerialization.deserialized(new String(raw.data), TestType.class);
  }

  @Override
  public BinaryState toRawState(final TestType state, final int stateVersion, final Metadata metadata) {
    final String serialization = JsonSerialization.serialized(state);
    return new BinaryState(BinaryState.NoOp, TestType.class, typeVersion(), serialization.getBytes(), stateVersion, Metadata.nullMetadata());
  }
}
