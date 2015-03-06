package org.nustaq.kontraktor.kollektiv;

import java.io.Serializable;

/**
 * Created by moelrue on 3/6/15.
 */
public class MemberDescription implements Serializable {

    String nodeId;

    public MemberDescription(String nodeId) {
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
