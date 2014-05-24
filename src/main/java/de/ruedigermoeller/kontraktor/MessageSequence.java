package de.ruedigermoeller.kontraktor;

import de.ruedigermoeller.kontraktor.Future;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruedi on 23.05.14.
 */
public class MessageSequence {
    List<Message> messages;

    public MessageSequence(Message msg, Object ... targets) {
        messages = new ArrayList<>();
        for (int i = 0; i < targets.length; i++) {
            Object target = targets[i];
            messages.add(msg.withTarget(target,true));
        }
    }

    public MessageSequence(List<Message> messages) {
        this.messages = messages;
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
        }
    }

    public IFuture<IFuture[]> yield() {
        IFuture[] res = new IFuture[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            if ( message.getMethod().getReturnType() == void.class ) {
                res[i] = new Future("void");
                message.send();
            } else {
                res[i] = message.send();
            }
        }
        return Actors.Yield(res);
    }

    public int size() {
        return messages.size();
    }

    public Message get( int index ) {
        return messages.get(index);
    }

    public Message first() {
        if ( size() == 0 )
            return null;
        return get(0);
    }

    public IFuture<IFuture[]> exec() {
        Future<IFuture[]> future = new Future<>();
        IFuture res[] = new IFuture[messages.size()];
        exec(res,0, future);
        return future;
    }

    private void exec(final IFuture res[], final int index, final Callback finished) {
        if ( index >= res.length ) {
            finished.receiveResult(res,null);
            return;
        }
        res[index] = messages.get(index).send();
        res[index].then(new Callback() {
            @Override
            public void receiveResult(Object result, Object error) {
                if ( error != null ) {
                    finished.receiveResult(res,error);
                    return;
                }
                exec(res, index + 1, finished);
            }
        });
    }

    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return "MessageSequence{" +
                "messages=" + messages +
                '}';
    }
}
