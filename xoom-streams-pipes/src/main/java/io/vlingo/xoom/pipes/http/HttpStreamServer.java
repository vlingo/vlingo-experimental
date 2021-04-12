// Copyright © 2012-2019 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.pipes.http;

import io.vlingo.xoom.actors.Stage;
import io.vlingo.xoom.http.Method;
import io.vlingo.xoom.http.Request;
import io.vlingo.xoom.http.Response;
import io.vlingo.xoom.http.resource.*;
import io.vlingo.xoom.pipes.Sink;
import io.vlingo.xoom.pipes.Source;
import io.vlingo.xoom.pipes.http.handler.RawRequestHandler;
import io.vlingo.xoom.pipes.http.handler.ResponseSender;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import static io.vlingo.xoom.http.resource.ResourceBuilder.resource;

public class HttpStreamServer implements Closeable {
    private final Stage stage;
    private final List<RequestHandler> handlers;
    private Server server;

    public HttpStreamServer(Stage stage) {
        this.stage = stage;
        this.handlers = new ArrayList<>();
        this.server = null;
    }

    public Source<Request> requestSource(Method method, String path) {
        RawRequestHandler handler = new RawRequestHandler(method, path, stage);
        this.handlers.add(handler);
        return handler;
    }

    public Sink<Response> responseSink() {
        return new ResponseSender();
    }

    public void start(Stage stage, int port) {
        if (!handlers.isEmpty()) {
            Resource resource = resource("http-stream", 10, handlers.toArray(new RequestHandler[0]));
            this.server = Server.startWith(stage, Resources.are(resource), port, Configuration.Sizing.define(), Configuration.Timing.define());
        }
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop();
        }
    }
}
