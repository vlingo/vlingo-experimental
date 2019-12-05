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

public class ProductCreatedEntryAdapter implements EntryAdapter<ProductCreated, BinaryEntry> {

  @Override
  public ProductCreated fromEntry(final BinaryEntry entry) {
    return JsonSerialization.deserialized(new String(entry.entryData()), ProductCreated.class);
  }

  @Override
  public BinaryEntry toEntry(final ProductCreated source, final int version, final String id, final Metadata metadata) {
    final String serialization = JsonSerialization.serialized(source);
    return new BinaryEntry(id, ProductCreated.class, 1, serialization.getBytes(), version, metadata);
  }

  @Override
  public BinaryEntry toEntry(final ProductCreated source, final Metadata metadata) {
    return toEntry(source, source.productId, metadata);
  }

  @Override
  public BinaryEntry toEntry(final ProductCreated source, final String id, final Metadata metadata) {
    final String serialization = JsonSerialization.serialized(source);
    return new BinaryEntry(id, ProductCreated.class, 1, serialization.getBytes(), metadata);
  }
}
