package org.nustaq.kontraktor.util.minbingen;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.minbin.GenMeta;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Created by ruedi on 01.11.14.
 */
public abstract class AbstractGen {

    protected FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
    protected HashSet<String> clazzSet = new HashSet<String>();
    protected HashMap<Class, List<MsgInfo>> infoMap = new HashMap<Class, List<MsgInfo>>();

    public void addTopLevelClass(String clazzName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {

	    conf.setForceSerializable(true);

        Class c = Class.forName(clazzName);
        try {
            if ( Actor.class.isAssignableFrom(c) ) {
//                clazzSet.add(c.getName());
                prepareActorMeta(c);
            } else if (GenMeta.class.isAssignableFrom(c)) {

                GenMeta meta = (GenMeta) c.newInstance();
                List<Class> clazz = meta.getClasses();
                for (int i = 0; i < clazz.size(); i++) {
                    Class aClass = clazz.get(i);
	                addClz(clazzSet, aClass, infoMap);
                }
            } else {
                addClz(clazzSet,c,infoMap);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
//        for (int i = 0; i < clazz.length; i++) {
//            Class aClass = clazz[i];
//            FSTClazzInfoRegistry.addAllReferencedClasses(aClass,list,c.getPackage().getName());
//        }

//        for (int i = 0; i < list.size(); i++) {
//            String s = list.get(i);
//            final String pack = c.getPackage().getName();
//            if (!s.startsWith(pack)) {
//                list.remove(i); i--;
//            }
//        }

    }

    protected void generate(String outFile) throws Exception {
        System.out.println("generating to " + new File(outFile).getAbsolutePath());
        GenContext ctx = new GenContext();
        genClzList(outFile, new ArrayList<String>(clazzSet), ctx, infoMap, getTemplateFileOrClazz());

        try {
            // generate classmapping kson
            File f = new File(outFile);
            if (!f.isDirectory())
                f = f.getParentFile();
            f = new File(f, "name-then.kson");
            PrintStream pout = new PrintStream(new FileOutputStream(f));
            pout.println("{");
            clazzSet.stream().forEach(clzStr -> {
                Class clz = null;
                try {
                    clz = Class.forName(clzStr);
                    String simpleName = clz.getSimpleName();
                    while (simpleName.length() < 20)
                        simpleName += " ";
                    pout.println("    " + simpleName + " : '" + clz.getName() + "'");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
            pout.println("}");
            pout.flush();
            pout.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

	protected void addClz(Set<String> clazzSet, Class aClass, HashMap<Class, List<MsgInfo>> infoMap) {
		if ( ! clazzSet.contains(aClass.getName()) ) {
			if ( aClass.getName().startsWith("java.") || aClass.getName().startsWith("javax.") )
				return;
			if (Actor.class.isAssignableFrom(aClass)) {
				prepareActorMeta(aClass);
			} else
				clazzSet.add(aClass.getName());
		}
	}

    private void prepareActorMeta(Class c) {
	    clazzSet.add(c.getName());
	    Method m[] = c.getMethods();
		ArrayList<MsgInfo> methodInfos = new ArrayList<MsgInfo>();
	    for (int i = 0; i < m.length; i++) {
		    Method method = m[i];
		    if (Modifier.isPublic(method.getModifiers()) &&
			    method.getAnnotation(CallerSideMethod.class) == null &&
			    method.getAnnotation(Local.class) == null &&
			    ( method.getReturnType() == void.class || IPromise.class.isAssignableFrom(method.getReturnType()) ) &&
				method.getDeclaringClass() != Object.class &&
                !Modifier.isStatic(method.getModifiers())
			) {
			    Class<?>[] parameterTypes = method.getParameterTypes();
                final java.lang.reflect.Parameter[] parameters = method.getParameters();
                methodInfos.add(new MsgInfo(parameterTypes,method.getName(),method.getReturnType().getSimpleName(),parameters));
			    for (int j = 0; j < parameterTypes.length; j++) {
				    Class<?> parameterType = parameterTypes[j];
				    if (shouldAdd(parameterType))
				    {
					    addClz(clazzSet, parameterType, infoMap);
				    }
			    }
			    if ( IPromise.class.isAssignableFrom( method.getReturnType() ) ) {
				    Type genericReturnType = method.getGenericReturnType();
				    if (genericReturnType instanceof ParameterizedType) {
						ParameterizedType pm = (ParameterizedType) genericReturnType;
					    Type[] actualTypeArguments = pm.getActualTypeArguments();
					    Type clz = actualTypeArguments[0];
					    if ( actualTypeArguments.length > 0 && clz instanceof Class
						     && shouldAdd((Class<?>) clz)) {
						    addClz(clazzSet, (Class) clz, infoMap);
					    }
				    }
			    }
			    System.out.println("method:"+method);
		    }
	    }

        infoMap.put(c, methodInfos);
    }

	private boolean shouldAdd(Class<?> parameterType) {
		return ! Callback.class.isAssignableFrom(parameterType) &&
			 ! parameterType.isPrimitive() &&
			 ! (parameterType.isArray() && parameterType.getComponentType().isPrimitive()) &&
			 ! String.class.isAssignableFrom(parameterType) &&
	         ! parameterType.isArray() &&
//			 Serializable.class.isAssignableFrom(parameterType) &&
//			 ! Actor.class.isAssignableFrom(parameterType) &&
			 ! Number.class.isAssignableFrom(parameterType);
	}

	abstract protected void genClzList(String outFile, ArrayList<String> finallist, GenContext ctx, HashMap<Class, List<MsgInfo>> infoMap, String templateFile) throws Exception;

    abstract public String getTemplateFileOrClazz();
}
