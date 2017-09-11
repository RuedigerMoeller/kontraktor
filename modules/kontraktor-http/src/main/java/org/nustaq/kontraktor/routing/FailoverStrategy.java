package org.nustaq.kontraktor.routing;

import org.nustaq.kontraktor.routers.*;

public enum FailoverStrategy {

    HotHot {
        @Override
        public Class<? extends AbstractKrouter> getClazz() {
            return HotHotFailoverKrouter.class;
        }
    },
    HotCold {
        @Override
        public Class<? extends AbstractKrouter> getClazz() {
            return HotColdFailoverKrouter.class;
        }
    },
    Simple {
        @Override
        public Class<? extends AbstractKrouter> getClazz() {
            return SimpleKrouter.class;
        }
    },
    RoundRobin {
        @Override
        public Class<? extends AbstractKrouter> getClazz() {
            return RoundRobinKrouter.class;
        }
    };

    public abstract Class<? extends AbstractKrouter> getClazz();

}
