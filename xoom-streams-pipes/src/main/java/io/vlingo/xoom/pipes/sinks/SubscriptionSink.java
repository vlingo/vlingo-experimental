// Copyright © 2012-2019 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.pipes.sinks;

import io.vlingo.xoom.actors.Stage;
import io.vlingo.xoom.pipes.Record;
import io.vlingo.xoom.pipes.Sink;
import io.vlingo.xoom.pipes.Stream;
import io.vlingo.xoom.pipes.actor.Materialized;
import io.vlingo.xoom.pipes.actor.MaterializedSinkActor;
import io.vlingo.xoom.pipes.actor.MaterializedSource;

import java.util.function.Consumer;

public class SubscriptionSink<T> implements Sink<T> {
    private final Consumer<T> subscriber;

    private SubscriptionSink(Consumer<T> subscriber) {
        this.subscriber = subscriber;
    }

    public static <T> SubscriptionSink<T> subscribingWith(Consumer<T> subscriber) {
        return new SubscriptionSink<>(subscriber);
    }

    @Override
    public void whenValue(Record<T> value) {
        subscriber.accept(value.value());
    }

    @Override
    public Materialized materialize(Stage stage, MaterializedSource source) {
        return stage.actorFor(Materialized.class, MaterializedSinkActor.class, source, this, Stream.DEFAULT_POLL_INTERVAL);
    }
}
