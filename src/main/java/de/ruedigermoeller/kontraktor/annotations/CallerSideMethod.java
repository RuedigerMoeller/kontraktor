package de.ruedigermoeller.kontraktor.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})

/**
 * Created by ruedi on 06.05.14.
 *
 * Specifies this method is a utility processed on client side.
 * e.g.
 *
 * class .. extends Actor {
 *     public void message( long timeStamp, String stuff ) {..}
 *
 *     @CallerSideMethod public void message( String stuff ) {
 *         message( System.currentTimeMillis(), stuff );
 *     }
 * }
 *
 * Note those method cannot access local state of the actor, they just might call methods.
 */
public @interface CallerSideMethod {
}
