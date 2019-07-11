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

import io.vlingo.symbio.Entry;
import io.vlingo.symbio.store.journal.Stream;

public class FoundationDBStreamReaderTest extends BaseFoundationDBJounralTest {

  @Test
  public void testThatReadsAllStreams() {
    final int sets = 30;
    final int total = KindsOfEvents * sets;

    dispatcher.afterCompleting(sets);
//long startTime1 = System.currentTimeMillis();
    appendSetsOfEvents(sets);
    assertEquals(total, dispatcher.entryElements());
//long endTime1 = System.currentTimeMillis();
//System.out.println("APPEND TIME: " + (endTime1 - startTime1));

    int countOfEntries = 0;
    int index = 0;
    for ( ; index < sets; ++index) {
      final String streamName = streamNameFor(index);
//long startTime2 = System.currentTimeMillis();
      final Stream<byte[]> stream = streamReader.streamFor(streamName).await();
//long endTime2 = System.currentTimeMillis();
//System.out.println("READ TIME: " + (endTime2 - startTime2));

      assertNotNull(stream);
      assertEquals(KindsOfEvents, stream.size());

      for (int entryIndex = 1; entryIndex <= stream.size()/*KindsOfEvents*/; ++entryIndex) {
        final Entry<byte[]> entry = stream.entries.get(entryIndex - 1);
        ++countOfEntries;
        switch (entryIndex % KindsOfEvents) {
        case 1:
          assertEquals(ProductCreated.class.getName(), entry.type());
          break;
        case 2:
          assertEquals(SprintPlanned.class.getName(), entry.type());
          break;
        case 0:
          assertEquals(BacklogItemCommitted.class.getName(), entry.type());
          break;
        default:
          assertEquals("Should not be reachable.", 0, 1);
        }
      }
    }

    assertEquals(total, countOfEntries);
  }

  @Test
  public void testThatReadsFullStream() {
    final int total = 101;

    dispatcher.afterCompleting(1); // total
long startTime1 = System.currentTimeMillis();
    //appendEvents(total);
    appendEventsBatch(total);
    assertEquals(total, dispatcher.entryElements());
long endTime1 = System.currentTimeMillis();
System.out.println("APPEND TIME: " + (endTime1 - startTime1));

    final String streamName = streamNameFor(total);
long startTime2 = System.currentTimeMillis();
System.out.println("FINDING FOR STREAM: " + streamName);
    final Stream<byte[]> stream = streamReader.streamFor(streamName).await();
long endTime2 = System.currentTimeMillis();
System.out.println("READ TIME: " + (endTime2 - startTime2));

    assertNotNull(stream);
    assertEquals(total, stream.size());

    final Entry<byte[]> productCreatedEntry = stream.entries.get(0);
    assertEquals(ProductCreated.class.getName(), productCreatedEntry.type());
    int countOfEntries = 1;

    for (int entryIndex = 2; entryIndex <= stream.size(); ++entryIndex) {
      final Entry<byte[]> entry = stream.entries.get(entryIndex - 1);
      ++countOfEntries;
      final int oddEvent = entryIndex % 2;
      switch (oddEvent) {
      case 0:
        assertEquals(SprintPlanned.class.getName(), entry.type());
        break;
      case 1:
        assertEquals(BacklogItemCommitted.class.getName(), entry.type());
        break;
      default:
        assertEquals("Should not be reachable.", 0, 1);
      }
    }

    assertEquals(total, countOfEntries);
  }
}
