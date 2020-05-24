package org.nustaq.reallive.api;

import org.nustaq.reallive.server.storage.HashIndex;
import org.nustaq.reallive.query.QToken;
import org.nustaq.reallive.query.Value;
import org.nustaq.reallive.query.VarPath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RLHashIndexPredicate implements RLPredicate<Record> {


    public static <T> RLHashIndexPredicate hashIndex(String path, Object key, RLPredicate<Record> subQuery ) {
        return new RLHashIndexPredicate(path,key,subQuery);
    }

    public static abstract class RLPath {

        public RLPath(String path,Object key) {
            this.path = path;
            this.key = HashIndex.unifyKey(key);
            varPath = new VarPath(path,null, new QToken(path,"",0 ));
        }

        String path;
        Object key;
        VarPath varPath;

        public boolean test(Record r) {
            return  key.equals(varPath.evaluate(r));
        }

        public Object getKey() {
            return key;
        }

        public String getPathString() {
            return path;
        }

        @Override
        public String toString() {
            return "RLPath{" +
                "path='" + path + '\'' +
                ", key=" + key +
                '}';
        }
    }

    public static class JoinPath extends RLPath implements Serializable {
        public JoinPath(String path, Object key) {
            super(path, key);
        }
    }
    public static class SubtractPath extends RLPath implements Serializable {
        public SubtractPath(String path, Object key) {
            super(path, key);
        }
    }
    public static class IntersectionPath extends RLPath implements Serializable {
        public IntersectionPath(String path, Object key) {
            super(path, key);
        }
    }

    public RLHashIndexPredicate(RLPredicate subQuery) {
        this.subQuery = subQuery;
    }

    public RLHashIndexPredicate(String path, Object key, RLPredicate subQuery) {
        join(path,key);
        this.subQuery = subQuery;
    }

    List<RLPath> pathes = new ArrayList<>();
    RLPredicate subQuery;

    public RLHashIndexPredicate join(String path, Object key) {
        pathes.add(new JoinPath(path,key));
        return this;
    }

    public RLHashIndexPredicate subtract(String path, Object key) {
        pathes.add(new SubtractPath(path,key));
        return this;
    }

    public RLHashIndexPredicate intersect(String path, Object key) {
        pathes.add(new IntersectionPath(path, key));
        return this;
    }

    public RLHashIndexPredicate subQuery( RLPredicate sq ) {
        subQuery = sq;
        return this;
    }

    @Override
    public boolean test(Record t) {
        boolean res = false;
        for (int i = 0; i < pathes.size(); i++) {
            RLPath rlPath = pathes.get(i);
            Value evaluate = rlPath.varPath.evaluate(t);
            if ( evaluate == null )
                return false;
            Object value = HashIndex.unifyKey(evaluate.getValue());
            if ( rlPath instanceof JoinPath ) {
                if ( rlPath.getKey().equals( value ) )
                    res = true;
            } else if ( rlPath instanceof SubtractPath ) {
                if ( rlPath.getKey().equals( value ) )
                    res = false;
            } else if ( rlPath instanceof IntersectionPath && res ) {
                if ( !rlPath.getKey().equals( value ) )
                    res = false;
            }
        }
        return res && subQuery.test(t);
    }

    public List<RLPath> getPath() {
        return pathes;
    }

    public RLPath getPath(int i) {
        return pathes.get(i);
    }

    @Override
    public String toString() {
        return "RLHashIndexPredicate{" +
            "pathes=" + pathes +
            ", subQuery=" + subQuery +
            '}';
    }
}
