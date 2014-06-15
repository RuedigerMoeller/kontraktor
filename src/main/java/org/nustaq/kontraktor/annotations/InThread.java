package org.nustaq.kontraktor.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})

/**
 * Created by moelrue on 07.05.2014.
 *
 * parameters tagged as callback get wrapped by 'Actors.Callback' automatically. Calls on these object are
 * executed in the callers thread (enqueued to the calling Actors queue)
 *
 */
public @interface InThread {
}
