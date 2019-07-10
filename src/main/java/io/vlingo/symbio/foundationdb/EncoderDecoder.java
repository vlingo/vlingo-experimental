// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import io.vlingo.symbio.BaseEntry;
import io.vlingo.symbio.Entry;
import io.vlingo.symbio.State;
import io.vlingo.symbio.State.BinaryState;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Encoder and decoder of binary types.
 */
public class EncoderDecoder {
  private final Map<String,byte[]> typeEncodings;

  EncoderDecoder() {
    this.typeEncodings = new HashMap<>();
  }

  static BaseEntry.BinaryEntry decodeEntry(final byte[] encoding, final String id) throws Exception {
    return decode(encoding, id, false);
  }

  static State<byte[]> decodeState(final byte[] encoding, final String id) throws Exception {
    return decode(encoding, id, true);
  }

  @SuppressWarnings("unchecked")
  static <T> T decode(final byte[] encoding, final String id, final boolean hasDataVersion) throws Exception {
    final ByteBuffer buffer = ByteBuffer.wrap(encoding);
    final String decodedId = decodeId(id, buffer);
    final String typeName = decodeTypeName(buffer);
    final int typeVersion = buffer.getShort();
    final int dataLength = buffer.getInt();
    final byte[] data = new byte[dataLength];
    buffer.get(data, 0, dataLength);

    if (hasDataVersion) {
      return (T) new BinaryState(decodedId, Class.forName(typeName), typeVersion, data, buffer.getInt());
    }
    return (T) new BaseEntry.BinaryEntry(decodedId, Class.forName(typeName), typeVersion, data);
  }

  private static String decodeId(final String id, final ByteBuffer buffer) {
    final short idLength = buffer.getShort();
    if (idLength > 0) {
      final byte[] typeEncoding = new byte[idLength];
      buffer.get(typeEncoding, 0, idLength);
      return new String(typeEncoding);
    }
    return id;
  }

  private static String decodeTypeName(final ByteBuffer buffer) {
    final short typeLength = buffer.getShort();
    final byte[] typeEncoding = new byte[typeLength];
    buffer.get(typeEncoding, 0, typeLength);
    final String typeName = new String(typeEncoding);
    return typeName;
  }

  byte[] encode(final Entry<byte[]> entry) {
    return encode(entry.id(), entry.type(), entry.typeVersion(), entry.entryData(), -1);
  }

  byte[] encode(final State<byte[]> state) {
    return encode(state.id, state.type, state.typeVersion, state.data, state.dataVersion);
  }

  byte[] encode(final String id, final Class<?> type, final int typeVersion, final byte[] data) {
    return encode(id, type.getName(), typeVersion, data, -1);
  }

  byte[] encode(final String id, final Class<?> type, final int typeVersion, final byte[] data, final int dataVersion) {
    return encode(id, type.getName(), typeVersion, data, dataVersion);
  }

  byte[] encode(final String id, final String typeName, final int typeVersion, final byte[] data, final int dataVersion) {
    final int idLengthLength = Short.BYTES;
    final int idLength = (id == null ? 0 :  id.length());
    final int dataVersionLength = dataVersion > 0 ? Integer.BYTES : 0;
    final byte[] typeEncoding = encodeType(typeName);
    final int length =
            idLengthLength + idLength +           // id
            Short.BYTES + typeEncoding.length +   // type
            Short.BYTES +                         // type version
            Integer.BYTES + data.length +         // data
            dataVersionLength;                    // data version

    final ByteBuffer buffer = ByteBuffer.allocate(length);

    buffer.putShort((short) idLength);
    if (idLength > 0) buffer.put(id.getBytes());
    buffer.putShort((short) typeEncoding.length);
    buffer.put(typeEncoding);
    buffer.putShort((short) typeVersion);
    buffer.putInt(data.length);
    buffer.put(data);
    if (dataVersionLength > 0) buffer.putInt(dataVersion);
    buffer.flip();
    return buffer.array();
  }

  byte[] encodeType(final String typeName) {
    byte[] typeEncoding = typeEncodings.get(typeName);
    if (typeEncoding == null) {
      typeEncoding = typeName.getBytes();
      typeEncodings.put(typeName, typeEncoding);
    }
    return typeEncoding;
  }
}
