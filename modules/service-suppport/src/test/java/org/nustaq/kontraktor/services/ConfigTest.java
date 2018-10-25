package org.nustaq.kontraktor.services;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ConfigTest {

    @Test
    public void readFromClasspath() throws Exception {
        Config config = new Config(ClusterCfg.filename, true, ClusterCfg.configClasses);
        ClusterCfg clusterCfg = config.fromClasspath();

        assertEquals("http://test:8888", clusterCfg.publicHostUrl);
        assertEquals("data/datadir2", clusterCfg.getDataCluster().getDataDir()[1]);
        assertEquals("table1", clusterCfg.getDataCluster().getSchema()[0].getName());
    }

    @Test
    public void readFromFilesystem() throws Exception {
        Config config = new Config(ClusterCfg.filename, true, ClusterCfg.configClasses);
        ClusterCfg clusterCfg = config.fromFilesystem("src/test/resource/config");

        assertEquals("http://test:8888", clusterCfg.publicHostUrl);
        assertEquals("data/datadir2", clusterCfg.getDataCluster().getDataDir()[1]);
        assertEquals("table1", clusterCfg.getDataCluster().getSchema()[0].getName());
    }

    @Test
    public void substitute() {
        Map<String, String> substitutions = Collections.singletonMap("TEST", "test");
        String toSubstitute = "${TEST}";

        String substituted = Config.substitute(toSubstitute, substitutions);

        assertEquals("test", substituted);
    }

    @Test
    public void substituteDefaultValue() {
        Map<String, String> substitutions = Collections.emptyMap();
        String toSubstitute = "${TEST:test}";

        String substituted = Config.substitute(toSubstitute, substitutions);

        assertEquals("test", substituted);
    }

    @Test
    public void substituteDefaultValueGetsOverwritten() {
        Map<String, String> substitutions = Collections.singletonMap("TEST", "test-overwritten");
        String toSubstitute = "${TEST:test}";

        String substituted = Config.substitute(toSubstitute, substitutions);

        assertEquals("test-overwritten", substituted);
    }

    @Test
    public void twoSubstitutionsDirectlyAfterEachOther() {
        Map<String, String> substitutions = Collections.singletonMap("TEST", "test");
        String toSubstitute = "${TEST}${TEST}";

        String substituted = Config.substitute(toSubstitute, substitutions);

        assertEquals("testtest", substituted);
    }
}
