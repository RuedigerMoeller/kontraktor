package org.nustaq.reallive.api;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Created by ruedi on 14/08/15.
 *
 * make JDK's predicate serializable
 *
 */
public interface RLPredicate<T> extends Predicate<T>, Serializable {

    default RLPredicate<T> and(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) && other.test(t);
    }

    default RLPredicate<T> negate() {
        return (t) -> !test(t);
    }
    default RLPredicate<T> or(Predicate<? super T> other) {
        Objects.requireNonNull(other);
        return (t) -> test(t) || other.test(t);
    }
    static <T> RLPredicate<T> isEqual(Object targetRef) {
        return (null == targetRef)
                ? Objects::isNull
                : object -> targetRef.equals(object);
    }

}
