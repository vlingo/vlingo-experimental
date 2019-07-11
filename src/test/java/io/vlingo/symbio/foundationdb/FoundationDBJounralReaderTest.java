// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

import io.vlingo.symbio.Entry;

public class FoundationDBJounralReaderTest extends BaseFoundationDBJounralTest {

  @Test
  public void testThatReaderReadsAllOneAtATime() {
    final int sets = 10;
    final int total = KindsOfEvents * sets;

    dispatcher.afterCompleting(sets);

    appendSetsOfEvents(sets);

    assertEquals(total, dispatcher.entryElements());

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

    dispatcher.afterCompleting(sets);

    appendSetsOfEvents(sets);

    assertEquals(total, dispatcher.entryElements());

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
