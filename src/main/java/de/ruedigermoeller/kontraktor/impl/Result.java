package de.ruedigermoeller.kontraktor.impl;

import de.ruedigermoeller.kontraktor.Callback;

/**
 * Created by ruedi on 21.05.14.
 */
public class Result<T> extends FutureImpl<T> {

    public Result(T result, Object error) {
        this.result = result;
        this.error = error;
    }

    public Result(T result) {
        this(result,null);
    }

    public Result() {
        super();
    }

    public T getResult() {
        return (T) result;
    }

    public Object getError() {
        return error;
    }
}
