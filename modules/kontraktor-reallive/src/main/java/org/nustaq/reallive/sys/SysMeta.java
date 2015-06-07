package org.nustaq.reallive.sys;

import org.nustaq.kontraktor.annotations.GenRemote;
import org.nustaq.reallive.ChangeBroadcast;
import org.nustaq.reallive.RecordChange;
import org.nustaq.reallive.sys.messages.*;
import org.nustaq.reallive.sys.metadata.ColumnMeta;
import org.nustaq.reallive.sys.metadata.Metadata;
import org.nustaq.reallive.sys.metadata.TableMeta;
import org.nustaq.reallive.sys.tables.SysTable;
import org.nustaq.serialization.minbin.GenMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ruedi on 07.07.14.
 */
@GenRemote
public class SysMeta implements GenMeta {
    @Override
    public List<Class> getClasses() {
        return new ArrayList<Class>( Arrays.asList(new Class[] {
            SysTable.class,
            ChangeBroadcast.class,
            Metadata.class,
            TableMeta.class,
            ColumnMeta.class,
            QueryTuple.class,
            RecordChange.class
        }));
    }
}
