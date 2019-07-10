// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import io.vlingo.actors.World;
import io.vlingo.symbio.BaseEntry;
import io.vlingo.symbio.EntryAdapterProvider;
import io.vlingo.symbio.Source;
import io.vlingo.symbio.StateAdapterProvider;
import io.vlingo.symbio.store.journal.Journal;
import io.vlingo.symbio.store.journal.JournalReader;
import io.vlingo.symbio.store.journal.StreamReader;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class BaseFoundationDBJounralTest {
  protected static final int KindsOfEvents = 3;

  protected Journal<byte[]> journal;
  protected JournalReader<BaseEntry.BinaryEntry> journalReader;
  protected MockJournalDispatcher dispatcher;
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
    dispatcher = new MockJournalDispatcher();
    EntryAdapterProvider.instance(world).registerAdapter(ProductCreated.class, productCreatedEntryAdapter);
    EntryAdapterProvider.instance(world).registerAdapter(SprintPlanned.class, sprintPlannedEntryAdapter);
    EntryAdapterProvider.instance(world).registerAdapter(BacklogItemCommitted.class, backlogItemCommittedEntryAdapter);
    StateAdapterProvider.instance(world).registerAdapter(TestType.class, testTypeStateAdapter);

    journal = Journal.using(world.stage(), FoundationDBJournalActor.class, dispatcher, "fdb-test-journal");
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

  protected void appendEvents(final int numberOfEvents) {
    final String productStreamId = streamNameFor(numberOfEvents);
    final ProductCreated productCreated = new ProductCreated(productStreamId);
    journal.append(productStreamId, 1, productCreated, null, null);
    boolean even = true;
    for (int idx = 1; idx < numberOfEvents; ++idx) {
      if (even) {
        final SprintPlanned sprintPlanned = new SprintPlanned("sprint"+idx);
        journal.append(productStreamId, idx+1, sprintPlanned, null, null);
      } else {
        final BacklogItemCommitted backlogItemCommitted = new BacklogItemCommitted("backlogItem"+idx, "sprint"+idx);
        journal.append(productStreamId, idx+1, backlogItemCommitted, null, null);
      }
      even = !even;
    }
  }

  protected void appendEventsBatch(final int numberOfEvents) {
    final List<Source<byte[]>> batch = new ArrayList<>();
    final String productStreamId = streamNameFor(numberOfEvents);
    final ProductCreated productCreated = new ProductCreated(productStreamId);
    batch.add(productCreated);
    boolean even = true;
    for (int idx = 1; idx < numberOfEvents; ++idx) {
      if (even) {
        final SprintPlanned sprintPlanned = new SprintPlanned("sprint"+idx);
        batch.add(sprintPlanned);
      } else {
        final BacklogItemCommitted backlogItemCommitted = new BacklogItemCommitted("backlogItem"+idx, "sprint"+idx);
        batch.add(backlogItemCommitted);
      }
      even = !even;
    }
    journal.appendAll(productStreamId, 1, batch, null, null);
  }

  protected String streamNameFor(final int index) {
    return "product" + index;
  }
}
