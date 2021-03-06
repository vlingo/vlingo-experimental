// Copyright © 2012-2020 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import java.util.ArrayList;
import java.util.List;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.KeySelector;
import com.apple.foundationdb.KeyValue;
import com.apple.foundationdb.ReadTransaction;
import com.apple.foundationdb.tuple.Tuple;

import io.vlingo.actors.Actor;
import io.vlingo.actors.ActorInstantiator;
import io.vlingo.common.Completes;
import io.vlingo.symbio.BaseEntry;
import io.vlingo.symbio.State;
import io.vlingo.symbio.store.journal.Stream;
import io.vlingo.symbio.store.journal.StreamReader;

/**
 * Actor-based {@code StreamReader} for the {@code Journal} over FoundationDB.
 */
public class FoundationDBStreamReaderActor extends Actor implements StreamReader<byte[]> {
  private static final List<BaseEntry<byte[]>> EmptyEntries = new ArrayList<>();

  private final Database database;
  private final String name;
  private final byte[] entriesSubspaceKey;
  private final byte[] snapshotsSubspaceKey;
  private final byte[] streamsSubspaceKey;

  /**
   * Construct my default state.
   * @param name the String name of the journal
   * @param entriesSubspaceKey the byte[] key of the entries subspace
   * @param streamsSubspaceKey the byte[] key of the streams subspace
   * @param snapshotsSubspaceKey the byte[] key of the snapshot subspace
   */
  public FoundationDBStreamReaderActor(
          final String name,
          final byte[] entriesSubspaceKey,
          final byte[] streamsSubspaceKey,
          final byte[] snapshotsSubspaceKey) {
    this.name = name;
    this.entriesSubspaceKey = entriesSubspaceKey;
    this.streamsSubspaceKey = streamsSubspaceKey;
    this.snapshotsSubspaceKey = snapshotsSubspaceKey;
    this.database = database();
  }

  /*
   * @see io.vlingo.symbio.store.journal.StreamReader#streamFor(java.lang.String)
   */
  @Override
  public Completes<Stream<byte[]>> streamFor(final String streamName) {
    return streamFor(streamName, 1);
  }

  /*
   * @see io.vlingo.symbio.store.journal.StreamReader#streamFor(java.lang.String, int)
   */
  @Override
  public Completes<Stream<byte[]>> streamFor(final String streamName, final int fromStreamVersion) {
    database.read(txn -> {
      final Stream<byte[]> stream = streamFor(streamName, fromStreamVersion, txn);
      return completes().with(stream);
    });
    return completes();
  }

  /**
   * Answer my database instance.
   * @return Database
   */
  private Database database() {
    final FDB fdb = FDB.selectAPIVersion(600);
    final Database database = fdb.open();
    return database;
  }

  /**
   * Answer the most recent snapshot for the {@code streamName} and {@code streamVersion} as a {@code KeyValue},
   * if any, or {@code null} if none exists.
   * @param streamName the String name of the stream
   * @param streamVersion the int version of the stream
   * @param txn the ReadTransaction within which to perform the read
   * @return KeyValue
   */
  private KeyValue snapshotOf(final String streamName, final int streamVersion, final ReadTransaction txn) {
    try {
      final int maybenapshotVersion = streamVersion > 0 ? streamVersion : Integer.MAX_VALUE;
      final byte[] snapshotKey = Tuple.from(snapshotsSubspaceKey, streamName, maybenapshotVersion).pack();
      final KeySelector begin = KeySelector.lastLessThan(snapshotKey);
      final KeySelector end = begin.add(1);
      final List<KeyValue> keysValues = txn.getRange(begin, end, 1, false).asList().get();
      if (!keysValues.isEmpty()) {
        return keysValues.get(0);
      }
    } catch (Throwable t) {
      logger().error("StreamReader '" + name + "' failed to read next because: " + t.getMessage(), t);
    }
    return null;
  }

  /**
   * Answer the {@code Stream<byte[]>} for the {@code streamName} and {@code fromStreamVersion}
   * as read from my {@code database}. If a snapshot is available that was persisted following
   * the {@code fromStreamVersion}, the actual version read is from the snapshot version.
   * @param streamName the String name of the stream
   * @param fromStreamVersion the int version of the stream where reading should/may begin
   * @param txn a ReadTransaction of my database within which the reading is performed
   * @return @{code Stream<byte[]>}
   */
  private Stream<byte[]> streamFor(final String streamName, final int fromStreamVersion, final ReadTransaction txn) {
    try {
      final KeyValue snapshot = snapshotOf(streamName, fromStreamVersion, txn);
      final long actualFromStreamVersion = snapshot == null ?
              fromStreamVersion :
              Tuple.fromBytes(snapshot.getKey()).getLong(2);
        final byte[] streamBeginKey = Tuple.from(streamsSubspaceKey, streamName, actualFromStreamVersion).pack();
        final KeySelector begin = KeySelector.firstGreaterOrEqual(streamBeginKey);
        final byte[] streamEndKey = Tuple.from(streamsSubspaceKey, streamName, Integer.MAX_VALUE).pack();
        // NOTE: It I don't add(1) to this I always get one less than the end-of-stream size.
        // Between this and all the Tuple instances there must be tons of garbage generated.
        final KeySelector end = KeySelector.lastLessOrEqual(streamEndKey).add(1);
        final List<KeyValue> streamOfKeysValues = txn.getRange(begin, end, ReadTransaction.ROW_LIMIT_UNLIMITED, false).asList().get();
        if (!streamOfKeysValues.isEmpty()) {
          return streamFrom(streamName, (int) actualFromStreamVersion, streamOfKeysValues, snapshot, txn);
        }
    } catch (Throwable t) {
      logger().error("StreamReader '" + name
              + "' failed to read the stream: " + streamName + ":" + fromStreamVersion
              + " because: " + t.getMessage(), t);
    }
    return new Stream<>(streamName, fromStreamVersion, EmptyEntries, null);
  }

