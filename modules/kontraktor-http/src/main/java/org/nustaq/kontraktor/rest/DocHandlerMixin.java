package org.nustaq.kontraktor.rest;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.undertow.util.HeaderMap;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.remoting.base.JsonMapable;
import org.nustaq.kontraktor.rest.doc.ApiOp;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public interface DocHandlerMixin {

    @CallerSideMethod boolean isDocEnabled();
    AtomicReference<ObjectMapper> mapper = new AtomicReference<>();
    private ObjectMapper getMapper() {
        if ( mapper.get() == null) {
            ObjectMapper mp = new ObjectMapper();
            mp.setSerializationInclusion(JsonInclude.Include.ALWAYS);
            mp.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mp.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
            mp.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mp.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
            mp.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
            mp.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
            mapper.set(mp);
        }
        return mapper.get();
    }

    private String methodPrefix(String mname) {
        if ("getDocumentation".equals(mname)) return null;
        if ("getClass".equals(mname)) return null;
        for (Iterator<String> iterator = HttpMethods.iterator(); iterator.hasNext(); ) {
            String htm = iterator.next();
            if ( mname.toLowerCase().startsWith(htm) ) {
                return htm;
            }
        }
        return null;
    }
    static Set<String> HttpMethods = Set.of( UndertowRESTHandler.METHODS);
    default IPromise getDocumentation( String path[]) {
        if ( ! isDocEnabled() )
            return new Promise(404 );
        Class<? extends DocHandlerMixin> aClass = this.getClass();
        Method[] methods = aClass.getMethods();
        List<Method> ml = Arrays.stream(methods)
            .filter(m ->
                !Modifier.isStatic(m.getModifiers()) &&
                    Modifier.isPublic(m.getModifiers()) &&
                    methodPrefix(m.getName() ) != null &&
                    m.getDeclaringClass() != Actor.class
            ).collect(Collectors.toList());
        OpenAPIDefinition apiDoc = aClass.getAnnotation(OpenAPIDefinition.class);
        String docTitle = apiDoc == null ? aClass.getSimpleName() : !apiDoc.info().title().isEmpty() ? apiDoc.info().title() : "no title";
        String docDesc = apiDoc == null ? "" : !apiDoc.info().description().isEmpty() ? apiDoc.info().description() : "";;

        StringBuilder res = new StringBuilder();
        res.append("<html>");
        res.append("<h1>"+docTitle+"</h1>");
        res.append("<i>"+docDesc+"</i>");

        ml.forEach( method -> {
            generateMethod(res,method);
        });

        res.append("</html>");
        return new Promise(res.toString());
    }

    private void generateMethod(StringBuilder res, Method method) {
        Operation op = method.getAnnotation(Operation.class);
        if ( op != null && op.hidden() ) return;
        String httpMethod = methodPrefix( method.getName() );
        String path = method.getName().substring(httpMethod.length(),httpMethod.length()+1).toLowerCase() + method.getName().substring(httpMethod.length()+1);
        String summary = op == null ? "" : op.summary();
        String description = op == null ? "" : op.description();
        List<Pair<FromQuery,Class>> qp = extractQueryParams(method.getParameterAnnotations(), method.getParameters() );
        List<Pair<String,Class>> pp = extractPathParams(method.getParameterAnnotations(), method.getParameters() );
        Class postDTO = extractPostDTO(method.getParameterAnnotations(), method.getParameters() );
        path = "/"+path+pathString(pp)+parameterString(qp);
        res.append("<div style='display: inline-block; background:#eee; margin:4px; padding: 8px; width: 97%;'>");

        lineHeader(res, httpMethod.toUpperCase(),"#fff",false);
        res.append("<div style='display: inline-block;margin: 2px;'><b>"+path+"</b></div>");
        ApiOp apiOp = method.getAnnotation(ApiOp.class);
        if ( apiOp != null && apiOp.docPostDataDTO() != Void.class ) {
            // always override reflection with explicit declaration
            postDTO = apiOp.docPostDataDTO();
        }
        if ( apiOp != null && summary.isEmpty() )
            summary = apiOp.summary();
        if ( ! summary.isEmpty() ) {
//            lineHeader(res, "&nbsp;","none");
            res.append("<div style='display: inline-block; margin: 4px;'> - <i>"+summary+"</i></div>");
        }
        if ( apiOp != null && description.isEmpty() )
            description = apiOp.description();
        if ( ! description.isEmpty() ) {
//            lineHeader(res, "&nbsp;","none");
            res.append("<div style='display: inline-block; margin: 4px;'><i>"+description+"</i></div>");
        }

        if ( qp.size() > 0 ) {
            qp.forEach( ap -> parameterDoc(res,ap));
        }

        if ( postDTO != null ) {
            String dtoStr = postDTO.getSimpleName();
            dtoStr = getDTOString(postDTO, dtoStr);
            lineHeader(res, "post data","none");
            if ( apiOp != null && ! apiOp.requestContainer().isEmpty() ) {
                dtoStr = apiOp.requestContainer()+" of \n"+dtoStr;
            }
            res.append("<div style='vertical-align: top;display: inline-block; margin: 2px;'><pre>"+dtoStr+"</pre></div>");
        }

        if (op !=null && op.responses().length > 0 ) {
            ApiResponse[] responses = op.responses();
            for (int i = 0; i < responses.length; i++) {
                ApiResponse response = responses[i];
                Content[] contArr = response.content();
                for (int j = 0; j < contArr.length; j++) {
                    Content content = contArr[j];
                    Class clz = content.schema().implementation();
                    if ( clz == Void.class && apiOp != null )
                        clz = apiOp.response();
                    if ( clz != Void.class ) {
                        String dtoStr = clz.getSimpleName();
                        dtoStr = getDTOString(clz, dtoStr);
                        if ( apiOp != null && ! apiOp.container().isEmpty() ) {
                            dtoStr = apiOp.container()+" of \n"+dtoStr;
                        }
                        lineHeader(res, "response","none");
                        res.append("<div style='vertical-align: top;display: inline-block; margin: 2px;'><pre>"+dtoStr+"</pre></div>");
                    } else { // arrayschema
                        clz = content.array().schema().implementation();
                        if ( clz == Void.class && apiOp != null )
                            clz = apiOp.response();
                        if ( clz != Void.class ) {
                            String dtoStr = clz.getSimpleName();
                            dtoStr = getDTOString(clz, dtoStr);
                            if ( apiOp != null && ! apiOp.container().isEmpty() ) {
                                dtoStr = apiOp.container()+" of \n"+dtoStr;
                            } else {
                                dtoStr = "array of \n"+dtoStr;
                            }
                            lineHeader(res, "response","none");
                            res.append("<div style='vertical-align: top;display: inline-block; margin: 2px;'><pre>"+dtoStr+"</pre></div>");
                        } else { // arrayschema
                        }
                    }
                }
            }
        } else if ( apiOp != null && apiOp.response() != Void.class ){
            Class clz = apiOp.response();
            String dtoStr = clz.getSimpleName();
            dtoStr = getDTOString(clz, dtoStr);
            if ( ! apiOp.container().isEmpty() ) {
                dtoStr = apiOp.container()+" of \n"+dtoStr;
            }
            lineHeader(res, "response","none");
            res.append("<div style='vertical-align: top;display: inline-block; margin: 2px;'><pre>"+dtoStr+"</pre></div>");
        }

        res.append("</div>");
    }

    private String getDTOString(Class clz, String dtoStr) {
        if ( !JsonMapable.class.isAssignableFrom(clz) )
            return dtoStr;
        try {
            JsonMapable o = (JsonMapable) clz.newInstance();
            if ( o != null ) {
                JsonObject parse = (JsonObject) Json.parse(getMapper().writeValueAsString(o));
                putTypes(clz, parse);
                dtoStr = parse.toString(WriterConfig.PRETTY_PRINT);
            }
        } catch (Throwable t) {
            Log.Info(this,t);
        }
        return dtoStr;
    }

    private void putTypes(Class clz, JsonObject parse) {
        parse.names().forEach(name -> {
            try {
                Field field = clz.getDeclaredField(name);
                if (field != null) {
                    if (JsonMapable.class.isAssignableFrom(field.getType())) {
                        JsonValue jsonValue = parse.get(name);
                        if (jsonValue.isNull()) {
                            JsonMapable o = (JsonMapable) field.getType().newInstance();
                            parse.set(name, Json.parse(getMapper().writeValueAsString(o)) );
                            putTypes(clz, parse); // recurse
                        } else
                            putTypes(field.getType(), (JsonObject) parse.get(name));
                    } else {
                        parse.set(name, "&lt;" + field.getType().getSimpleName() + "&gt;");
                    }
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }

    private void parameterDoc(StringBuilder res, Pair<FromQuery,Class> ap) {
        if ( !ap.car().desc().isEmpty() ) {
            lineHeader(res, "param", "none");
            res.append("<div style='display: inline-block; margin: 2px;'> &lt;" + ap.cdr().getSimpleName() + "&gt; <b>" + ap.car().value() + "</b> - <i>" + ap.car().desc() + "</i></div>");
        }
    }

    private String parameterString(List<Pair<FromQuery,Class>> li) {
        if ( li.isEmpty() )
            return "";
        String res = "?";
        for (int i = 0; i < li.size(); i++) {
            Pair<FromQuery,Class> pair = li.get(i);
            Class clz = pair.cdr();
            String type = clz.getSimpleName();
            res+=pair.car().value()+"=&lt;"+ type +"&gt;";
        }
        return res;
    }
    private String pathString(List<Pair<String,Class>> li) {
        if ( li.isEmpty() )
            return "";
        String res = "";
        for (int i = 0; i < li.size(); i++) {
            Pair<String,Class> pair = li.get(i);
            Class clz = pair.cdr();
            String type = clz.getSimpleName();
            res+="/"+pair.car()+"&lt;"+ type +"&gt;";
        }
        return res;
    }

    private List<Pair<FromQuery,Class>> extractQueryParams(Annotation[][] annotatedParameterTypes, Parameter p[]) {
        List<Pair<FromQuery,Class>> queryParams = new ArrayList<>();
        for (int i = 0; i < annotatedParameterTypes.length; i++) {
            Annotation ans[] = annotatedParameterTypes[i];
            Parameter par = p[i];
            for (int j = 0; j < ans.length; j++) {
                Annotation an = ans[j];
                if ( an.annotationType() == FromQuery.class  ) {
                    queryParams.add(new Pair<>((FromQuery) an,par.getType()));
                }
            }
        }
        return queryParams;
    }

    private List<Pair<String,Class>> extractPathParams(Annotation[][] annotatedParameterTypes, Parameter p[]) {
        List<Pair<String,Class>> queryParams = new ArrayList<>();
        for (int i = 0; i < annotatedParameterTypes.length; i++) {
            Annotation ans[] = annotatedParameterTypes[i];
            boolean hadSpecialAnnotation = false;
            Parameter par = p[i];
            Class<?> parType = par.getType();
            String desc = "";
            for (int j = 0; j < ans.length; j++) {
                Annotation an = ans[j];
                if ( an.annotationType() == FromQuery.class ||
                    an.annotationType() == RequestPath.class ||
                    an.annotationType() == AuthCredentials.class
                ) {
                    hadSpecialAnnotation = true;
                    if ( an.annotationType() == FromQuery.class) {
                        desc = ((FromQuery) an).desc();
                    }
                }
            }
            if (JsonMapable.class.isAssignableFrom(parType) ||
                parType == HeaderMap.class ||
                parType == String[].class ||
                parType == byte[].class ||
                parType == JsonObject.class ||
                Map.class.isAssignableFrom(parType) ||
                parType == JsonValue.class
            )
                hadSpecialAnnotation = true;
            if ( ! hadSpecialAnnotation ) {
                queryParams.add(new Pair(desc, parType));
            }
        }
        return queryParams;
    }
    private Class extractPostDTO(Annotation[][] annotatedParameterTypes, Parameter p[]) {
        for (int i = 0; i < annotatedParameterTypes.length; i++) {
            Parameter par = p[i];
            Class<?> parType = par.getType();
            if (JsonMapable.class.isAssignableFrom(parType)) {
                return parType;
            }
        }
        return null;
    }

    private static void lineHeader(StringBuilder res, String httpMethod, String col) {
        lineHeader(res,httpMethod,col,true);
    }

    private static void lineHeader(StringBuilder res, String httpMethod, String col,boolean br) {
        if ( br )
            res.append("<br/>");
        res.append("<div style='vertical-align: top; display: inline-block; width: 80px; padding: 2px; margin: 0; background: "+col+"; color:black'>"+ httpMethod +"</div>&nbsp;");
    }
}
