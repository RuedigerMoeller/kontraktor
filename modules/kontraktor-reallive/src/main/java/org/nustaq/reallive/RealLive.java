package org.nustaq.reallive;

import org.nustaq.reallive.sys.annotations.ColOrder;
import org.nustaq.reallive.sys.annotations.Description;
import org.nustaq.reallive.sys.annotations.DisplayName;
import org.nustaq.reallive.sys.annotations.Hidden;
import org.nustaq.reallive.sys.metadata.ColumnMeta;
import org.nustaq.reallive.sys.metadata.Metadata;
import org.nustaq.reallive.sys.metadata.TableMeta;
import org.nustaq.reallive.sys.tables.SysTable;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ruedi on 07/06/15.
 */
public class RealLive {

    public static int CHANGE_Q_SIZE = 50000;
    public static int FILTER_Q_SIZE = 100000;

    protected String dataDirectory;
    protected FSTConfiguration conf;

    ConcurrentHashMap<String, RLTable> tables = new ConcurrentHashMap<>();
    Metadata model;

    public RealLive() {
        model = new Metadata();
    }

    public RealLive(String dataDir) {
        model = new Metadata();
        dataDirectory = dataDir;
    }

    public void init() {
        initSystemTables();
    }


    protected void initSystemTables() {
        Arrays.stream(new Class[]{SysTable.class}).forEach(
            (clz) -> createTable(clz.getSimpleName(), clz)
        );
    }

    public RealLive createTable(String name, Class<? extends Record> clazz) {
        if (tables.get(name) != null ) {
            throw new RuntimeException("table already created");
        }
        //if ( clazz.getAnnotation(Virtual.class) == null )
        {
            pureCreateTable(name, clazz);
            addToSysTable(name, clazz);
            return this;
        }
//        addToSysTable(name, clazz);
//        return this;
    }

    public RealLive createTable(Class<? extends Record> recordClass) {
        createTable(recordClass.getSimpleName(), recordClass);
        return this;
    }

//    @Override
//    public void createVirtualStream(String name, ReplicatedSet set) {
//        throw new RuntimeException("not supported in core implementation");
//    }

    public void shutDown() {
        tables.values().forEach( rlTab -> {
            rlTab.shutDown();
        });
    }

    // specialized to also include transient fields for virtual recs
    final List<Field> getTransientFields(Class c, List<Field> res) {
        if ( c == Record.class )
            return res;
        if (res == null) {
            res = new ArrayList<Field>();
        }
        if (c == null) {
            return res;
        }
        List<Field> c1 = Arrays.asList(c.getDeclaredFields());
        Collections.reverse(c1);
        for (int i = 0; i < c1.size(); i++) {
            Field field = c1.get(i);
            if ( Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()) ) {
                res.add(field);
            }
        }
        return getTransientFields(c.getSuperclass(), res);
    }

    private void addToSysTable(String name, Class rowClass) {
        TableMeta tableMeta = new TableMeta();
        tableMeta.setName(name);
        final FSTClazzInfo classInfo = conf.getClassInfo(rowClass);

        Description desc = (Description) classInfo.getClazz().getAnnotation(Description.class);
        if ( desc != null ) {
            tableMeta.setDescription(desc.value());
        }

        DisplayName ds = (DisplayName) classInfo.getClazz().getAnnotation(DisplayName.class);
        if ( ds != null ) {
            tableMeta.setDisplayName(ds.value());
        } else {
            tableMeta.setDisplayName(tableMeta.getName());
        }

        final FSTClazzInfo.FSTFieldInfo[] fieldInfo = classInfo.getFieldInfo();
        for (int i = 0; i < fieldInfo.length; i++) {
            FSTClazzInfo.FSTFieldInfo fi = fieldInfo[i];
            ColumnMeta cm = new ColumnMeta();
            Field field = fi.getField();
            processFieldAnnotations(i, cm, field);
            tableMeta.putColumn(cm.getName(),cm);
        }
        List<Field> transientFields = getTransientFields(classInfo.getClazz(), null);
        int fieldOffset = fieldInfo.length;
        for (int i = 0; i < transientFields.size(); i++) {
            Field field = transientFields.get(i);
            ColumnMeta cm = new ColumnMeta();
            processFieldAnnotations(i+fieldOffset, cm, field);
            tableMeta.putColumn(cm.getName(),cm);
        }

        model.putTable(name,tableMeta);

        RLTable<SysTable> sysTables = getTable("SysTable");
        SysTable sysTab = sysTables.createForAdd();
        sysTab._setRecordKey(name);
        sysTab.setTableName(name);
        sysTab.setDescription(model.getTable(name).getDescription());
        sysTab.setMeta(tableMeta);
        sysTab.computeUpdateBcast(true,0);
    }

    private void processFieldAnnotations(int i, ColumnMeta cm, Field field) {
        Description desc;
        DisplayName ds;
        cm.setName(field.getName());
        cm.setFieldId(i);

        desc = field.getAnnotation(Description.class);
        if ( desc != null ) {
            cm.setDescription(desc.value());
        }

        ds = field.getAnnotation(DisplayName.class);
        if ( ds != null ) {
            cm.setDisplayName(ds.value());
        } else {
            cm.setDisplayName(decamel(cm.getName()));
        }

        ColOrder ord = field.getAnnotation(ColOrder.class);
        if ( ord != null ) {
            cm.setOrder(ord.value());
        } else {
            cm.setOrder(cm.getName().hashCode()&0xff+0xffff00);
        }

        Hidden hid = field.getAnnotation(Hidden.class);
        if ( hid != null ) {
            cm.setHidden(true);
        } else {
            cm.setHidden(false);
        }

        cm.setJavaType(field.getType().getSimpleName());
    }

    private String decamel(String name) {
        String res = "";
        char prevChar = ' ';
        for ( int i = 0; i < name.length(); i++ ) {
            char c = name.charAt(i);
            if ( i == 0 ) {
                prevChar = Character.toUpperCase(c);
                res += prevChar;
                continue;
            }
            if ( Character.isUpperCase(c) && (Character.isLowerCase(prevChar) || !Character.isLetter(prevChar)) ) {
                res += ' ';
                prevChar = c;
                res += c;
            } else {
                prevChar = c;
                res += c;
            }
        }
        return res;
    }

    protected void pureCreateTable(String name, Class<? extends Record> clazz) {
        RLTable table = new RLTable();
        table.init(name, getDataDirectory(), clazz,getConf());
        tables.put( name, table );
    }

    public RLTable getTable(String tableId) {
        return tables.get(tableId);
    }

    public Metadata getMetadata() {
        return model;
    }


    public String getDataDirectory() {
        return dataDirectory;
    }

    public FSTConfiguration getConf() {
        return conf;
    }
}
