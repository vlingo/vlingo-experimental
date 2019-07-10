// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.MutationType;
import com.apple.foundationdb.Transaction;
import com.apple.foundationdb.directory.Directory;
import com.apple.foundationdb.directory.DirectoryLayer;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.apple.foundationdb.subspace.Subspace;
import com.apple.foundationdb.tuple.Tuple;
import com.apple.foundationdb.tuple.Versionstamp;
import io.vlingo.actors.Actor;
import io.vlingo.actors.Definition;
import io.vlingo.common.Completes;
import io.vlingo.common.Failure;
import io.vlingo.common.Outcome;
import io.vlingo.common.Success;
import io.vlingo.common.Tuple2;
import io.vlingo.common.Tuple4;
import io.vlingo.common.collection.ResettableReadOnlyList;
import io.vlingo.common.identity.IdentityGenerator;
import io.vlingo.symbio.BaseEntry;
import io.vlingo.symbio.Entry;
import io.vlingo.symbio.EntryAdapterProvider;
import io.vlingo.symbio.Metadata;
import io.vlingo.symbio.Source;
import io.vlingo.symbio.State;
import io.vlingo.symbio.StateAdapterProvider;
import io.vlingo.symbio.store.Result;
import io.vlingo.symbio.store.StorageException;
import io.vlingo.symbio.store.dispatch.Dispatchable;
import io.vlingo.symbio.store.dispatch.Dispatcher;
import io.vlingo.symbio.store.journal.Journal;
import io.vlingo.symbio.store.journal.JournalReader;
import io.vlingo.symbio.store.journal.StreamReader;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Actor-based {@code Journal} over FoundationDB.
 */
public class FoundationDBJournalActor extends Actor implements Journal<byte[]> {
  private static final byte[] NoModValue = new byte[0];

  private final Database database;
  private final EncoderDecoder encoder;
  private final byte[] entriesSubspaceKey;
  private int entriesUserVersion;
  private final String name;
  private final Dispatcher<Dispatchable<Entry<byte[]>, State<byte[]>>> dispatcher;
  private final ResettableReadOnlyList<Source<?>> wrapper;
  private final byte[] snapshotsSubspaceKey;
  private final Map<String, JournalReader<BaseEntry.BinaryEntry>> journalReaders;
  private final Map<String, StreamReader<byte[]>> streamReaders;
  private final byte[] streamsSubspaceKey;

  private final EntryAdapterProvider entryAdapterProvider;
  private final StateAdapterProvider stateAdapterProvider;
  private final IdentityGenerator dispatchablesIdentityGenerator;

