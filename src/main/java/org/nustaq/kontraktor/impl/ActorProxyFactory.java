/*
Kontraktor Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
*/

package org.nustaq.kontraktor.impl;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.AsCallback;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import javassist.*;
import javassist.bytecode.AccessFlag;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.util.FSTUtil;

import java.io.Externalizable;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Copyright (c) 2012, Ruediger Moeller. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Date: 03.01.14
 * Time: 18:46
 */

/**
 * generate an actor proxy. This class is in awful state as it has been moved from fast-cast + extended iterative.
 * Out of fear of breaking stuff, a major refactoring is postponed.
 */
public class ActorProxyFactory {

    HashMap<Class,Class> generatedProxyClasses = new HashMap<Class, Class>();

    public ActorProxyFactory() {
    }

    public <T> T instantiateProxy(Actor target) {
        try {
            Class<? extends Actor> targetClass = target.getClass();
            if ( ! Modifier.isPublic(targetClass.getModifiers()) ) {
                throw new RuntimeException("Actor class must be public:" + targetClass.getName() );
            }
            if ( targetClass.isAnonymousClass() ) {
                throw new RuntimeException("Anonymous classes can't be Actors:" + targetClass.getName() );
            }
            if ( targetClass.isMemberClass() && !Modifier.isStatic(targetClass.getModifiers()) ) {
                throw new RuntimeException("Only STATIC inner classes can be Actors:" + targetClass.getName() );
            }
            Class proxyClass = createProxyClass(targetClass, targetClass.getClassLoader() );
            Constructor[] constructors = proxyClass.getConstructors();
            T instance = null;
            try {
                if ( FSTUtil.unFlaggedUnsafe != null ) {
                    // avoid running instance-initialiezr on actor proxy. Currently only
                    // unsafe allows to do that, though its completely safe to do so
                    instance = (T) FSTUtil.unFlaggedUnsafe.allocateInstance(proxyClass);
                }
                else
                    instance = (T) proxyClass.newInstance();
            } catch (Exception e) {
                for (int i = 0; i < constructors.length; i++) {
                    Constructor constructor = constructors[i];
                    if ( constructor.getParameterTypes().length == 0) {
                        constructor.setAccessible(true);
                        instance = (T) constructor.newInstance();
                        break;
                    }
                    if ( constructor.getParameterTypes().length == 1) {
                        instance = (T) constructor.newInstance((Class)null);
                        break;
                    }
                }
                if ( instance == null )
                    throw e;
            }
            Field f = instance.getClass().getField("__target");
            f.setAccessible(true);
            f.set(instance, target);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> Class<T> createProxyClass(Class<T> clazz, ClassLoader loader) throws Exception {
        synchronized (generatedProxyClasses) {
            String proxyName = clazz.getName() + "_ActorProxy";
            Class ccClz = generatedProxyClasses.get(clazz);
            if (ccClz == null) {
                ClassPool pool = ClassPool.getDefault();
                if ( loader instanceof ClassPathProvider ) {
                    ClassPool local = new ClassPool(pool);
                    List<File> classPath = ((ClassPathProvider) loader).getClassPath();
                    for (int i = 0; i < classPath.size(); i++) {
                        File file = classPath.get(i);
                        local.appendClassPath(file.getAbsolutePath());
                    }
                    pool = local;
                }
                CtClass cc = null;
                try {
                    cc = pool.getCtClass(proxyName);
                } catch (NotFoundException ex) {
                    //ignore
                }
                if (cc == null) {
                    cc = pool.makeClass(proxyName);
                    CtClass orig;
                    try {
                        orig = pool.get(clazz.getName());
                    } catch (NotFoundException ex) {
                        //insert for next proxy
                        pool.insertClassPath(new ClassClassPath(clazz));
                        orig = pool.get(clazz.getName());
                        if (orig == null)
                        {
                            throw ex;
                        }
                    }
                    cc.setSuperclass(orig);
                    cc.setInterfaces(new CtClass[]{pool.get(Externalizable.class.getName()), pool.get(ActorProxy.class.getName())});

                    defineProxyFields(pool, cc);
                    defineProxyMethods(cc, orig);
                }

                ccClz = loadProxyClass(clazz, pool, cc);
                generatedProxyClasses.put(clazz, ccClz);
            }
            return ccClz;
        }
    }

    protected <T> Class loadProxyClass(Class clazz, ClassPool pool, final CtClass cc) throws ClassNotFoundException {
        Class ccClz;
        Loader cl = new Loader(clazz.getClassLoader(), pool) {
            protected Class loadClassByDelegation(String name)
                    throws ClassNotFoundException
            {
                if ( name.equals(cc.getName()) )
                    return null;
                return delegateToParent(name);
            }
        };
        ccClz = cl.loadClass(cc.getName());
        return ccClz;
    }

    protected void defineProxyFields(ClassPool pool, CtClass cc) throws CannotCompileException, NotFoundException {
        CtField target = new CtField(pool.get(cc.getSuperclass().getName()), "__target", cc);
        target.setModifiers(AccessFlag.PUBLIC);
        cc.addField(target);
    }

    //FIXME: needs cleanup ...
    protected void defineProxyMethods(CtClass cc, CtClass orig) throws Exception {
//        cc.addMethod( CtMethod.make( "public void __setDispatcher( "+ DispatcherThread.class.getName()+" d ) { __target.__dispatcher(d); }", cc ) );
        CtMethod[] methods = getSortedPublicCtMethods(orig,false);

        for (int i = 0; i < methods.length; i++) {
            CtMethod method = methods[i];
            CtMethod originalMethod = method;
            if ( method.getName().equals("refDisconnected")) {
                int xx = 1;
            }
            if (method.getName().equals("getActor")) {
                ClassMap map = new ClassMap();
                map.put(Actor.class.getName(),Actor.class.getName());
                method = CtMethod.make( "public "+Actor.class.getName()+" getActor() { return __target; }", cc ) ;
            } else {
                ClassMap map = new ClassMap();
                map.fix(orig);
                map.fix(Actor.class.getName());
                method = new CtMethod(method, cc, map);
            }

            CtClass returnType = method.getReturnType();
            boolean isCallerSide = // don't touch
                    originalMethod.getAnnotation(CallerSideMethod.class) != null ||
                    (originalMethod.getName().equals("self"));// || originalMethod.getName().equals("future")); ??


            if ( isCallerSide ) {
                // verify callerside and inthread are not used
                Object[][] availableParameterAnnotations = originalMethod.getAvailableParameterAnnotations();
                CtClass[] parameterTypes = originalMethod.getParameterTypes();
                for (int j = 0; j < availableParameterAnnotations.length; j++) {
                    Object[] availableParameterAnnotation = availableParameterAnnotations[j];
                    if ( availableParameterAnnotation.length > 0 ) {
                        for (int k = 0; k < availableParameterAnnotation.length; k++) {
                            Object annot = availableParameterAnnotation[k];
                            if ( annot.toString().indexOf("kontraktor.annotations.InThread") > 0 ) {
                                throw new RuntimeException("cannot combine @CallerSide and @InThread, manually wrap callback using inThread(). method:"+originalMethod+" clz:"+orig);
                            }
                        }
                    }
                }
            }

            boolean allowed = ((method.getModifiers() & AccessFlag.ABSTRACT) == 0 ) &&
                    (method.getModifiers() & (AccessFlag.NATIVE|AccessFlag.FINAL|AccessFlag.STATIC)) == 0 &&
                    (method.getModifiers() & AccessFlag.PUBLIC) != 0 &&
                    !isCallerSide;
            // by default lock all method of object and actor
            allowed &= !originalMethod.getDeclaringClass().getName().equals(Object.class.getName()) &&
                       !originalMethod.getDeclaringClass().getName().equals(Actor.class.getName());

            // exceptions: async built-in actor methods that can be called
            if ( //originalMethod.getName().equals("executeInActorThread") || // needed again ! see spore
                // async methods at actor class. FIXME: add annotation
                 originalMethod.getName().equals("getSubMonitorables") ||
                 originalMethod.getName().equals("getReport") ||
                 originalMethod.getName().equals("ask") ||
                 originalMethod.getName().equals("tell") ||
                 originalMethod.getName().equals("__unpublish") ||
                 originalMethod.getName().equals("__republished") ||
                 originalMethod.getName().equals("ping") ||
                 originalMethod.getName().equals("__submit") ||
                 originalMethod.getName().equals("exec") ||
                 originalMethod.getName().equals("asyncstop") ||
                 originalMethod.getName().equals("receive") ||
                 originalMethod.getName().equals("complete") ||
                 originalMethod.getName().equals("close")
            )
            {
                allowed = true;
            }

            if (allowed) {
                boolean isVoid = returnType == CtPrimitiveType.voidType;
                boolean isCallbackCall = originalMethod.getAnnotation(AsCallback.class) != null;
                if (returnType != CtPrimitiveType.voidType && !returnType.getName().equals(IPromise.class.getName()) ) {
                    throw new RuntimeException("only void methods or methods returning IPromise allowed problematic method:"+originalMethod );
                }
                String conversion = "";
                Object[][] availableParameterAnnotations = originalMethod.getAvailableParameterAnnotations();
                CtClass[] parameterTypes = originalMethod.getParameterTypes();
                for (int j = 0; j < availableParameterAnnotations.length; j++) {
                    Object[] availableParameterAnnotation = availableParameterAnnotations[j];
                    if ( availableParameterAnnotation.length > 0 ) {
                        for (int k = 0; k < availableParameterAnnotation.length; k++) {
                            Object annot = availableParameterAnnotation[k];
                            if ( annot.toString().indexOf("kontraktor.annotations.InThread") > 0 ) {
                                if ( parameterTypes[j].getName().equals(Callback.class.getName()) ) {
                                    Log.Info(this,"InThread unnecessary when using built in Callback class. method:"+originalMethod+" clz:"+orig);
                                    continue;
                                }
                                if ( ! parameterTypes[j].isInterface() )
                                    throw new RuntimeException("@InThread can be used on interfaces only");
                                String an = Actor.class.getName();
                                conversion += an+" sender=("+an+")sender.get();";
                                conversion += "if ( sender != null ) { args[" + j + "] = sender.__scheduler.inThread(sender.__self, args[" + j + "]); }";
                                break;
                            }
                        }
                    }
                }
                String call = "__target.__enqueueCall( this, \""+method.getName()+"\", args, "+isCallbackCall+" );";
                if ( ! isVoid ) {
                    call = "return ("+originalMethod.getReturnType().getName()+") (Object)"+call;
                }
                String body = "{ Object args[] = $args;" +
                        conversion +
                        call+
                    "}";
                method.setBody(body);
                cc.addMethod(method);
            } else if ( (method.getModifiers() & (AccessFlag.NATIVE|AccessFlag.FINAL|AccessFlag.STATIC)) == 0 )
            {
                if (isCallerSide || method.getName().equals("toString")) {
                } else if (
                    ! method.getName().equals("getActor") &&
                    ! method.getName().equals("delayed") &&
//                    ! method.getName().equals("run") &&
                    ! method.getName().equals("exec")
                )
                {
                    method.setBody("throw new RuntimeException(\"can only call public methods on actor ref. method:'"+method.getName()+"\");");
                    cc.addMethod(method);
                } else {
                    cc.addMethod(method);
                }
            }
        }

//        methods = getSortedNonPublicCtMethods(orig);
//
//        for (int i = 0; i < methods.length; i++) {
//            CtMethod method = methods[i];
//            CtMethod originalMethod = method;
//            if (method.getName().equals("getActor")) {
//                ClassMap then = new ClassMap();
//                then.put(Actor.class.getName(),Actor.class.getName());
//                method = CtMethod.make( "public "+Actor.class.getName()+" getActor() { return __target; }", cc ) ;
//            } else {
//                ClassMap then = new ClassMap();
//                then.fix(orig);
//                method = new CtMethod(method, cc, then);
//            }
//
//            CtClass returnType = method.getReturnType();
//            // by default ignore all method of object and actor
//            boolean isObjectOrActor = !originalMethod.getDeclaringClass().getName().equals(Object.class.getName()) &&
//                    !originalMethod.getDeclaringClass().getName().equals(Actor.class.getName());
//
//            if (! isObjectOrActor &&
//                (method.getModifiers() & (AccessFlag.NATIVE|AccessFlag.FINAL|AccessFlag.STATIC)) == 0 )
//            {
//                method.setBody("throw new RuntimeException(\"can only call public methods on actor ref\");");
//                cc.addMethod(method);
//            }
//        }
    }

//    protected boolean isFastCall(CtMethod m) throws NotFoundException {
//        CtClass[] parameterTypes = m.getParameterTypes();
//        if (parameterTypes==null|| parameterTypes.length==0) {
//            return true;
//        }
//        for (int i = 0; i < parameterTypes.length; i++) {
//            CtClass parameterType = parameterTypes[i];
//            boolean isPrimArray = parameterType.isArray() && parameterType.getComponentType().isPrimitive();
//            if ( !isPrimArray && ! parameterType.isPrimitive() && !parameterType.getName().equals(String.class.getName()) ) {
//                return false;
//            }
//        }
//        return true;
//    }

    public String toString(Method m) {
        try {
            StringBuilder sb = new StringBuilder();
            int mod = m.getModifiers() & java.lang.reflect.Modifier.methodModifiers();
            if (mod != 0) {
                sb.append(java.lang.reflect.Modifier.toString(mod)).append(' ');
            }
            sb.append(m.getReturnType()).append(' ');
            sb.append(m.getName()).append('(');
            Class<?>[] params = m.getParameterTypes();
            for (int j = 0; j < params.length; j++) {
                sb.append(params[j].toString());
                if (j < (params.length - 1))
                    sb.append(',');
            }
            sb.append(')');
            return sb.toString();
        } catch (Exception e) {
            return "<" + e + ">";
        }
    }

    public static String toString(CtMethod m) {
        try {
            StringBuilder sb = new StringBuilder();
//            int mod = m.getModifiers() & java.lang.reflect.Modifier.methodModifiers();
//            if (mod != 0) {
//                sb.append(java.lang.reflect.Modifier.toString(mod)).append(' ');
//            }
            sb.append(m.getDeclaringClass().getName()+"::");
            sb.append(m.getReturnType().getName()).append(' ');
            sb.append(m.getName()).append('(');
            CtClass[] params = m.getParameterTypes();
            for (int j = 0; j < params.length; j++) {
                sb.append(params[j].getName());
                if (j < (params.length - 1))
                    sb.append(',');
            }
            sb.append(')');
            return sb.toString();
        } catch (Exception e) {
            return "<" + e + ">";
        }
    }

    protected CtMethod[] getSortedNonPublicCtMethods(CtClass orig) throws NotFoundException {
        int count = 0;
        CtMethod[] methods0 = orig.getMethods();
        for (int i = methods0.length-1; i >= 0; i-- ) {
            CtMethod method = methods0[i];
            if ( ! method.getDeclaringClass().isInterface() ) {
                String str = toString(method);
                boolean isVolatile = method.toString().indexOf("volatile ") >= 0;
                boolean isStatic = Modifier.isStatic(method.getModifiers());
                boolean isPublic = Modifier.isPublic(method.getModifiers());
                if ( isPublic || isVolatile || isStatic || method.getName().startsWith("access$")) // ignore synthetic methods
                {
                    methods0[i] = null;
                }
            }
        }

        CtMethod methods[] = null;
        for (int i = 0; i < methods0.length; i++) {
            CtMethod method = methods0[i];
            if (method != null ) {
                count++;
            }
        }

        methods = new CtMethod[count];
        count = 0;
        for (int i = 0; i < methods0.length; i++) {
            CtMethod method = methods0[i];
            if ( method != null ) {
                methods[count++] = method;
            }
        }

        Arrays.sort(methods, (o1,o2) -> {
            try {
                return (o1.getName() + o1.getReturnType() + o1.getParameterTypes().length).compareTo(o2.getName() + o2.getReturnType() + o2.getParameterTypes().length);
            } catch (NotFoundException e) {
                e.printStackTrace();
                return 0;
            }
        });
        return methods;
    }

    public static CtMethod[] getSortedPublicCtMethods(CtClass orig, boolean onlyRemote) throws NotFoundException {
        //fixme: grown stuff, lots of redundant checks
        int count = 0;
        CtMethod[] methods0 = orig.getMethods();
        HashSet alreadypresent = new HashSet();
        HashSet unqiqueForActors = new HashSet();
        for (int i = methods0.length-1; i >= 0; i-- ) {
            CtMethod method = methods0[i];
            if ( ! method.getDeclaringClass().isInterface() && (method.getModifiers() & AccessFlag.PUBLIC) != 0 ) {
                String str = toString(method);
                boolean isVolatile = method.toString().indexOf("volatile ") >= 0;
                boolean isStatic = Modifier.isStatic(method.getModifiers());
                if ( isVolatile || isStatic ||
                   alreadypresent.contains(str) || method.getName().startsWith("access$")) // ignore synthetic methods
                {
                    methods0[i] = null;
                } else {
                    String key = method.getName();
                    if (unqiqueForActors.contains(key) && !method.getDeclaringClass().getName().equals("java.lang.Object")) {
                        throw new RuntimeException("method overloading not supported for actors. problematic Method: "+key+" on class "+orig.getName());
                    }
                    unqiqueForActors.add(key);
                    alreadypresent.add(str);
                }
            }
        }

        CtMethod methods[] = null;
        if ( onlyRemote ) {
            for (int i = 0; i < methods0.length; i++) {
                CtMethod method = methods0[i];
                if (method != null ) {
                    if ( (method.getModifiers() & AccessFlag.PUBLIC) != 0 )
                    {
                        count++;
                    }
                }
            }
            methods = new CtMethod[count];
            count = 0;
            for (int i = 0; i < methods0.length; i++) {
                CtMethod method = methods0[i];
                if ( (method.getModifiers() & AccessFlag.PUBLIC) != 0 ) {
                    methods[count++] = method;
                }
            }
        } else {
            count = 0;
            for (int i = 0; i < methods0.length; i++) {
                CtMethod method = methods0[i];
                if ( method != null ) {
                    count++;
                }
            }
            methods = new CtMethod[count];
            count = 0;
            for (int i = 0; i < methods0.length; i++) {
                CtMethod method = methods0[i];
                if ( method != null ) {
                    methods[count++] = method;
                }
            }
        }

        Arrays.sort(methods, (o1,o2) -> {
            try {
                return (o1.getName() + o1.getReturnType() + o1.getParameterTypes().length).compareTo(o2.getName() + o2.getReturnType() + o2.getParameterTypes().length);
            } catch (NotFoundException e) {
                e.printStackTrace();
                return 0;
            }
        });
        return methods;
    }
}

