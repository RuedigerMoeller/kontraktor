package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.RemoteCallEntry;
import org.nustaq.kson.Kson;
import org.nustaq.kson.KsonArgTypesResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ruedi on 14.08.2014.
 */
public class ArgTypesResolver implements KsonArgTypesResolver {

    HashMap<String, Class[]> methodMap;

    public ArgTypesResolver(Class c) {
        initMap(c);
    }

    public ArgTypesResolver(Method allowed[]) {
        initMap(allowed);
    }

    protected void initMap(Class c) {
        initMap(c.getMethods());
    }

    protected void initMap(Method[] allowed) {
        methodMap = new HashMap<>();
        for (int i = 0; i < allowed.length; i++) {
            Method method = allowed[i];
            final int mods = method.getModifiers();
            if ( !Modifier.isStatic(mods) && ! Modifier.isAbstract(mods) && Modifier.isPublic(mods)
               && method.getDeclaringClass() != Object.class
               && ! method.getName().equals("§stop")
//               && method.getDeclaringClass() != Actor.class want to call ping !
            )
            {
                methodMap.put(method.getName(), method.getParameterTypes());
            }
        }
    }

    @Override
    public Class[] getArgTypes(Class outerClazz, List currentParse) {
        if ( RemoteCallEntry.class == outerClazz ) {
            // expect methodname preceding arglist for now ..
            String methodName = (String) currentParse.get(currentParse.size() - 2);
            final Class[] classes = methodMap.get(methodName);
            if ( classes == null )
                throw new RuntimeException("no such method "+methodName);
            return classes;
        }
        throw new RuntimeException("unexpected class in getArgTypes");
    }

    public static class SampleTest {
        public void methodName( String a, int b, int c ) {}
    }

    public static void main(String arg[]) throws Exception {
        ArgTypesResolver res = new ArgTypesResolver(SampleTest.class);
        Kson kson = new Kson().map("call",RemoteCallEntry.class);
        final Object call = kson.readObject("{ receiverKey: 1 method: methodName args: [ hello 1 2 ] }", "call", res);
        System.out.println(call);
    }
}
