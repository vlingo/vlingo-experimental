// Copyright © 2012-2019 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.pipes.http.handler;

import io.vlingo.xoom.actors.Logger;
import io.vlingo.xoom.actors.Stage;
import io.vlingo.xoom.common.Completes;
import io.vlingo.xoom.http.Method;
import io.vlingo.xoom.http.Request;
import io.vlingo.xoom.http.Response;
import io.vlingo.xoom.http.resource.Action;
import io.vlingo.xoom.http.resource.RequestHandler;
import io.vlingo.xoom.pipes.Record;
import io.vlingo.xoom.pipes.Source;
import io.vlingo.xoom.pipes.Stream;
import io.vlingo.xoom.pipes.actor.Materialized;
import io.vlingo.xoom.pipes.actor.MaterializedSource;
import io.vlingo.xoom.pipes.actor.MaterializedSourceActor;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class RawRequestHandler extends RequestHandler implements Source<Request> {
    private final Stage stage;
    private final Queue<Record<Request>> input;

    public RawRequestHandler(Method method, String path, Stage stage) {
        super(method, path, Collections.emptyList());
        this.stage = stage;
        this.input = new ArrayDeque<>(32);
    }

    @Override
    protected Completes<Response> execute(Request var1, Action.MappedParameters var2, Logger var3) {
        Completes<Response> completes = Completes.using(stage.scheduler());
        Record<Request> request = Record.of(var1).withMetadata("completes", completes);

        input.add(request);
        return completes;
    }

    @Override
    public CompletableFuture<Record<Request>[]> poll() {
        CompletableFuture<Record<Request>[]> result = CompletableFuture.completedFuture(input.toArray(new Record[0]));
        input.clear();

        return result;
    }

    @Override
    public Materialized materialize(Stage stage, MaterializedSource source) {
        return stage.actorFor(MaterializedSource.class, MaterializedSourceActor.class, this, Stream.DEFAULT_POLL_INTERVAL);
    }
}