  public FoundationDBJournalActor(final Dispatcher<Dispatchable<Entry<byte[]>, State<byte[]>>> dispatcher, final String name) throws Exception {
    this.name = name;
    this.dispatcher = dispatcher;
    final Tuple4<Database, byte[], byte[], byte[]> database = initialize(name);
    this.database = database._1;
    this.entriesSubspaceKey = database._2;
    this.entriesUserVersion = 0;
    this.streamsSubspaceKey = database._3;
    this.snapshotsSubspaceKey = database._4;
    this.entryAdapterProvider = EntryAdapterProvider.instance(stage().world());
    this.stateAdapterProvider = StateAdapterProvider.instance(stage().world());
    this.journalReaders = new HashMap<>(1);
    this.streamReaders = new HashMap<>(1);
    this.encoder = new EncoderDecoder();
    this.wrapper = new ResettableReadOnlyList<>();
    this.dispatchablesIdentityGenerator = new IdentityGenerator.RandomIdentityGenerator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <S, ST> void append(final String streamName, final int streamVersion, final Source<S> source, final Metadata metadata,
          final AppendResultInterest interest, final Object object) {
    appendUsing(streamName, streamVersion, source, null, interest, object, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <S, ST> void appendWith(final String streamName, final int streamVersion, final Source<S> source, final Metadata metadata, final ST snapshot,
          final AppendResultInterest interest, final Object object) {
    appendUsing(streamName, streamVersion, source, snapshot, interest, object, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <S, ST> void appendAll(final String streamName, final int fromStreamVersion, final List<Source<S>> sources, final Metadata metadata,
          final AppendResultInterest interest, final Object object) {
    appendUsing(streamName, fromStreamVersion, sources, null, interest, object, true, metadata);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <S, ST> void appendAllWith(final String streamName, final int fromStreamVersion, final List<Source<S>> sources, final Metadata metadata,
          final ST snapshot, final AppendResultInterest interest, final Object object) {
    appendUsing(streamName, fromStreamVersion, sources, snapshot, interest, object, true, metadata);
  }

  /*
   * @see io.vlingo.symbio.store.journal.Journal#journalReader(java.lang.String)
   */
  @Override
  @SuppressWarnings("unchecked")
  public Completes<JournalReader<BaseEntry.BinaryEntry>> journalReader(final String name) {
    JournalReader<BaseEntry.BinaryEntry> reader = journalReaders.get(name);
    if (reader == null) {
      final List<Object> parameters = Definition.parameters(name, entriesSubspaceKey);
      reader = childActorFor(JournalReader.class, Definition.has(FoundationDBJournalReaderActor.class, parameters));
      journalReaders.put(name, reader);
    }
    return completes().with(reader);
  }

  /*
   * @see io.vlingo.symbio.store.journal.Journal#streamReader(java.lang.String)
   */
  @Override
  @SuppressWarnings("unchecked")
  public Completes<StreamReader<byte[]>> streamReader(final String name) {
    StreamReader<byte[]> reader = streamReaders.get(name);
    if (reader == null) {
      final List<Object> parameters = Definition.parameters(name, entriesSubspaceKey, streamsSubspaceKey, snapshotsSubspaceKey);
      reader = childActorFor(StreamReader.class, Definition.has(FoundationDBStreamReaderActor.class, parameters));
      streamReaders.put(name, reader);
    }
    return completes().with(reader);
  }

  private <S> void appendStreamEntry(final byte[] fullEntryKey, final byte[] entryKey, final String streamName, final int streamVersion,
          final Entry<byte[]> entry, final Transaction inTransaction) {

    try {
      inTransaction.set(fullEntryKey, encoder.encode(entry));
      final byte[] streamKey = Tuple.from(streamsSubspaceKey, streamName, streamVersion).pack();
      inTransaction.set(streamKey, entryKey);
    } catch (Throwable t) {
      throw new StorageException(Result.Failure, "Could not append entry because: " + t.getMessage(), t);
    }
  }

  private void appendStreamSnapshot(final String streamName, final long streamVersion, final State<byte[]> snapshot, final Transaction inTransaction) {

    if (snapshot != null) {
      final byte[] snapshotKey = Tuple.from(snapshotsSubspaceKey, streamName, streamVersion).pack();
      final byte[] encoding = encoder.encode(snapshot);
      inTransaction.set(snapshotKey, encoding);
    }
  }

  private <S, ST> void appendUsing(final String streamName, final int streamVersion, final Source<S> source, final ST snapshot,
          final AppendResultInterest interest, final Object object, final Metadata metadata) {

    wrapper.wrap(source);

    appendUsing(streamName, streamVersion, wrapper.asList(), snapshot, interest, object, false, metadata);
  }

  private <S, ST> void appendUsing(final String streamName, final int fromStreamVersion, final List<Source<S>> sources, final ST snapshot,
          final AppendResultInterest interest, final Object object, final boolean many, final Metadata metadata) {

    final List<Entry<byte[]>> entries = new ArrayList<>(sources.size());
    final State<byte[]> state = asState(streamName, snapshot, fromStreamVersion, metadata);
    final Optional<ST> snapshotResult = state == null ? Optional.empty() : Optional.of(snapshot);

    database.run(txn -> {
      int index = 0;
      for (final Source<S> source : sources) {
        final Tuple2<byte[], byte[]> entryKeys = entryKeysFor(entriesSubspaceKey, entriesUserVersion());
        final Entry<byte[]> entry = asEntry(source, entryKeys._2, metadata);
        entries.add(entry);
        appendStreamEntry(entryKeys._1, entryKeys._2, streamName, fromStreamVersion + index, entry, txn);
        ++index;
      }
      appendStreamSnapshot(streamName, fromStreamVersion, state, txn);
      return Success.of(Result.Success);
    }).andThen(result -> {
      dispatch(streamName, fromStreamVersion, entries, state);
      informInterest(Success.of(Result.Success), streamName, fromStreamVersion, sources, interest, object, snapshotResult, many);
      return result;
    }).otherwise(failure -> {
      logger().error("Failed to append journal: " + name + " with: " + streamName + " : " + fromStreamVersion, failure);
      Outcome<StorageException, Result> outcome = Failure.of(new StorageException(Result.Failure, failure.getMessage(), failure));
      informInterest(outcome, streamName, fromStreamVersion, sources, interest, object, snapshotResult, many);
      return Result.Failure;
    });
  }

  @SuppressWarnings("unchecked")
  private Entry<byte[]> asEntry(final Source<?> source, final byte[] versionTimestampEntryKey, final Metadata metadata) {
    final String id = KeyConverter.fromVersionTimestamp(versionTimestampEntryKey);

    final Entry<byte[]> entry = entryAdapterProvider.asEntry(source, metadata);
    ((BaseEntry) entry).__internal__setId(id);
    return entry;
  }

  @SuppressWarnings("unchecked")
  private <ST> State<byte[]> asState(final String streamName, final ST snapshot, final int streamVersion, final Metadata metadata) {
    if (snapshot != null) {
      return stateAdapterProvider.asRaw(streamName, snapshot, streamVersion, metadata);
    }
    return null;
  }

  private Tuple4<Database, byte[], byte[], byte[]> initialize(final String name) throws Exception {
    final FDB fdb = FDB.selectAPIVersion(600);
    final Database database = fdb.open();

    final Subspace nodeSubspace = new Subspace(Tuple.from("io.vlingo.streams.journals"));
    final Directory directory = DirectoryLayer.createWithContentSubspace(nodeSubspace);

    final List<String> entriesPath = Arrays.asList("entires", name);
    final DirectorySubspace directoryEntriesSubspace = directory.createOrOpen(database, entriesPath).get();
    final byte[] entriesSubspaceKey = directoryEntriesSubspace.getKey();

    final List<String> streamsPath = Arrays.asList("streams", name);
    final DirectorySubspace directoryStreamsSubspace = directory.createOrOpen(database, streamsPath).get();
    final byte[] streamsSubspaceKey = directoryStreamsSubspace.getKey();

    final List<String> snapshotsPath = Arrays.asList("snapshots", name);
    final DirectorySubspace directorySnapshotsSubspace = directory.createOrOpen(database, snapshotsPath).get();
    final byte[] snapshotsSubspaceKey = directorySnapshotsSubspace.getKey();

    return Tuple4.from(database, entriesSubspaceKey, streamsSubspaceKey, snapshotsSubspaceKey);
  }

  private int entriesUserVersion() {
    if (entriesUserVersion > 65535) {
      entriesUserVersion = 0;
    }
    return entriesUserVersion++;
  }

  /**
   * Answer the Tuple2 of both the {@code fullEntryKey} and the {@code versionTimestampKey} segment.
   *
   * @param subspaceKey the byte[] subspace key
   * @param userVersion the int user version
   * @return {@code Tuple2<byte[],byte[]>}
   */
  private Tuple2<byte[], byte[]> entryKeysFor(final byte[] subspaceKey, final int userVersion) {

    // NOTE: The userVersion should never be necessary as the timestamp
    // resolution on FoundationDB transactions is 1 million per second.
    // this is being sent just in case and for (unlikely) future-proofing.
    // Also, since the VERSIONS_PER_SECOND knob can be dialed differently,
    // potentially at great risk, the userVersion could be critical for
    // custom FoundationDB configurations.
    //
    // See: https://forums.foundationdb.org/t/keyspace-partitions-performance/168/7
    // And: https://forums.foundationdb.org/t/keyspace-partitions-performance/168/8

    // NOTE: opportunity for async here, but use care due to potential
    // out of order writes. (I originally implemented with async on the
    // inTransaction.getVersionstamp() but I was concerned about potential
    // for out-of-order appends, which could cause the JournalReader and
    // StreamReader from reading correctly. I could have stowed messages
    // until completion but I was uncertain about the overall effects of
    // that approach. This deserves more thought and experimentation.

    try {
      final CompletableFuture<byte[]> versionstampFuture = database.run(txn -> {
        final Versionstamp version = Versionstamp.incomplete(userVersion);
        final Tuple template = Tuple.from(version);
        txn.mutate(MutationType.SET_VERSIONSTAMPED_KEY, template.packWithVersionstamp(), NoModValue);
        return txn.getVersionstamp();
      });
      final byte[] versionTimestampKey = versionstampFuture.get();
      final byte[] fullEntryKey = Tuple.from(subspaceKey, versionTimestampKey).pack();
      return Tuple2.from(fullEntryKey, versionTimestampKey);
    } catch (Throwable t) {
      throw new StorageException(Result.Failure, "Cannot allocate version timestamp identity because: " + t.getMessage(), t);
    }
  }

  private <S, ST> void informInterest(final Outcome<StorageException, Result> outcome, final String streamName, final int fromStreamVersion,
          final List<Source<S>> sources, final AppendResultInterest interest, final Object object, final Optional<ST> snapshot, final boolean many) {

    if (interest != null) {
      if (many) {
        interest.appendAllResultedIn(outcome, streamName, fromStreamVersion, sources, snapshot, object);
      } else {
        interest.appendResultedIn(outcome, streamName, fromStreamVersion, sources.get(0), snapshot, object);
      }
    }
  }

  private void dispatch(final String streamName, final int streamVersion, final List<Entry<byte[]>> entries, final State<byte[]> snapshot) {
    final String id = streamName + ":" + streamVersion + ":" + dispatchablesIdentityGenerator.generate().toString();
    dispatcher.dispatch(new Dispatchable<>(id, LocalDateTime.now(), snapshot, entries));
  }
}
