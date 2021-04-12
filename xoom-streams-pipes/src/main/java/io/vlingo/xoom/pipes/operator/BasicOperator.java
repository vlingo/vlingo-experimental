// Copyright © 2012-2019 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.pipes.operator;

import io.vlingo.xoom.actors.Stage;
import io.vlingo.xoom.pipes.Operator;
import io.vlingo.xoom.pipes.Stream;
import io.vlingo.xoom.pipes.actor.Materialized;
import io.vlingo.xoom.pipes.actor.MaterializedOperatorActor;
import io.vlingo.xoom.pipes.actor.MaterializedSource;

public abstract class BasicOperator<A, B> implements Operator<A, B> {
    @Override
    public Materialized materialize(Stage stage, MaterializedSource source) {
        return stage.actorFor(MaterializedSource.class, MaterializedOperatorActor.class, source, this, Stream.DEFAULT_POLL_INTERVAL);
    }
}
