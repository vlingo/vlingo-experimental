// Copyright © 2012-2019 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.pipes.sources;

import io.vlingo.xoom.actors.Stage;
import io.vlingo.xoom.pipes.Record;
import io.vlingo.xoom.pipes.Source;
import io.vlingo.xoom.pipes.Stream;
import io.vlingo.xoom.pipes.actor.Materialized;
import io.vlingo.xoom.pipes.actor.MaterializedSource;
import io.vlingo.xoom.pipes.actor.MaterializedSourceActor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class SupplierSource<T> implements Source<T> {
    private final Supplier<T> supplier;

    public SupplierSource(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public static <T> SupplierSource<T> fromSupplier(Supplier<T> t) {
        return new SupplierSource<>(t);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Record<T>[]> poll() {
        Record<T>[] result = new Record[] { Record.of(supplier.get()) };
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public Materialized materialize(Stage stage, MaterializedSource source) {
        return stage.actorFor(MaterializedSource.class, MaterializedSourceActor.class, this, Stream.DEFAULT_POLL_INTERVAL);
    }
}
