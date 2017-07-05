package org.nustaq.babelremote;

import java.io.Serializable;

/**
 * Created by ruedi on 05.07.17.
 */
public class BabelOpts implements Serializable {

    boolean debug = true;
    String[] presets = { "import-export", "react" };

    public BabelOpts debug(final boolean debug) {
        this.debug = debug;
        return this;
    }

    public BabelOpts presets(final String[] presets) {
        this.presets = presets;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    public String[] getPresets() {
        return presets;
    }
}
