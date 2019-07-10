// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import io.vlingo.actors.testkit.TestUntil;
import io.vlingo.symbio.Entry;
import io.vlingo.symbio.State.BinaryState;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FoundationDBJounralTest extends BaseFoundationDBJounralTest {

  @Test
  public void testThatJounralAppendsOne() {
    final TestUntil until = dispatcher.untilHappenings(1);

    final ProductCreated productCreated = new ProductCreated("product1");

    journal.append(productCreated.productId, 1, productCreated, null, null);

    until.completes();

    assertEquals(1, dispatcher.entryElements());

    final Entry<byte[]> entry = dispatcher.entry(0);

    assertNotNull(entry);
    assertNotNull(entry.id());
    assertEquals(ProductCreated.class.getName(), entry.type());
  }

  @Test
  public void testThatJounralAppendsOneWithSnapshot() {
    final TestUntil until = dispatcher.untilHappenings(1);

    final ProductCreated productCreated = new ProductCreated("product2");

    final TestType testType = new TestType("product2", "Product2", 1);

    journal.appendWith(productCreated.productId, 1, productCreated, testType, null, null);

    until.completes();

    assertEquals(1, dispatcher.entryElements());

    final Entry<byte[]> entry = dispatcher.entry(0);

    assertNotNull(entry);
    assertNotNull(entry.id());
    assertEquals(ProductCreated.class.getName(), entry.type());

    final BinaryState snapshot = dispatcher.snapshot();
    assertNotNull(snapshot);
    final TestTypeStateAdapter adapter = new TestTypeStateAdapter();
    final TestType testType2 = adapter.fromRawState(snapshot);
    assertEquals(testType2.id, testType.id);
    assertEquals(testType2.name, testType.name);
    assertEquals(testType2.age, testType.age);
  }

  @Test
  public void testThatJounralAppendsMultiple() {
    final TestUntil until = dispatcher.untilHappenings(1);

    final ProductCreated productCreated = new ProductCreated("product1");
    final SprintPlanned sprintPlanned = new SprintPlanned("sprint1");
    final BacklogItemCommitted backlogItemCommitted = new BacklogItemCommitted("backlogItem1", "sprint1");

    journal.appendAll(productCreated.productId, 1, Arrays.asList(productCreated, sprintPlanned, backlogItemCommitted), null, null);

    until.completes();

    assertEquals(3, dispatcher.entryElements());

    final Entry<byte[]> entry1 = dispatcher.entry(0);
    assertNotNull(entry1.id());
    assertEquals(ProductCreated.class.getName(), entry1.type());

    final Entry<byte[]> entry2 = dispatcher.entry(1);
    assertNotNull(entry2.id());
    assertEquals(SprintPlanned.class.getName(), entry2.type());

    final Entry<byte[]> entry3 = dispatcher.entry(2);
    assertNotNull(entry3.id());
    assertEquals(BacklogItemCommitted.class.getName(), entry3.type());
  }

  @Test
  public void testThatJounralAppendsMultipleWithSnapshot() {
    final TestUntil until = dispatcher.untilHappenings(1);

    final ProductCreated productCreated = new ProductCreated("product3");
    final SprintPlanned sprintPlanned = new SprintPlanned("sprint3");
    final BacklogItemCommitted backlogItemCommitted = new BacklogItemCommitted("backlogItem3", "sprint3");

    final TestType testType = new TestType("product3", "Product3", 1);

    journal.appendAllWith(productCreated.productId, 1, Arrays.asList(productCreated, sprintPlanned, backlogItemCommitted), testType, null, null);

    until.completes();

    assertEquals(3, dispatcher.entryElements());

    final Entry<byte[]> entry1 = dispatcher.entry(0);
    assertNotNull(entry1.id());
    assertEquals(ProductCreated.class.getName(), entry1.type());

    final Entry<byte[]> entry2 = dispatcher.entry(1);
    assertNotNull(entry2.id());
    assertEquals(SprintPlanned.class.getName(), entry2.type());

    final Entry<byte[]> entry3 = dispatcher.entry(2);
    assertNotNull(entry3.id());
    assertEquals(BacklogItemCommitted.class.getName(), entry3.type());

    final BinaryState snapshot = dispatcher.snapshot();
    assertNotNull(snapshot);
    final TestTypeStateAdapter adapter = new TestTypeStateAdapter();
    final TestType testType2 = adapter.fromRawState(snapshot);
    assertEquals(testType2.id, testType.id);
    assertEquals(testType2.name, testType.name);
    assertEquals(testType2.age, testType.age);
  }
}
