// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.vlingo.actors.testkit.TestUntil;
import io.vlingo.symbio.Entry;
import io.vlingo.symbio.State;
import io.vlingo.symbio.State.BinaryState;
import io.vlingo.symbio.store.journal.JournalListener;

public class MockJournalListener implements JournalListener<byte[]> {
  private List<Entry<byte[]>> entries = new ArrayList<>();
  private Object lock = new Object();
  private AtomicReference<BinaryState> snapshot = new AtomicReference<>();
  private TestUntil until;

  @Override
  public void appended(final Entry<byte[]> entry) {
    synchronized (lock) {
      //System.out.println("APPENDED: " + entry);
      this.entries.add(entry);
      this.until.happened();
    }
  }

  @Override
  public void appendedWith(final Entry<byte[]> entry, final State<byte[]> snapshot) {
    synchronized (lock) {
      //System.out.println("APPENDED: " + entry + " AND: " + snapshot);
      this.entries.add(entry);
      this.snapshot.set((BinaryState) snapshot);
      this.until.happened();
    }
  }

  @Override
  public void appendedAll(final List<Entry<byte[]>> entries) {
    synchronized (lock) {
      //System.out.println("APPENDED: " + entries);
      this.entries.addAll(entries);
      this.until.happened();
    }
  }

  @Override
  public void appendedAllWith(final List<Entry<byte[]>> entries, final State<byte[]> snapshot) {
    synchronized (lock) {
      //System.out.println("APPENDED: " + entries + " AND: " + snapshot);
      this.entries.addAll(entries);
      this.snapshot.set((BinaryState) snapshot);
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
