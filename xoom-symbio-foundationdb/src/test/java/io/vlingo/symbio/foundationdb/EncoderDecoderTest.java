// Copyright © 2012-2020 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import io.vlingo.common.serialization.JsonSerialization;
import io.vlingo.symbio.BaseEntry.BinaryEntry;
import io.vlingo.symbio.Entry;
import io.vlingo.symbio.State;
import io.vlingo.symbio.State.BinaryState;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class EncoderDecoderTest {

  @Test
  public void testThatEntryEncodesDecodes() throws Exception {
    final String hexId = "B12C314FD5A67E819450";
    final TestType instance = new TestType(hexId, "Tom Jerry", 12);
    final byte[] hexIdBytes = KeyConverter.toVersionTimestamp(hexId);
    final byte[] instanceIdBytes = KeyConverter.toVersionTimestamp(instance.id);
    assertArrayEquals(hexIdBytes, instanceIdBytes);
    final String data = JsonSerialization.serialized(instance);
    final BinaryEntry entry1 = new BinaryEntry(hexId, TestType.class, 2, data.getBytes());
    final EncoderDecoder encoding = new EncoderDecoder();
    final byte[] encoding1 = encoding.encode(entry1);
    final Entry<byte[]> entry2 = EncoderDecoder.decodeEntry(encoding1, hexId);
    assertEquals(hexId, entry2.id());
    assertEquals(entry1.id(), entry2.id());
    assertArrayEquals(entry1.entryData(), entry2.entryData());
  }

  @Test
  public void testThatStateEncodesDecodes() throws Exception {
    final String hexId = "F0ECD17FBFACEED1A3FF";
    final TestType instance = new TestType(hexId, "Wile E Coyote", 999);
    final byte[] hexIdBytes = KeyConverter.toVersionTimestamp(hexId);
    final byte[] instanceIdBytes = KeyConverter.toVersionTimestamp(instance.id);
    assertArrayEquals(hexIdBytes, instanceIdBytes);
    final String data = JsonSerialization.serialized(instance);
    final BinaryState state1 = new BinaryState(hexId, TestType.class, 2, data.getBytes(), 4);
    final EncoderDecoder encoding = new EncoderDecoder();
    final byte[] encoding1 = encoding.encode(state1);
    final State<byte[]> state2 = EncoderDecoder.decodeState(encoding1, hexId);
    assertEquals(hexId, state2.id);
    assertEquals(state1.id, state2.id);
    assertEquals(state1.type, state2.type);
    assertEquals(state1.typeVersion, state2.typeVersion);
    assertArrayEquals(state1.data, state2.data);
    assertEquals(state1.dataVersion, state2.dataVersion);
  }
}