  /**
   * Answer the {@code Stream<byte[]>} for the {@code streamName} and {@code fromStreamVersion}
   * as built from the {@code entriesKeysValues} and {@code snapshot}.
   * @param streamName the String name of the stream
   * @param streamVersion the int version of the stream
   * @param streamKeysValues the {@code List<KeyValue>} containing encoded entries
   * @param snapshot the KeyValue containing the encoded snapshot state, or null
   * @param txn a ReadTransaction of my database within which the reading is performed
   * @return {@code Stream<byte[]>}
   * @throws Exception the exception possibly occurring inside the EncoderDecoder
   */
  private Stream<byte[]> streamFrom(
          final String streamName,
          final int streamVersion,
          final List<KeyValue> streamKeysValues,
          final KeyValue snapshot,
          final ReadTransaction txn)
  throws Exception {
    final List<KeyValue> entriesKeysValues = streamJoinUsing(streamKeysValues, txn);
    final List<BaseEntry<byte[]>> entries = toEntries(entriesKeysValues, streamName);
    final State<byte[]> state = snapshot == null ?
            null :
            EncoderDecoder.decodeState(snapshot.getValue(), streamName);
    return new Stream<>(streamName, streamVersion, entries, state);
  }

  private List<KeyValue> streamJoinUsing(
          final List<KeyValue> streamKeysValues,
          final ReadTransaction txn)
  throws Exception {
    final List<KeyValue> entriesKeysValues = new ArrayList<>(streamKeysValues.size());
    for (final KeyValue kv : streamKeysValues) {
      final byte[] entryKey = Tuple.from(entriesSubspaceKey, kv.getValue()).pack();
      final KeySelector begin = KeySelector.firstGreaterOrEqual(entryKey);
      final KeySelector end = begin.add(1);
      final List<KeyValue> entryKeyValue = txn.getRange(begin, end, 1, false).asList().get();
      if (!entryKeyValue.isEmpty()) {
        entriesKeysValues.addAll(entryKeyValue);
      }
    }
    return entriesKeysValues;
  }

  /**
   * Answer a new {@code List<BaseEntry.BinaryEntry>} as built from the {@code entriesKeysValues}.
   * @param entriesKeysValues the {@code List<KeyValue>} read from my database
   * @param streamName the String name of the stream that may be used as the identity of each Entry
   * @return {@code List<BaseEntry.BinaryEntry>}
   * @throws Exception the exception possibly occurring inside the EncoderDecoder
   */
  private List<BaseEntry<byte[]>> toEntries(
          final List<KeyValue> entriesKeysValues,
          final String streamName)
  throws Exception {
    final List<BaseEntry<byte[]>> entries = new ArrayList<>(entriesKeysValues.size());
    for (final KeyValue kv : entriesKeysValues) {
      final BaseEntry.BinaryEntry entry = EncoderDecoder.decodeEntry(kv.getValue(), streamName);
      entries.add(entry);
    }
    return entries;
  }

  public static class FoundationDBStreamReaderInstantiator implements ActorInstantiator<FoundationDBStreamReaderActor> {
    private final String name;
    private final byte[] entriesSubspaceKey;
    private final byte[] streamsSubspaceKey;
    private final byte[] snapshotsSubspaceKey;

    public FoundationDBStreamReaderInstantiator(
            final String name,
            final byte[] entriesSubspaceKey,
            final byte[] streamsSubspaceKey,
            final byte[] snapshotsSubspaceKey) {
      this.name = name;
      this.entriesSubspaceKey = entriesSubspaceKey;
      this.streamsSubspaceKey = streamsSubspaceKey;
      this.snapshotsSubspaceKey = snapshotsSubspaceKey;
    }

    @Override
    public FoundationDBStreamReaderActor instantiate() {
      return new FoundationDBStreamReaderActor(name, entriesSubspaceKey, streamsSubspaceKey, snapshotsSubspaceKey);
    }

    @Override
    public Class<FoundationDBStreamReaderActor> type() {
      return FoundationDBStreamReaderActor.class;
    }
  }
}
