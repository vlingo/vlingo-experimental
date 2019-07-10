// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import io.vlingo.actors.testkit.TestUntil;
import io.vlingo.symbio.Entry;
import io.vlingo.symbio.State;
import io.vlingo.symbio.State.BinaryState;
import io.vlingo.symbio.store.dispatch.Dispatchable;
import io.vlingo.symbio.store.dispatch.Dispatcher;
import io.vlingo.symbio.store.dispatch.DispatcherControl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MockJournalDispatcher implements Dispatcher<Dispatchable<Entry<byte[]>, State<byte[]>>> {
  private final Object lock = new Object();
  private List<Entry<byte[]>> entries = new ArrayList<>();
  private AtomicReference<BinaryState> snapshot = new AtomicReference<>();
  private TestUntil until;

  @Override
  public void controlWith(final DispatcherControl control) {

  }

  @Override
  public void dispatch(final Dispatchable<Entry<byte[]>, State<byte[]>> dispatchable) {
    synchronized (lock) {
      this.entries.addAll(dispatchable.entries());
      dispatchable.state().ifPresent(state -> {
        this.snapshot.set(state.asBinaryState());
      });
      this.until.happened();
    }
  }

  public Entry<byte[]> entry(final int index) {
    synchronized (lock) {
      return entries.get(index);
    }
  }

  public int entryElements() {
    synchronized (lock) {
      return entries.size();
    }
  }

  public BinaryState snapshot() {
    synchronized (lock) {
      return snapshot.get();
    }
  }

  public TestUntil untilHappenings(final int times) {
    synchronized (lock) {
      this.until = TestUntil.happenings(times);
      return this.until;
    }
  }

}
