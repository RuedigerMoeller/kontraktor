package org.nustaq.kontraktor.rest;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTConfiguration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * as simple json message validator
 */
public class MessageValidator {

    FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
    Map<String,Class> type2Clazz = new HashMap<>();

    public MessageValidator map(Class cl) {
        type2Clazz.put(cl.getSimpleName(),cl);
        return this;
    }

    public MessageValidator map(String s, Class cl) {
        type2Clazz.put(s,cl);
        return this;
    }

    public <T> Object read(Class<T> targetType, JsonObject parsed) throws ValidationException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        if ( targetType == null ) {
            JsonValue jtype = parsed.get("type");
            String jtstring;
            if ( jtype == null || ! jtype.isString() )
            {
                if ( type2Clazz.containsKey("") )
                    jtstring = "";
                else
                    throw new ValidationException("missing type information "+parsed.toString() );
            } else
                jtstring = jtype.asString();
            if ( type2Clazz.containsKey(jtstring) ) {
                targetType = type2Clazz.get(jtstring);
            } else {
                targetType = (Class<T>) Class.forName(jtstring);
            }
        }
        Object target = targetType.newInstance();

        FSTClazzInfo classInfo = conf.getClassInfo(target.getClass());
        FSTClazzInfo.FSTFieldInfo[] fields = classInfo.getFieldInfo();
        for (int i = 0; i < fields.length; i++) {
            FSTClazzInfo.FSTFieldInfo field = fields[i];
            if ( field.isPrimitive() || field.getType() == String.class || field.getType().isEnum()) {
                JsonValue jsonValue = parsed.get(field.getName());
                correlatePrimitiveValue(target,field,jsonValue);
            } else if ( field.getField().getAnnotation(JsonOption.class) == null )
                throw new ValidationException("unhandled field '"+field.getName()+"' on "+target.getClass());
        }
        Set<String> names = Arrays.stream(fields).map(x -> x.getName()).collect(Collectors.toSet());
        names.add("type");
        String unknown = parsed.names().stream().filter(name -> !names.contains(name)).collect(Collectors.joining(","));
        if ( unknown.length() > 0 )
            throw new ValidationException("unknown fields in message:"+unknown);
        return target;
    }

    private void correlatePrimitiveValue(Object target, FSTClazzInfo.FSTFieldInfo field, JsonValue jsonValue) throws IllegalAccessException, ValidationException {
        if ( jsonValue == null || jsonValue.isNull() ) {
            if ( field.getField().getAnnotation(JsonOption.class) == null )
                throw new ValidationException("missing field '"+field.getName()+"' on "+target.getClass());
            return;
        }
        if ( field.getType() == String.class ) {
            field.getField().set(target,jsonValue.asString());
        } else
        if ( field.getType() == int.class ) {
            field.getField().set(target,jsonValue.asInt());
        } else
        if ( field.getType() == boolean.class ) {
            field.getField().set(target,jsonValue.asBoolean());
        } else
        if ( field.getType() == double.class ) {
            double value = jsonValue.asDouble();
            field.getField().set(target, value);
        } else
        if ( field.getType() == long.class ) {
            field.getField().set(target,jsonValue.asLong());
        } else
        if ( field.getType() == Long.class ) {
            field.getField().set(target,jsonValue.asLong());
        } else
        if ( field.getType() == Integer.class ) {
            field.getField().set(target,jsonValue.asInt());
        } else
        if ( field.getType() == Boolean.class ) {
            field.getField().set(target,jsonValue.asBoolean());
        } else
        if ( field.getType() == Double.class ) {
            double value = jsonValue.asDouble();
            field.getField().set(target, value);
        } else
        if ( field.getType().isEnum() ) {
            Enum anEnum = valueOfIgnoreCase(field.getType(), jsonValue.asString());
            if ( anEnum == null )
                throw new ValidationException("invalid enum value for field '"+field.getName()+"': value:"+jsonValue.asString()+" valid:"+ Arrays.toString(field.getType().getEnumConstants()));
            field.getField().set(target,anEnum);
        } else
            throw new ValidationException("could not correlate "+field.getName()+" to "+jsonValue);
    }

    public static <T extends Enum<T>> T valueOfIgnoreCase(
        Class<T> enumeration, String name) {

        for (T enumValue : enumeration.getEnumConstants()) {
            if (enumValue.name().equalsIgnoreCase(name)) {
                return enumValue;
            }
        }
        return null;
    }

}
