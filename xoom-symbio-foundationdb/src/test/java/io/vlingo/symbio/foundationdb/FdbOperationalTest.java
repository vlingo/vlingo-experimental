// Copyright © 2012-2020 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import org.junit.Test;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import com.apple.foundationdb.tuple.Tuple;

/**
 * FoundationDB up and running.
 */
public class FdbOperationalTest {

  @Test
  public void testThatKeyValueSetsGets() {
    final FDB fdb = FDB.selectAPIVersion(600);

    try(Database db = fdb.open()) {
      // Run an operation on the database
      db.run(tr -> {
        tr.set(Tuple.from("hello").pack(), Tuple.from("world").pack());
        return null;
      });

      // Get the value of 'hello' from the database
      String hello = db.run(tr -> {
        byte[] result = tr.get(Tuple.from("hello").pack()).join();
        return Tuple.fromBytes(result).getString(0);
      });

      System.out.println("Hello " + hello);
    }
  }
}