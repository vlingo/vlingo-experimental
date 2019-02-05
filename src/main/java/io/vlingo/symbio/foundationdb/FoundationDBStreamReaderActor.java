// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.KeySelector;
import com.apple.foundationdb.KeyValue;
import com.apple.foundationdb.Transaction;
import com.apple.foundationdb.tuple.Tuple;

import io.vlingo.actors.Actor;
import io.vlingo.common.Completes;
import io.vlingo.symbio.Entry;
import io.vlingo.symbio.State;
import io.vlingo.symbio.store.journal.Stream;
import io.vlingo.symbio.store.journal.StreamReader;

/**
 * Actor-based {@code StreamReader} for the {@code Journal} over FoundationDB.
 */
public class FoundationDBStreamReaderActor extends Actor implements StreamReader<byte[]> {
  private static final List<Entry<byte[]>> EmptyEntries = new ArrayList<>();

  private final Database database;
  private final String name;
  private final byte[] entriesSubspaceKey;
  private final byte[] snapshotsSubspaceKey;
  private final byte[] streamsSubspaceKey;

  /**
   * Construct my default state.
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
    database.run(txn -> {
      final Stream<byte[]> stream = streamFor(streamName, fromStreamVersion, txn);
      return completes().with(stream);
    });
    return null;
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
   * @param txn the Transaction within which to perform the read
   * @return KeyValue
   */
  private KeyValue snapshotOf(final String streamName, final int streamVersion, final Transaction txn) {
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
      logger().log("StreamReader '" + name + "' failed to read next because: " + t.getMessage(), t);
    }
    return null;
  }

  /**
   * Answer the {@code Stream<byte[]>} for the {@code streamName} and {@code fromStreamVersion}
   * as read from my {@code database}. If a snapshot is available that was persisted following
   * the {@code fromStreamVersion}, the actual version read is from the snapshot version.
   * @param streamName the String name of the stream
   * @param fromStreamVersion the int version of the stream where reading should/may begin
   * @param txn a Transaction of my database within which the reading is performed
   * @return @{code Stream<byte[]>}
   */
  private Stream<byte[]> streamFor(final String streamName, final int fromStreamVersion, final Transaction txn) {
    try {
      final KeyValue snapshot = snapshotOf(streamName, fromStreamVersion, txn);
      final int actualFromStreamVersion = snapshot == null ?
              fromStreamVersion :
              (int) Tuple.fromBytes(snapshot.getKey()).get(2);
//      final byte[] streamEntriesKey = Tuple.from(streamsSubspaceKey, streamName).pack();
//      final Range range = Range.startsWith(streamEntriesKey);
//      System.out.println("RANGE: " + range);
//      final List<KeyValue> keysValues = txn.getRange(range, ReadTransaction.ROW_LIMIT_UNLIMITED, false).asList().get();
//      System.out.println("FOUND RANGE: " + keysValues.size());
      final List<KeyValue> streamOfKeysValues = new ArrayList<>(2);
      for (int idx = 0; ; ++idx) {
        final byte[] streamEntriesKey = Tuple.from(streamsSubspaceKey, streamName, actualFromStreamVersion + idx).pack();
        final KeySelector begin = KeySelector.firstGreaterOrEqual(streamEntriesKey);
        final KeySelector end = begin.add(1);
        //final byte[] streamLastKey = Tuple.from(streamsSubspaceKey, streamName, 100).pack();
        // final KeySelector end = KeySelector.lastLessOrEqual(streamLastKey);
        final List<KeyValue> oneKeyValue = txn.getRange(begin, end, 1, false).asList().get();
        if (oneKeyValue == null || oneKeyValue.isEmpty() ||
                !Arrays.equals(streamEntriesKey, oneKeyValue.get(0).getKey())) break;
        streamOfKeysValues.addAll(oneKeyValue);
      }
      if (!streamOfKeysValues.isEmpty()) {
        return streamFrom(streamName, actualFromStreamVersion, streamOfKeysValues, snapshot, txn);
      }
    } catch (Throwable t) {
      logger().log("StreamReader '" + name
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
   * @param txn a Transaction of my database within which the reading is performed
   * @return {@code Stream<byte[]>}
   * @throws Exception the exception possibly occurring inside the EncoderDecoder
   */
  private Stream<byte[]> streamFrom(
          final String streamName,
          final int streamVersion,
          final List<KeyValue> streamKeysValues,
          final KeyValue snapshot,
          final Transaction txn)
  throws Exception {
    final List<KeyValue> entriesKeysValues = streamJoinUsing(streamKeysValues, txn);
    final List<Entry<byte[]>> entries = toEntries(entriesKeysValues, streamName);
    final State<byte[]> state = snapshot == null ?
            null :
            EncoderDecoder.decodeState(snapshot.getValue(), streamName);
    final Stream<byte[]> stream = new Stream<>(streamName, streamVersion, entries, state);
    return stream;
  }

  private List<KeyValue> streamJoinUsing(
          final List<KeyValue> streamKeysValues,
          final Transaction txn)
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
   * Answer a new {@code List<Entry<byte[]>>} as built from the {@code entriesKeysValues}.
   * @param entriesKeysValues the {@code List<KeyValue>} read from my database
   * @param streamName the String name of the stream that may be used as the identity of each Entry
   * @return {@code List<Entry<byte[]>>}
   * @throws Exception the exception possibly occurring inside the EncoderDecoder
   */
  private List<Entry<byte[]>> toEntries(
          final List<KeyValue> entriesKeysValues,
          final String streamName)
  throws Exception {
    final List<Entry<byte[]>> entries = new ArrayList<>(entriesKeysValues.size());
    for (final KeyValue kv : entriesKeysValues) {
      final Entry<byte[]> entry = EncoderDecoder.decodeEntry(kv.getValue(), streamName);
      entries.add(entry);
    }
    return entries;
  }
}
