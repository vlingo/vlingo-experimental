// Copyright © 2012-2019 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.pipes.http.handler;

import io.vlingo.xoom.actors.Stage;
import io.vlingo.xoom.common.Completes;
import io.vlingo.xoom.http.Response;
import io.vlingo.xoom.pipes.Record;
import io.vlingo.xoom.pipes.Sink;
import io.vlingo.xoom.pipes.Stream;
import io.vlingo.xoom.pipes.actor.Materialized;
import io.vlingo.xoom.pipes.actor.MaterializedSinkActor;
import io.vlingo.xoom.pipes.actor.MaterializedSource;

public class ResponseSender implements Sink<Response> {
    @Override
    public void whenValue(Record<Response> value) {
        Completes<Response> completes = (Completes<Response>) value.metadata().get("completes");
        completes.with(value.value());
    }

    @Override
    public Materialized materialize(Stage stage, MaterializedSource source) {
        return stage.actorFor(Materialized.class, MaterializedSinkActor.class, source, this, Stream.DEFAULT_POLL_INTERVAL);
    }
}
