/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.client.solrj.cloud.autoscaling;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;

import org.apache.solr.common.MapWriter;
import org.apache.solr.common.util.Utils;

public class Violation implements MapWriter {
  final String shard, coll, node;
  final Object actualVal;
  final Long delta;//how far is the actual value from the expected value
  final Object tagKey;
  private final int hash;
  private final Clause clause;


  Violation(Clause clause, String coll, String shard, String node, Object actualVal, Long delta, Object tagKey) {
    this.clause = clause;
    this.shard = shard;
    this.coll = coll;
    this.node = node;
    this.delta = delta;
    this.actualVal = actualVal;
    this.tagKey = tagKey;
    hash = ("" + coll + " " + shard + " " + node + " " + String.valueOf(tagKey) + " " + Utils.toJSONString(getClause().toMap(new HashMap<>()))).hashCode();
  }

  public Clause getClause() {
    return clause;
  }

  @Override
  public int hashCode() {
    return hash;
  }
  //if the delta is lower , this violation is less serious
  public boolean isLessSerious(Violation that) {
    return that.delta != null && delta != null &&
        Math.abs(delta) < Math.abs(that.delta);
  }

  @Override
  public boolean equals(Object that) {
    if (that instanceof Violation) {
      Violation v = (Violation) that;
      return Objects.equals(this.shard, v.shard) &&
          Objects.equals(this.coll, v.coll) &&
          Objects.equals(this.node, v.node) &&
          Objects.equals(this.tagKey, v.tagKey)
          ;
    }
    return false;
  }

  @Override
  public String toString() {
    return Utils.toJSONString(Utils.getDeepCopy(toMap(new LinkedHashMap<>()), 5));
  }

  @Override
  public void writeMap(EntryWriter ew) throws IOException {
    ew.putIfNotNull("collection", coll);
    ew.putIfNotNull("shard", shard);
    ew.putIfNotNull("node", node);
    ew.putIfNotNull("tagKey", String.valueOf(tagKey));
    ew.putIfNotNull("violation", (MapWriter) ew1 -> {
      if (getClause().isPerCollectiontag()) ew1.put("replica", actualVal);
      else ew1.put(clause.tag.name, String.valueOf(actualVal));
      ew1.putIfNotNull("delta", delta);
    });
    ew.put("clause", getClause());
  }
}
