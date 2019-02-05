// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KeyConverterTest {

  @Test
  public void testForCorrectConversions() {
    final String hexId = "B1FE004FD5A67E8194FF";

    final byte[] versionTimestamp1 = new byte[] {
        (byte) 0xB1, (byte) 0xFE, (byte) 0x00, (byte) 0x4F, (byte) 0xD5,
        (byte) 0xA6, (byte) 0x7E, (byte) 0x81, (byte) 0x94, (byte) 0xFF };
    
    final byte[] versionTimestamp2 = new byte[] {
        (byte)  -79, (byte)   -2, (byte)    0, (byte)   79, (byte)  -43,
        (byte)  -90, (byte)  126, (byte) -127, (byte) -108, (byte)   -1 };

    final byte[] idVersionTimestamp = KeyConverter.toVersionTimestamp(hexId);
    assertArrayEquals(versionTimestamp2, idVersionTimestamp);
    assertArrayEquals(versionTimestamp1, idVersionTimestamp);

    final String expectedHexId = KeyConverter.fromVersionTimestamp(idVersionTimestamp);
    assertEquals(hexId, expectedHexId);
  }
}
