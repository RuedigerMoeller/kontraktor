package microservice.pub;

import java.io.Serializable;

public class Item implements Serializable {

    private String name;
    private String description;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Item name(String name) {
        this.name = name;
        return this;
    }

    public Item description(String description) {
        this.description = description;
        return this;
    }
}
