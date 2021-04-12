// Copyright © 2012-2020 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.vlingo.actors.testkit.AccessSafely;
import io.vlingo.symbio.Entry;
import io.vlingo.symbio.State;
import io.vlingo.symbio.State.BinaryState;
import io.vlingo.symbio.store.dispatch.Dispatchable;
import io.vlingo.symbio.store.dispatch.Dispatcher;
import io.vlingo.symbio.store.dispatch.DispatcherControl;

public class MockJournalDispatcher implements Dispatcher<Dispatchable<Entry<byte[]>, State<byte[]>>> {
  private List<Entry<byte[]>> entries = new ArrayList<>();
  private AtomicReference<BinaryState> snapshot = new AtomicReference<>();
  private AccessSafely access = AccessSafely.afterCompleting(0);

  @Override
  public void controlWith(final DispatcherControl control) {

  }

  @Override
  public void dispatch(final Dispatchable<Entry<byte[]>, State<byte[]>> dispatchable) {
    access.writeUsing("entries", dispatchable.entries());
    dispatchable.state().ifPresent(state -> {
      access.writeUsing("state", state.asBinaryState());
    });
  }

  public Entry<byte[]> entry(final int index) {
    return access.readFrom("entry", index);
  }

  public int entryElements() {
    return access.readFrom("entriesSize");
  }

  public BinaryState snapshot() {
    return access.readFrom("snapshot");
  }

  public AccessSafely afterCompleting(final int times) {
    access = AccessSafely.afterCompleting(times);
    access.writingWith("entries", (List<Entry<byte[]>> e) -> entries.addAll(e));
    access.writingWith("state", (State<byte[]> state) -> snapshot.set(state.asBinaryState()));
    access.readingWith("entry", (Integer index) -> entries.get(index));
    access.readingWith("entriesSize", () -> entries.size());
    access.readingWith("snapshot", () -> snapshot.get());

    return access;
  }
}
