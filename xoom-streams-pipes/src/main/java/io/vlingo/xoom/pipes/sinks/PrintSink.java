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

import java.io.PrintStream;

public class PrintSink<T> implements Sink<T> {
    private final PrintStream printStream;
    private final String prefix;

    public PrintSink(PrintStream printStream, String prefix) {
        this.printStream = printStream;
        this.prefix = prefix;
    }

    public static <T> PrintSink<T> stdout(String prefix) {
        return new PrintSink<>(System.out, prefix);
    }

    @Override
    public void whenValue(Record<T> value) {
        printStream.println(prefix + value.toString());
    }

    @Override
    public Materialized materialize(Stage stage, MaterializedSource source) {
        return stage.actorFor(Materialized.class, MaterializedSinkActor.class, source, this, Stream.DEFAULT_POLL_INTERVAL);
    }
}
