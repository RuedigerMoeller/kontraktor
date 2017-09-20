package org.nustaq.kontraktor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class KFlow<T> {
    KFlow next;
    Callback<T> cb;

    public KFlow(Callback originalcb) {
        this.cb = originalcb;
    }

    public KFlow() {
        cb = (r,e) -> {
            consume(r,e);
        };
    }

    public void consume(Object item, Object err ) {
        if ( next != null ) {
            next.consume(item,err);
        }
    }

    public KFlow<T> each(Consumer<T> fun) {
        next = new KFlow(cb) {
            @Override
            public void consume(Object item, Object err) {
                if ( Actors.CONT.equals(err) )
                    fun.accept((T)item);
                super.consume((T) item,err);
            }
        };
        return next;
    }

    public KFlow<T> pairs(BiConsumer<T,T> fun) {
        next = new KFlow(cb) {
            T prev;
            @Override
            public void consume(Object item, Object err) {
                if ( Actors.CONT.equals(err) ) {
                    fun.accept(prev, (T) item);
                    prev = (T) item;
                }
                super.consume((T) item,err);
            }
        };
        return next;
    }

    public KFlow<T> onComplete(Consumer errfun) {
        next = new KFlow(cb) {
            @Override
            public void consume(Object item, Object err) {
                if ( !Actors.CONT.equals(err) )
                    errfun.accept(err);
                super.consume((T) item,err);
            }
        };
        return next;
    }

    public KFlow<T> filter(Function<T,Boolean> fun) {
        next = new KFlow(cb) {
            @Override
            public void consume(Object item, Object err) {
                if ( Actors.CONT.equals(err) ) {
                    Boolean apply = fun.apply((T) item);
                    if (apply)
                        super.consume((T) item,err);
                } else {
                    super.consume(item,err);
                }
            }
        };
        return next;
    }

    public <K> KFlow<K> map(Function<T,K> fun) {
        next = new KFlow(cb) {
            @Override
            public void consume(Object item, Object err) {
                if ( Actors.CONT.equals(err) ) {
                    K apply = fun.apply((T) item);
                    super.consume(apply, err);
                } else {
                    super.consume(item, err);
                }
            }
        };
        return next;
    }

    public KFlow<Collection<T>> collect(Collection<T> c) {
        next = new KFlow(cb) {
            @Override
            public void consume(Object item, Object err) {
                if ( Actors.CONT.equals(err) ) {
                    c.add((T) item);
                } else {
                    super.consume(c, Actors.CONT);
                    super.consume(null, null);
                }
            }
        };
        return next;
    }

    public KFlow<List<T>> list() {
        return (KFlow)collect(new ArrayList<>());
    }

    public Callback cb() {
        return cb;
    }

    public Callback callback() {
        return cb;
    }

    public static void main(String[] args) {
        String arr[] = {"a","b","c","d","e"};
        KFlow<String> ks =
            new KFlow()
                .filter( x -> x.equals("b") )
                .each(x -> System.out.println(x));

        for (int i = 0; i < arr.length; i++) {
            String s = arr[i];
            ks.cb().pipe(s);
        }
    }
}
