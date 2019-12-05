// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import io.vlingo.common.serialization.JsonSerialization;
import io.vlingo.symbio.BaseEntry.BinaryEntry;
import io.vlingo.symbio.EntryAdapter;
import io.vlingo.symbio.Metadata;

public class BacklogItemCommittedEntryAdapter implements EntryAdapter<BacklogItemCommitted, BinaryEntry> {

  @Override
  public BacklogItemCommitted fromEntry(final BinaryEntry entry) {
    return JsonSerialization.deserialized(new String(entry.entryData()), BacklogItemCommitted.class);
  }

  @Override
  public BinaryEntry toEntry(final BacklogItemCommitted source, final int version, final String id, final Metadata metadata) {
    final String serialization = JsonSerialization.serialized(source);
    return new BinaryEntry(id, BacklogItemCommitted.class, 1, serialization.getBytes(), version, metadata);
  }

  @Override
  public BinaryEntry toEntry(final BacklogItemCommitted source, final Metadata metadata) {
    return toEntry(source, source.backlogItemId, metadata);
  }

  @Override
  public BinaryEntry toEntry(final BacklogItemCommitted source, final String id, final Metadata metadata) {
    final String serialization = JsonSerialization.serialized(source);
    return new BinaryEntry(id, BacklogItemCommitted.class, 1, serialization.getBytes(), metadata);
  }
}
