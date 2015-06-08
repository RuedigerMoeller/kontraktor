package org.nustaq.reallive;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.serialization.FSTClazzInfo;

import java.util.function.Predicate;

/**
 * Created by moelrue on 6/8/15.
 */
public interface RecordContext {

    FSTClazzInfo.FSTFieldInfo[] getFieldInfo();

    FSTClazzInfo.FSTFieldInfo getFieldInfo(String fieldName);

    String getTableId();
}
