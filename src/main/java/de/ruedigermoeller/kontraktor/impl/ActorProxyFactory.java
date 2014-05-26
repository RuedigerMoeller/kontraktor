package de.ruedigermoeller.kontraktor.impl;

import de.ruedigermoeller.kontraktor.*;
import de.ruedigermoeller.kontraktor.annotations.CallerSideMethod;
import javassist.*;
import javassist.bytecode.AccessFlag;

import java.io.Externalizable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

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
 * To change this template use File | Settings | File Templates.
 */
public class ActorProxyFactory {

    HashMap<String,Class> generatedProxyClasses = new HashMap<String, Class>();

    public ActorProxyFactory() {
    }

    public <T> T instantiateProxy(Actor target) {
        try {
            Class proxyClass = createProxyClass(target.getClass());
            Constructor[] constructors = proxyClass.getConstructors();
            T instance = null;
            try {
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

    protected <T> Class<T> createProxyClass(Class<T> clazz) throws Exception {
        synchronized (generatedProxyClasses) {
            String proxyName = clazz.getName() + "_ActorProxy";
            String key = clazz.getName();
            Class ccClz = generatedProxyClasses.get(key);
            if (ccClz == null) {
                ClassPool pool = ClassPool.getDefault();
                CtClass cc = null;
                try {
                    cc = pool.getCtClass(proxyName);
                } catch (NotFoundException ex) {
                    //ignore
                }
                if (cc == null) {
                    cc = pool.makeClass(proxyName);
                    CtClass orig = pool.get(clazz.getName());
                    cc.setSuperclass(orig);
                    cc.setInterfaces(new CtClass[]{pool.get(Externalizable.class.getName()), pool.get(ActorProxy.class.getName())});

                    defineProxyFields(pool, cc);
                    defineProxyMethods(cc, orig);
                }

                ccClz = loadProxyClass(clazz, pool, cc);
                generatedProxyClasses.put(key, ccClz);
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

    protected void defineProxyMethods(CtClass cc, CtClass orig) throws Exception {
        cc.addMethod( CtMethod.make( "public void __setDispatcher( "+ DispatcherThread.class.getName()+" d ) { __target.__dispatcher(d); }", cc ) );
        CtMethod[] methods = getSortedPublicCtMethods(orig,false);
        for (int i = 0; i < methods.length; i++) {
            CtMethod method = methods[i];
            CtMethod originalMethod = method;
            if (method.getName().equals("getActor")) {
                ClassMap map = new ClassMap();
                map.put(Actor.class.getName(),Actor.class.getName());
                method = CtMethod.make( "public "+Actor.class.getName()+" getActor() { return __target; }", cc ) ;
            } else {
                ClassMap map = new ClassMap();
                map.fix(orig);
                method = new CtMethod(method, cc, map);
            }
//            CtClass[] parameterTypes = method.getParameterTypes();
            CtClass returnType = method.getReturnType();
            boolean isCallerSide = // don't touch
                    originalMethod.getAnnotation(CallerSideMethod.class) != null ||
                    (originalMethod.getName().equals("self") || originalMethod.getName().equals("future"));
            boolean allowed = ((method.getModifiers() & AccessFlag.ABSTRACT) == 0 ) &&
                    (method.getModifiers() & (AccessFlag.NATIVE|AccessFlag.FINAL|AccessFlag.STATIC)) == 0 &&
                    (method.getModifiers() & AccessFlag.PUBLIC) != 0 &&
                    !isCallerSide;
            allowed &= !originalMethod.getDeclaringClass().getName().equals(Object.class.getName()) &&
                       !originalMethod.getDeclaringClass().getName().equals(Actor.class.getName()) ;

            if ( originalMethod.getName().equals("executeInActorThread") ) {
                allowed = true;
            }

            if (allowed) {
                boolean isVoid = returnType == CtPrimitiveType.voidType;
                if (returnType != CtPrimitiveType.voidType && !returnType.getName().equals(Future.class.getName()) ) {
                    throw new RuntimeException("only void methods or methods returning Future allowed");
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
                                    System.out.println("InThread unnecessary when using built in Callback class");
                                    continue;
                                }
                                if ( ! parameterTypes[j].isInterface() )
                                    throw new RuntimeException("@InThread can be used on interfaces only");
                                conversion += "args[" + j + "] = de.ruedigermoeller.kontraktor.Actors.InThread(args[" + j + "]);";
                                break;
                            }
                        }
                    }
                }
                String call = "__target.__dispatchCall( this, \""+method.getName()+"\", args );";
                if ( ! isVoid ) {
                    call = "return ("+originalMethod.getReturnType().getName()+") (Object)"+call;
                }
                String body = "{ Object args[] = $args;" +
                        conversion +
                        call+
                    "}";
                method.setBody(body);
                cc.addMethod(method);
//                System.out.println("patch method 0 "+method.getName());
            } else if ( (method.getModifiers() & (AccessFlag.NATIVE|AccessFlag.FINAL|AccessFlag.STATIC)) == 0 )
            {
                if (isCallerSide || method.getName().equals("toString")) {
                    // do nothing
                } else if ( method.getName().equals("getDispatcher") ) {
                    method.setBody(" return __target.getDispatcher();");
                    cc.addMethod(method);
//                    System.out.println("patch method 1 "+method.getName());
                } else if ( ! method.getName().equals("getActor") ) {
                    method.setBody("throw new RuntimeException(\"can only call public methods on actor ref\");");
                    cc.addMethod(method);
//                    System.out.println("patch method 2 "+method.getName());
                } else {
                    cc.addMethod(method);
//                    System.out.println("patch method 3 "+method.getName());
                }
            }
        }
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

    public String toString(CtMethod m) {
        try {
            StringBuilder sb = new StringBuilder();
            int mod = m.getModifiers() & java.lang.reflect.Modifier.methodModifiers();
            if (mod != 0) {
                sb.append(java.lang.reflect.Modifier.toString(mod)).append(' ');
            }
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

    protected CtMethod[] getSortedPublicCtMethods(CtClass orig, boolean onlyRemote) throws NotFoundException {
        int count = 0;
        CtMethod[] methods0 = orig.getMethods();
        HashSet alreadypresent = new HashSet();
        HashSet unqiqueForActors = new HashSet();
        for (int i = methods0.length-1; i >= 0; i-- ) {
            CtMethod method = methods0[i];
            String str = toString(method);
            if (alreadypresent.contains(str)||method.getName().startsWith("access$")) {
                methods0[i] = null;
            } else {
                String key = method.getName();
                if ( unqiqueForActors.contains(key) && !method.getDeclaringClass().getName().equals("java.lang.Object") ) {
                    throw new RuntimeException("method overloading not supported for actors.");
                }
                unqiqueForActors.add(key);
                alreadypresent.add(str);
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

        Arrays.sort(methods, new Comparator<CtMethod>() {
            @Override
            public int compare(CtMethod o1, CtMethod o2) {
                try {
                    return (o1.getName() + o1.getReturnType() + o1.getParameterTypes().length).compareTo(o2.getName() + o2.getReturnType() + o2.getParameterTypes().length);
                } catch (NotFoundException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        });
        return methods;
    }
}

