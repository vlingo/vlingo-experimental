// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import io.vlingo.actors.testkit.TestUntil;
import io.vlingo.symbio.Entry;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class FoundationDBJounralReaderTest extends BaseFoundationDBJounralTest {

  @Test
  public void testThatReaderReadsAllOneAtATime() {
    final int sets = 10;
    final int total = KindsOfEvents * sets;

    final TestUntil until = dispatcher.untilHappenings(sets);

    appendSetsOfEvents(sets);

    until.completes();

    int index = 1;
    for ( ; index <= total; ++index) {
      final Entry<byte[]> entry = journalReader.readNext().await();

      assertNotNull(entry);

      switch (index % KindsOfEvents) {
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

    assertEquals(total + 1, index);
    final Entry<byte[]> entry = journalReader.readNext().await();
    assertNull(entry);
  }

  @Test
  public void testThatReaderReadsAllAtOnce() {
    final int sets = 20;
    final int total = KindsOfEvents * sets;

    final TestUntil until = dispatcher.untilHappenings(sets);

    appendSetsOfEvents(sets);

    until.completes();

    final List<Entry<byte[]>> entries = journalReader.readNext(total).await();
    int index = 1;
    for ( ; index <= total; ++index) {
      final Entry<byte[]> entry = entries.get(index - 1);

      assertNotNull(entry);

      switch (index % KindsOfEvents) {
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

    assertEquals(total + 1, index);
    final Entry<byte[]> entry = journalReader.readNext().await();
    assertNull(entry);
  }
}
