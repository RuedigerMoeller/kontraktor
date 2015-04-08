package myapp;

import org.nustaq.kontraktor.annotations.GenRemote;
import org.nustaq.serialization.annotations.Serialize;

import java.io.Serializable;
import java.util.*;

/**
 * Created by ruedi on 08/04/15.
 */
@GenRemote
public class SPAPojo implements Serializable {

    int num;
    double doub;
    String string;

    Map map;
    Set set;
    List list;

    public SPAPojo() {
    }

    public SPAPojo setVals() {
        num = 77;
        doub = 234.234;
        string = "one string %$&üü";
        map = new HashMap<>();
        map.put( "A", "B");
        map.put( 13, "13" );
        set = new HashSet<>();
        set.add("pokpok");
        set.add(456);
        set.add(234.234);

        list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add(234.234);
        list.add(19);
        return this;
    }

    public int getNum() {
        return num;
    }

    public double getDoub() {
        return doub;
    }

    public String getString() {
        return string;
    }

    public Map getMap() {
        return map;
    }

    public Set getSet() {
        return set;
    }

    public List getList() {
        return list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SPAPojo)) return false;

        SPAPojo spaPojo = (SPAPojo) o;

        if (Double.compare(spaPojo.doub, doub) != 0) return false;
        if (num != spaPojo.num) return false;
        if (list != null ? !list.equals(spaPojo.list) : spaPojo.list != null) return false;
        if (map != null ? !map.equals(spaPojo.map) : spaPojo.map != null) return false;
        if (set != null ? !set.equals(spaPojo.set) : spaPojo.set != null) return false;
        if (string != null ? !string.equals(spaPojo.string) : spaPojo.string != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = num;
        temp = Double.doubleToLongBits(doub);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (string != null ? string.hashCode() : 0);
        result = 31 * result + (map != null ? map.hashCode() : 0);
        result = 31 * result + (set != null ? set.hashCode() : 0);
        result = 31 * result + (list != null ? list.hashCode() : 0);
        return result;
    }
}
