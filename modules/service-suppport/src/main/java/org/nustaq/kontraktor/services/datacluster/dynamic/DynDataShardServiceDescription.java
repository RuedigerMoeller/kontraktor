package org.nustaq.kontraktor.services.datacluster.dynamic;

import org.nustaq.kontraktor.remoting.base.ServiceDescription;

import java.util.BitSet;

public class DynDataShardServiceDescription extends ServiceDescription {

    private BitSet slots;

    public DynDataShardServiceDescription(String name) {
        super(name);
    }

    DynDataShardServiceDescription slots(BitSet slots) {
        this.slots = slots;
        return this;
    }

    public BitSet getSlots() {
        return slots;
    }
}
