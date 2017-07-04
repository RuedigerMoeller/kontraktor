package org.nustaq.babelremote;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.annotations.Local;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;

/**
 * Created by ruedi on 04.07.17.
 */
public class StubGenerator {

    public void generate(Class c, PrintStream out) {
        String clnam = c.getSimpleName();
        out.println("class "+clnam+" {");
        Arrays.stream(c.getMethods()).forEach( method -> {
            if (
                method.getDeclaringClass() != Object.class &&
                method.getDeclaringClass() != Actor.class &&
                method.getDeclaringClass() != Actors.class &&
                !Modifier.isStatic(method.getModifiers()) &&
                method.getAnnotation(Local.class) == null
            )
            {
                out.println();
                out.print("  "+method.getName()+"(");
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    out.print( parameter.getType().getSimpleName().toLowerCase() + ((i!= parameters.length-1) ? ", " : "") );
                }
                out.println(") {");
                if ( method.getReturnType() != void.class )
                    out.println("    return new KPromise(null));" );
                out.println("  }");
            }
        });
        out.println();
        out.println("}");
    }

    public static void genStub(Class<? extends Actor> cl) {
        genStub(cl,System.out);
    }

    public static void genStub(Class<? extends Actor> cl, PrintStream out) {
        StubGenerator gen = new StubGenerator();
        gen.generate(cl, out);
    }

    public static void main(String[] args) throws ClassNotFoundException {
        genStub((Class<? extends Actor>) Class.forName(args[0]));
    }


}
