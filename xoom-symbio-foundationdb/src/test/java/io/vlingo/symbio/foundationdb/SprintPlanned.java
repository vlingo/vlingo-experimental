// Copyright © 2012-2020 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

import io.vlingo.symbio.Source;

public class SprintPlanned extends Source<byte[]> {
  public final String sprintId;

  public SprintPlanned(final String sprintId) {
    this.sprintId = sprintId;
  }
}
