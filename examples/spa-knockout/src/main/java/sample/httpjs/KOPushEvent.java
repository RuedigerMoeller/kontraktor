package sample.httpjs;

import java.io.Serializable;

/**
 * Created by moelrue on 6/10/15.
 */
public class KOPushEvent implements Serializable {

    int numSessions;
    String msg;
    String msgFrom;

    public KOPushEvent numSessions(final int numSessions) {
        this.numSessions = numSessions;
        return this;
    }

    public KOPushEvent msg(final String msg) {
        this.msg = msg;
        return this;
    }

    public KOPushEvent msgFrom(final String msgFrom) {
        this.msgFrom = msgFrom;
        return this;
    }


}
