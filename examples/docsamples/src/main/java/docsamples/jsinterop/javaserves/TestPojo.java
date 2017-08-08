package docsamples.jsinterop.javaserves;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestPojo implements Serializable {
    Map aMap = new HashMap();
    List<String> strings = new ArrayList<>();
    String someString;

    public TestPojo() {
        aMap.put("A",1);
        aMap.put("B",1);
        strings.add("1");
        strings.add("2");
        strings.add("3");strings.add("4");
    }

    public Map getMap() {
        return aMap;
    }

    public List<String> getStrings() {
        return strings;
    }

    public String getSomeString() {
        return someString;
    }
}