package microservice.pub;

import java.io.Serializable;

public class ChangeMessage implements Serializable {

    public enum Action {
        ADDED,
        REMOVED
    }

    Action action;
    Item item;

    public ChangeMessage action(Action action) {
        this.action = action;
        return this;
    }

    public ChangeMessage item(Item item) {
        this.item = item;
        return this;
    }

    public Action getAction() {
        return action;
    }

    public Item getItem() {
        return item;
    }

    @Override
    public String toString() {
        return "ChangeMessage{" +
            "action=" + action +
            ", item=" + item +
            '}';
    }
}
