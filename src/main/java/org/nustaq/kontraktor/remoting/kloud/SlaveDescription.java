package org.nustaq.kontraktor.remoting.kloud;

import java.io.Serializable;

/**
 * Created by moelrue on 3/6/15.
 */
public class SlaveDescription implements Serializable {

    String nodeId;

    public SlaveDescription(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public String toString() {
        return "SlaveDescription{" +
                "nodeId='" + nodeId + '\'' +
                '}';
    }
}
