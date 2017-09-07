package org.nustaq.kontraktor.routing;

import org.nustaq.kontraktor.routers.AbstractKrouter;
import org.nustaq.kontraktor.routers.HotColdFailoverKrouter;

public enum FailoverStrategy {

    HotHot {
        @Override
        public Class<? extends AbstractKrouter> getClazz() {
            return HotColdFailoverKrouter.class;
        }
    },
    HotCold {
        @Override
        public Class<? extends AbstractKrouter> getClazz() {
            return HotColdFailoverKrouter.class;
        }
    },
    RoundRobin {
        @Override
        public Class<? extends AbstractKrouter> getClazz() {
            return HotColdFailoverKrouter.class;
        }
    };


    public abstract Class<? extends AbstractKrouter> getClazz();

}
