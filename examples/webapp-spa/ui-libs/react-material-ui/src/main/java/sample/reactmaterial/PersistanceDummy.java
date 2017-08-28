package sample.reactmaterial;

import org.nustaq.offheap.FSTAsciiStringOffheapMap;
import org.nustaq.offheap.FSTUTFStringOffheapMap;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PersistanceDummy {

    // below a dummy implementation (save session data for resurrection in a mmapped file)
    protected FSTAsciiStringOffheapMap sessionDataCache;
    private int sessionDataIndex = 1;

    public PersistanceDummy() {
        try {
            sessionDataCache = new FSTAsciiStringOffheapMap(
                "./session.bin", 48, 1024L*1024*10, 100_000
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Object getSessionData(String sessionId) {
        Object[] dataAndTimeStamp = (Object[]) sessionDataCache.get(sessionId);
        if ( dataAndTimeStamp == null )
            return null;
        return dataAndTimeStamp[0];
    }

    // fade out old session data
    public void flipSessionCache() {
        // demo impl, need persistence to have clients survive a server restart
        // TODO: remove old sessions using timestamp
        // sessionDataCache.values().forEachRemaining(..);
    }

    public void putSessionData(String sessionid, Object data) {
        // implicitely moves to "new bucket"
        sessionDataCache.put(sessionid,new Object[] {data,System.currentTimeMillis()});
    }

}
