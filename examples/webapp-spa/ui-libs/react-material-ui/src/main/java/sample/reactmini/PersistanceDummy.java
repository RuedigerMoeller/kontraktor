package sample.reactmini;

import java.util.HashMap;
import java.util.Map;

public class PersistanceDummy {

    // below a dummy implementation (save session data for resurrection
    private Map<String,Object> sessionDataCache[] = new Map[]{new HashMap(), new HashMap<>()};
    private int sessionDataIndex = 1;

    public Object getSessionData(String sessionId) {
        String userName = (String) sessionDataCache[0].get(sessionId);
        if ( userName == null )
            userName = (String) sessionDataCache[1].get(sessionId);
        return userName;
    }

    // fade out inmemory data to avoid OOM
    public void flipSessionCache() {
        // demo impl, need persistence to have clients survive a server restart
        sessionDataIndex = (sessionDataIndex+1) % 2;
        sessionDataCache[sessionDataIndex] = new HashMap<>();
    }

    public void putSessionData(String sessionid, Object data) {
        // implicitely moves to "new bucket"
        sessionDataCache[0].put(sessionid,data);
        sessionDataCache[1].put(sessionid,data);
    }

}
