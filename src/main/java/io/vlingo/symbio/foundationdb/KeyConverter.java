// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

/**
 * Conversion functions for keys.
 */
public class KeyConverter {

  /**
   * Answer the {@code String} representation of the {@code versionTimestamp}.
   * @return String
   */
  public static String fromVersionTimestamp(final byte[] versionTimestamp) {
    final StringBuilder builder = new StringBuilder(versionTimestamp.length * 2);
    for (final byte byteValue : versionTimestamp) {
      final int intValue = Byte.toUnsignedInt(byteValue);
      builder.append(String.format("%02x", intValue));
    }
    return builder.toString().toUpperCase();
  }

  /**
   * Answer the {@code byte[]} of the version timestamp {@code representation}.
   * @param representation the String representing the version timestamp value
   * @return byte[]
   */
  public static byte[] toVersionTimestamp(final String representation) {
    final byte[] versionTimestamp = new byte[representation.length() / 2];
    int versionTimestampIndex = 0;
    for (int idx = 0; idx < representation.length(); idx += 2) {
      final String hexDigits = representation.substring(idx, idx + 2);
      versionTimestamp[versionTimestampIndex++] = (byte) (Integer.parseInt(hexDigits, 16) & 0xff);
    }
    return versionTimestamp;
  }
}
