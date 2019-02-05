// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import java.util.Arrays;

import org.junit.Before;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;

import io.vlingo.actors.World;
import io.vlingo.symbio.store.journal.Journal;
import io.vlingo.symbio.store.journal.JournalReader;
import io.vlingo.symbio.store.journal.StreamReader;

public abstract class BaseFoundationDBJounralTest {
  protected static final int KindsOfEvents = 3;

  protected Journal<byte[]> journal;
  protected JournalReader<byte[]> journalReader;
  protected MockJournalListener listener;
  protected StreamReader<byte[]> streamReader;
  protected World world;
  
  protected final ProductCreatedEntryAdapter productCreatedEntryAdapter = new ProductCreatedEntryAdapter();
  protected final SprintPlannedEntryAdapter sprintPlannedEntryAdapter = new SprintPlannedEntryAdapter();
  protected final BacklogItemCommittedEntryAdapter backlogItemCommittedEntryAdapter = new BacklogItemCommittedEntryAdapter();
  
  protected final TestTypeStateAdapter testTypeStateAdapter = new TestTypeStateAdapter();

  @Before
  public void setUp() {
    // clear/delete all keys-values
    final FDB fdb = FDB.selectAPIVersion(600);
    final Database database = fdb.open();
    database.run(txn -> { txn.clear("".getBytes(), "\\xFF".getBytes()); return null; });

    world = World.startWithDefaults("fdb-journal-tests");
    listener = new MockJournalListener();
    journal = Journal.using(world.stage(), FoundationDBJournalActor.class, listener, "fdb-test-journal");
    journal.registerEntryAdapter(ProductCreated.class, productCreatedEntryAdapter);
    journal.registerEntryAdapter(SprintPlanned.class, sprintPlannedEntryAdapter);
    journal.registerEntryAdapter(BacklogItemCommitted.class, backlogItemCommittedEntryAdapter);
    journal.registerStateAdapter(TestType.class, testTypeStateAdapter);
    journalReader = journal.journalReader("fdb-test-journal-reader").await();
    streamReader = journal.streamReader("fdb-test-stream-reader").await();
  }

  protected void appendSetsOfEvents(final int numberOfSets) {
    for (int idx = 0; idx < numberOfSets; ++idx) {
      final String productStreamId = streamNameFor(idx);
      final ProductCreated productCreated = new ProductCreated(productStreamId);
      final SprintPlanned sprintPlanned = new SprintPlanned("sprint"+idx);
      final BacklogItemCommitted backlogItemCommitted = new BacklogItemCommitted("backlogItem"+idx, "sprint"+idx);

      journal.appendAll(productStreamId, 1, Arrays.asList(productCreated, sprintPlanned, backlogItemCommitted), null, null);
    }
  }

  protected String streamNameFor(final int index) {
    return "product" + index;
  }
}
