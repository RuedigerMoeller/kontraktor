package org.nustaq.kontraktor.webapp;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

public class KJson {
    public static String objs(Object ... keyval) {
        return obj(keyval).toString();
    }

    public static JsonObject obj(Object ... keyval) {
        JsonObject res = new JsonObject();
        for (int i = 0; i < keyval.length; i+=2) {
            String k = (String) keyval[i];
            Object v = keyval[i+1];
            if ( v instanceof Double ) {
                res.set(k,((Double) v).doubleValue());
            } else
            if ( v instanceof Float ) {
                res.set(k,((Float) v).doubleValue());
            } else
            if ( v instanceof Number ) {
                res.set(k,((Number) v).longValue());
            } else
            if ( v instanceof String ) {
                res.set(k,(String) v);
            } else
            if ( v instanceof Boolean ) {
                res.set(k,((Boolean)v).booleanValue());
            } else
            if ( v == null ) {
                res.set(k,((Double) v).doubleValue());
            } else
            if ( v instanceof JsonObject ) {
                res.set(k, (JsonObject)v);
            } else
            if ( v instanceof JsonArray ) {
                res.set(k, (JsonArray)v);
            } else
            if ( v instanceof Object[] ) {
                res.set(k, obj(v));
            } else
                throw new RuntimeException("unexpected json value "+v);
        }
        return res;
    }

    public static JsonArray arr(Object ... vals) {
        JsonArray res = new JsonArray();
        for (int i = 0; i < vals.length; i++) {
            Object v = vals[i];
            if ( v instanceof Double ) {
                res.add(((Double) v).doubleValue());
            } else
            if ( v instanceof Float ) {
                res.add(((Float) v).doubleValue());
            } else
            if ( v instanceof Number ) {
                res.add(((Number) v).longValue());
            } else
            if ( v instanceof String ) {
                res.add((String) v);
            } else
            if ( v instanceof Boolean ) {
                res.add(((Boolean)v).booleanValue());
            } else
            if ( v == null ) {
                res.add(((Double) v).doubleValue());
            } else
            if ( v instanceof JsonArray ) {
                res.add((JsonArray)v);
            } else
            if ( v instanceof JsonObject ) {
                res.add((JsonObject)v);
            } else
            if ( v instanceof Object[] ) {
                res.add(obj(v));
            } else
                throw new RuntimeException("unexpected json value "+v);
        }
        return res;
    }

    public static void main(String[] args) {
        JsonObject jo = obj(
            "test", 13,
            "name", "Rüdiger Moellerius",
            "subobj", obj(
                "test", arr(1, 2, 3, 4, 5, 6, 7.23)
            ),
            "bool", true
        );

        System.out.println(jo.toString(WriterConfig.PRETTY_PRINT));
    }
}
