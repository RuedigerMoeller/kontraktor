package org.nustaq.kontraktor.remoting.encoding;

/**
 * Created by ruedi on 26.10.14.
 *
 * currently bound to fst serialization, could be abstracted with a simple interface (configure, asObject, asByteArray)
 *
 */
public enum SerializerType {
    FSTSer,
    MinBin,
    Json,
    UnsafeBinary
}
