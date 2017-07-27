package org.nustaq.kontraktor.babel;

import java.io.Serializable;

/**
 * Created by ruedi on 05.07.17.
 */
public class BabelResult implements Serializable {
    public String code;
    public String err;

    @Override
    public String toString() {
        return "BabelResult{" +
            "code='" + code + '\'' +
            ", err='" + err + '\'' +
            '}';
    }
}
