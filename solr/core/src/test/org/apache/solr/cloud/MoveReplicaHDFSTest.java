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
package org.apache.solr.cloud;

import com.carrotsearch.randomizedtesting.ThreadFilter;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakFilters;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.solr.cloud.hdfs.HdfsTestUtil;
import org.apache.solr.common.cloud.ZkConfigManager;
import org.apache.solr.util.BadHdfsThreadsFilter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
@ThreadLeakFilters(defaultFilters = true, filters = {
    BadHdfsThreadsFilter.class, // hdfs currently leaks thread(s)
    MoveReplicaHDFSTest.ForkJoinThreadsFilter.class
})
public class MoveReplicaHDFSTest extends MoveReplicaTest {

  private static MiniDFSCluster dfsCluster;

  @BeforeClass
  public static void setupClass() throws Exception {
    System.setProperty("solr.hdfs.blockcache.enabled", "false");
    dfsCluster = HdfsTestUtil.setupClass(createTempDir().toFile().getAbsolutePath());

    ZkConfigManager configManager = new ZkConfigManager(zkClient());
    configManager.uploadConfigDir(configset("cloud-hdfs"), "conf1");

    System.setProperty("solr.hdfs.home", HdfsTestUtil.getDataDir(dfsCluster, "data"));
  }

  @AfterClass
  public static void teardownClass() throws Exception {
    cluster.shutdown(); // need to close before the MiniDFSCluster
    HdfsTestUtil.teardownClass(dfsCluster);
    dfsCluster = null;
  }

  @Test
  public void testNormalMove() throws Exception {
    inPlaceMove = false;
    test();
  }

  @Test
  @BadApple(bugUrl="https://issues.apache.org/jira/browse/SOLR-12028")
  public void testNormalFailedMove() throws Exception {
    inPlaceMove = false;
    testFailedMove();
  }

  public static class ForkJoinThreadsFilter implements ThreadFilter {

    @Override
    public boolean reject(Thread t) {
      String name = t.getName();
      if (name.startsWith("ForkJoinPool.commonPool")) {
        return true;
      }
      return false;
    }
  }

}
