// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import io.vlingo.common.serialization.JsonSerialization;
import io.vlingo.symbio.Entry.BinaryEntry;
import io.vlingo.symbio.EntryAdapter;

public class SprintPlannedEntryAdapter implements EntryAdapter<SprintPlanned,BinaryEntry> {

  @Override
  public SprintPlanned fromEntry(final BinaryEntry entry) {
    return JsonSerialization.deserialized(new String(entry.entryData), SprintPlanned.class);
  }

  @Override
  public BinaryEntry toEntry(final SprintPlanned source) {
    return toEntry(source, source.sprintId);
  }

  @Override
  public BinaryEntry toEntry(final SprintPlanned source, final String id) {
    final String serialization = JsonSerialization.serialized(source);
    return new BinaryEntry(id, SprintPlanned.class, 1, serialization.getBytes());
  }
}
