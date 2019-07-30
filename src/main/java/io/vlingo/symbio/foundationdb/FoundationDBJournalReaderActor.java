// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.KeySelector;
import com.apple.foundationdb.KeyValue;
import com.apple.foundationdb.tuple.Tuple;

import io.vlingo.actors.Actor;
import io.vlingo.common.Completes;
import io.vlingo.symbio.BaseEntry.BinaryEntry;
import io.vlingo.symbio.store.journal.JournalReader;

/**
 * Actor-based {@code JournalReader} over FoundationDB.
 */
public class FoundationDBJournalReaderActor extends Actor implements JournalReader<BinaryEntry> {
  private static final byte[] StartingId = new byte[0];
  private static final byte[] EndId = new byte[] { (byte) 0xFF };

  private boolean afterCurrent;
  private byte[] currentId;
  private final Database database;
  private final byte[] entriesSubspaceKey;
  private final String name;

  public FoundationDBJournalReaderActor(final String name, final byte[] entriesSubspaceKey) {
    this.name = name;
    this.entriesSubspaceKey = entriesSubspaceKey;
    this.currentId = StartingId;
    this.afterCurrent = false;
    this.database = database();
  }

  @Override
  public void close() {
    this.database.close();
  }

  /*
   * @see io.vlingo.symbio.store.journal.JournalReader#name()
   */
  @Override
  public Completes<String> name() {
    return completes().with(name);
  }

  /*
   * @see io.vlingo.symbio.store.journal.JournalReader#readNext()
   */
  @Override
  public Completes<BinaryEntry> readNext() {
    final KeySelector begin = afterCurrent ? keyAfter(currentId) : keyEqualishTo(currentId);
    final KeySelector end = begin.add(1);
    final List<BinaryEntry> maybeOne = readFromTo(begin, end, 1);
    return completes().with(maybeOne.isEmpty() ? null : maybeOne.get(0));
  }

  /*
   * @see io.vlingo.symbio.store.journal.JournalReader#readNext(int)
   */
  @Override
  public Completes<List<BinaryEntry>> readNext(final int maximumEntries) {
    final KeySelector begin = afterCurrent ? keyAfter(currentId) : keyEqualishTo(currentId);
    final KeySelector end = begin.add(maximumEntries);
    final List<BinaryEntry> entries = readFromTo(begin, end, maximumEntries);
    return completes().with(entries);
  }

  /*
   * @see io.vlingo.symbio.store.journal.JournalReader#rewind()
   */
  @Override
  public void rewind() {
    currentId = StartingId;
    afterCurrent = false;
  }

  /*
   * @see io.vlingo.symbio.store.journal.JournalReader#seekTo(java.lang.String)
   */
  @Override
  public Completes<String> seekTo(final String id) {
    switch (id) {
    case Beginning:
      rewind();
      break;
    case End:
      fastForward();
      break;
    case Query:
      break;
    default:
      currentId = KeyConverter.toVersionTimestamp(id);
      break;
    }
    return completes().with(KeyConverter.fromVersionTimestamp(currentId));
  }

  @Override
  public Completes<Long> size() {
    // unsupported:
    // https://forums.foundationdb.org/t/getting-the-number-of-key-value-pairs/189

    return completes().with(-1L);
  }

  private Database database() {
    final FDB fdb = FDB.selectAPIVersion(600);
    return fdb.open();
  }

  private void fastForward() {
    currentId = EndId;
  }

  private List<BinaryEntry> readFromTo(final KeySelector begin, final KeySelector end, final int limit) {

    final byte[] saveId = currentId;

    final List<BinaryEntry> entries = database.run(txn -> {
      try {
        final List<BinaryEntry> all = new ArrayList<>(limit);
        final List<KeyValue> keysValues = txn.getRange(begin, end, limit, false).asList().get();
        for (KeyValue kv : keysValues) {
          final Tuple keySegments = Tuple.fromBytes(kv.getKey());
          if (!preserveCurrentId(keySegments))
            break;
          final String id = KeyConverter.fromVersionTimestamp(currentId);
          final BinaryEntry entry = EncoderDecoder.decodeEntry(kv.getValue(), id);
          all.add(entry);
        }
        if (currentId != saveId) {
          afterCurrent = true;
        }
        return all;
      } catch (Throwable t) {
        logger().error("JournalReader '" + name + "' failed to read next because: " + t.getMessage(), t);
      }

      currentId = saveId;

      return null;
    });

    return entries != null ? entries : Collections.emptyList();
  }

  private KeySelector keyAfter(final byte[] entryKeySegment) {
    return KeySelector.firstGreaterThan(Tuple.from(entriesSubspaceKey, entryKeySegment).pack());
  }

  private KeySelector keyEqualishTo(final byte[] entryKeySegment) {
    return KeySelector.firstGreaterOrEqual(Tuple.from(entriesSubspaceKey, entryKeySegment).pack());
  }

  /**
   * Answer whether or not the currentId was successfully preserved.
   *
   * @param keySegments the Tuple
   * @return boolean
   */
  private boolean preserveCurrentId(final Tuple keySegments) {
    try {
      // Note: Unknown why, but when the getRange() reads past the
      // end of the journal, the keySegments look something like this:
      // SEG 0: [B@3db228c8
      // SEG 1: product0
      // SEG 2: 1
      // This is not compatible with currentId because the key
      // at index 1 is a String, not a byte[], which is totally
      // bogus and unrelated to the kinds of keys used. Also
      // the String "product0" is not even a key used anywhere.
      // I cannot currently find how to detect/recognize this by
      // other means. Please inform if you understand this.

      currentId = keySegments.getBytes(1);
      return true;
    } catch (Exception e) {
      fastForward();
      return false;
    }
  }
}
