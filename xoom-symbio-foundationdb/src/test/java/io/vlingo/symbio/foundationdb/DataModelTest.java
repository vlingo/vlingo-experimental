// Copyright © 2012-2020 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.MutationType;
import com.apple.foundationdb.directory.Directory;
import com.apple.foundationdb.directory.DirectoryLayer;
import com.apple.foundationdb.directory.DirectorySubspace;
import com.apple.foundationdb.subspace.Subspace;
import com.apple.foundationdb.tuple.Tuple;
import com.apple.foundationdb.tuple.Versionstamp;

/**
 * Experiments for learning about some FoundationDB features.
 */
public class DataModelTest {
  private int userVersion;

  @Test
  public void testThatKeysUnpack() throws Exception {
    final FDB fdb = FDB.selectAPIVersion(600);
    final Database database = fdb.open();
    
    final Subspace nodeSubspace = new Subspace(Tuple.from("io.vlingo.streams"));
    final Directory directory = DirectoryLayer.createWithContentSubspace(nodeSubspace);

    final List<String> contentSubspaces = Arrays.asList("io.vlingo.streams", "com.abc", "journal", "accounts");
    final DirectorySubspace directorySubspace = directory.createOrOpen(database, contentSubspaces).get();
    final byte[] contentKey = directorySubspace.getKey();

    final CompletableFuture<byte[]> trVersionFuture = database.run(txn -> {
      // The incomplete Versionstamp will be overwritten with tr's version information when committed.
      final Versionstamp version = Versionstamp.incomplete(20);
      final Tuple template = Tuple.from(version);
      txn.mutate(MutationType.SET_VERSIONSTAMPED_KEY, template.packWithVersionstamp(), new byte[0]);
      return txn.getVersionstamp();
      });

    final byte[] entryKey = trVersionFuture.get();
    show("1: RAW KEY: ", entryKey);

    final Tuple original = Tuple.from(contentKey, entryKey);
    show("2: ORG KEY: ", original.getBytes(1));

    final byte[] keyPath = original.pack();

    final Tuple restored = Tuple.fromBytes(keyPath);
    show("3: RES KEY: ", restored.getBytes(1));

    assertEquals(original, restored);
    assertArrayEquals(entryKey, restored.getBytes(1));
    assertArrayEquals(original.getBytes(1), restored.getBytes(1));
  }

  @Test
  public void testThatDataStructures() throws Exception {
    final FDB fdb = FDB.selectAPIVersion(600);

    final Database database = fdb.open();
    
    final Subspace nodeSubspace = new Subspace(Tuple.from("io.vlingo.streams"));
    final Directory directory = DirectoryLayer.createWithContentSubspace(nodeSubspace);
    final byte[] directoryKey = directory.getLayer();
    show("DIRECTORY", directoryKey);

    final List<String> contentSubspaces = Arrays.asList("io.vlingo.streams", "com.abc", "journal", "accounts");
    final DirectorySubspace directorySubspace = directory.createOrOpen(database, contentSubspaces).get();
    final byte[] contentKey = directorySubspace.getKey();
    for (final String name : directorySubspace.getPath()) {
      System.out.print("/"+name);
    }
    System.out.println();
    show("  CONTENT", contentKey);

    for (int idx = 1; idx <= 10; ++idx) {
      userVersion = idx;
      final CompletableFuture<byte[]> trVersionFuture = database.run(txn -> {
        // The incomplete Versionstamp will be overwritten with tr's version information when committed.
        final Versionstamp version = Versionstamp.incomplete(userVersion);
        final Tuple template = Tuple.from(version);
        txn.mutate(MutationType.SET_VERSIONSTAMPED_KEY, template.packWithVersionstamp(), new byte[0]);
        return txn.getVersionstamp();
        });

      final byte[] entryKey = trVersionFuture.get();

      final byte[] keyPath = Tuple.from(contentKey, entryKey).pack();
      show("  KEYPATH", keyPath);

      final String data = "event" + idx;

      database.run(txn -> {
        txn.set(keyPath, Tuple.from(data).pack());
        return null;
      });

      final String event = database.run(txn -> {
        byte[] result = txn.get(keyPath).join();
        return Tuple.fromBytes(result).getString(0);
      });

      System.out.println("KEY(" + idx + "): QUERY EVENT: " + event);
    }
  }

  private void show(final String name, final byte[] key) {
//    System.out.print("KEY " + name + " (" + key.length + "): ");
//    if (key.length == 0) {
//      System.out.print("[]");
//    }
//    for (int i = 0; i < key.length; i++)
//    {
//      int val = Byte.toUnsignedInt(key[i]);
//      System.out.print("[" + val +"]");
//    }
//    System.out.println();
  }
}
