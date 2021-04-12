// Copyright © 2012-2019 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.pipes.actor;

import io.vlingo.xoom.common.Completes;
import io.vlingo.xoom.pipes.Record;

import java.util.Optional;

public interface MaterializedSource extends Materialized {
    Completes<Optional<Record[]>> nextIfAny();
}
