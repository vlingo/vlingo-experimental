// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.vlingo.actors.testkit.TestUntil;
import io.vlingo.symbio.Entry;
import io.vlingo.symbio.store.journal.Stream;

public class FoundationDBStreamReaderTest extends BaseFoundationDBJounralTest {

  @Test
  public void testThatReadersAllStreams() {
    final int sets = 30;
    final int total = KindsOfEvents * sets;

    final TestUntil until = listener.untilHappenings(sets);

    appendSetsOfEvents(sets);

    until.completes();

    int countOfEntries = 0;
    int index = 0;
    for ( ; index < sets; ++index) {
      final String streamName = streamNameFor(index);

      final Stream<byte[]> stream = streamReader.streamFor(streamName).await();

      assertNotNull(stream);
      assertEquals(KindsOfEvents, stream.entries.size());

      for (int entryIndex = 1; entryIndex <= KindsOfEvents; ++entryIndex) {
        final Entry<byte[]> entry = stream.entries.get(entryIndex - 1);
        ++countOfEntries;
        switch (entryIndex % KindsOfEvents) {
        case 1:
          assertEquals(ProductCreated.class.getName(), entry.type);
          break;
        case 2:
          assertEquals(SprintPlanned.class.getName(), entry.type);
          break;
        case 0:
          assertEquals(BacklogItemCommitted.class.getName(), entry.type);
          break;
        default:
          assertEquals("Should not be reachable.", 0, 1);
        }
      }
    }

    assertEquals(total, countOfEntries);
  }
}
