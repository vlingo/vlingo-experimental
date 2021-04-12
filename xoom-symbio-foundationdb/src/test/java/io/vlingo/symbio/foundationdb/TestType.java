// Copyright © 2012-2020 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.symbio.foundationdb;

public class TestType {
  public final String id;
  public final String name;
  public final int age;

  public TestType(final String id, final String name, final int age) {
    this.id = id;
    this.name = name;
    this.age = age;
  }

  @Override
  public int hashCode() {
    return 31 * id.hashCode() * name.hashCode() * Integer.hashCode(age);
  }

  @Override
  public boolean equals(final Object other) {
    if (other == null || other.getClass() != getClass()) {
      return false;
    }
    final TestType otherTestType = (TestType) other;

    return this.id.equals(otherTestType.id);
  }

  @Override
  public String toString() {
    return "TestType[id=" + id + " name=" + name + " age=" + age + "]";
  }
}
