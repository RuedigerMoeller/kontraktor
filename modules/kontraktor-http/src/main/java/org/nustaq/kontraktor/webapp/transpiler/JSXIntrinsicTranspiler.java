package org.nustaq.kontraktor.webapp.transpiler;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.webapp.javascript.FileResolver;
import org.nustaq.kontraktor.webapp.javascript.jsmin.JSMin;
import org.nustaq.kontraktor.webapp.transpiler.jsx.JSXGenerator;
import org.nustaq.kontraktor.webapp.transpiler.jsx.JSXParser;
import org.nustaq.kontraktor.webapp.transpiler.jsx.NodeLibNameResolver;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * transpiles jsx without requiring babel.
 */
public class JSXIntrinsicTranspiler implements TranspilerHook {

    protected final boolean dev;
    private final boolean minifyjsx;

    public JSXIntrinsicTranspiler(boolean dev, boolean minifyjsx) {
        this.dev = dev;
        this.minifyjsx = minifyjsx;
    }

    @Override
    public byte[] transpile(File f) throws TranspileException {
        throw new RuntimeException("should not be called");
    }

    @Override
    public byte[] transpile(File f, FileResolver resolver, Map<String,Object> alreadyResolved) {
        if ( dev )
            return processJSX_Dev(f,resolver, alreadyResolved);
        else
            return processJSX_Prod(f,resolver,alreadyResolved);
    }

    protected byte[] processJSX_Prod(File f, FileResolver resolver, Map<String, Object> alreadyResolved) {
        try {
            JSXGenerator.ParseResult result = JSXGenerator.process(f,false,createNodeLibNameResolver(resolver));
            List<JSXParser.ImportSpec> specs = result.getImports();
            byte[] res = result.getFiledata();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1_000_000);
            if ( "index.jsx".equals(f.getName()) ) {
                baos.write((getInitialShims()+"\n").getBytes("UTF-8"));
                alreadyResolved.put("JSXIndex",Boolean.TRUE);
            }
            for (int i = 0; i < specs.size(); i++) {
                JSXParser.ImportSpec importSpec = specs.get(i);
                String from = importSpec.getFrom();
                if ( ! from.endsWith(".js") && ! from.endsWith(".jsx") ) {
                    from += ".js";
                }
                byte[] resolved = resolver.resolve(f.getParentFile(), from, alreadyResolved);
                if ( resolved == null && from.endsWith(".js") ) {
                    // try jsx
                    from = from.substring(0,from.length()-3)+".jsx";
                    resolved = resolver.resolve(f.getParentFile(), from, alreadyResolved);
                }
                if ( resolved != null )
                    baos.write(resolved);
                else
                    Log.Warn(this,from+" not found");
            }
            if (result.generateESWrap())
                baos.write(generateImportPrologue(result, resolver).getBytes("UTF-8"));
            if ( minifyjsx )
                res = JSMin.minify(res);
            baos.write(res);
            if ( result.generateESWrap() )
                baos.write(generateImportEnd(result, resolver).getBytes("UTF-8"));
            return baos.toByteArray();
        } catch (Exception e) {
            Log.Error(this,e);
            StringWriter out = new StringWriter();
            e.printStackTrace(new PrintWriter(out));
            try {
                return out.getBuffer().toString().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
        }
        return new byte[0];
    }

    private NodeLibNameResolver createNodeLibNameResolver(FileResolver resolver) {
        return new NodeLibNameResolver() {
            @Override
            public String getFinalLibName(File requiredIn, FileResolver res, String requireText) {
                File file = resolver.resolveFile(requiredIn.getParentFile(), requireText);
                if ( file == null )
                    file = resolver.resolveFile(requiredIn.getParentFile(), requireText+".js");
                if ( file == null )
                    file = resolver.resolveFile(requiredIn.getParentFile(), requireText+".jsx");
                if ( file.isDirectory() )
                    file = processNodeDir(file,resolver,new HashMap());
                if ( file == null )
                    return requireText;
                return constructLibName(file,resolver);
            }

            @Override
            public byte[] resolve(File baseDir, String name, Map<String, Object> alreadyProcessed) {
                return resolver.resolve(baseDir,name,alreadyProcessed);
            }

            @Override
            public File resolveFile(File baseDir, String name) {
                return resolver.resolveFile(baseDir,name);
            }

            @Override
            public void install(String path, byte[] resolved) {
                resolver.install(path,resolved);
            }

            @Override
            public String resolveUniquePath(File file) {
                return resolver.resolveUniquePath(file);
            }
        };
    }

    static File falseFile = new File("false");
    String findNodeSubDir(File file) throws IOException {
        if ( file == null )
            return null;
        if ( file.getParentFile() != null && file.getParentFile().getName().equals("node_modules") )
            return file.getCanonicalPath();
        else
            return findNodeSubDir(file.getParentFile());
    }

    private File processNodeDir(File file, FileResolver resolver, Map<String, Object> alreadyResolved) {
        File jfi = new File(file, "package.json");
        if ( jfi.exists() ) {
            try {
                JsonObject pkg = Json.parse(new FileReader(jfi)).asObject();
                JsonValue browser = pkg.get("browser");
                if ( browser != null ) {
                    if (browser.isBoolean() && !browser.asBoolean() ) {
                        return falseFile;
                    }
                    if ( browser.isString() ) {
//                        Log.Info(this,"package.json browser entry map to "+browser.asString());
                        return new File(file,browser.asString());
                    }
                    if ( browser.isObject() ) {
                        String nodeModuleDir = file.getCanonicalPath();
                        JsonObject members = browser.asObject();
                        members.forEach( member -> {
                            String key = "browser_" + nodeModuleDir + "_" + member.getName();
                            alreadyResolved.put(key, member.getValue());
//                            System.out.println("put browser:"+key);
//                            System.out.println("  val:"+member.getValue());
                        });
                    } else {
                        Log.Warn(this, "unrecognized 'browser' entry in package.json, " + file.getCanonicalPath());
                        return null;
                    }
                }

                String main = pkg.getString("main", null);
                if ( main != null ) {
                    File newF = new File(file, main);
                    return newF;
                }
                File indexf = new File(file, "index.js");
                if ( indexf.exists() ) {
                    return indexf;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ( new File(file,"index.js").exists() ) {
            return new File(file,"index.js");
        }
        return null;
    }

    protected byte[] processJSX_Dev(File f, FileResolver resolver, Map<String, Object> alreadyResolved) {
        try {
            JSXGenerator.ParseResult result = JSXGenerator.process(f,true,createNodeLibNameResolver(resolver));
            List<JSXParser.ImportSpec> specs = result.getImports();
            byte[] res = result.getFiledata();
            boolean isInitialIndexJSX = "index.jsx".equals(f.getName());
            if (isInitialIndexJSX) {
                alreadyResolved.put("JSXIndexStart", System.currentTimeMillis());
                ByteArrayOutputStream baos = new ByteArrayOutputStream(1_000_000);
                baos.write((getInitialShims()+"\n").getBytes("UTF-8"));
                alreadyResolved.put("JSXIndex", baos);
            }
            ByteArrayOutputStream baos = (ByteArrayOutputStream) alreadyResolved.get("JSXIndex");
            if ( alreadyResolved.get("_Ignored") == null ) {
                alreadyResolved.put("_Ignored",result.getIgnoredRequires());
            }
            Set ignoredRequires = (Set) alreadyResolved.get("_Ignored");
            ignoredRequires.addAll(result.getIgnoredRequires());

            for (int i = 0; i < specs.size(); i++) {
                JSXParser.ImportSpec importSpec = specs.get(i);
                File redirected = resolveImportSpec(f, importSpec, baos, resolver, alreadyResolved, ignoredRequires);
                if (redirected == null) continue;
            }
            ByteArrayOutputStream mainBao = new ByteArrayOutputStream(10_000);
            if (result.generateESWrap())
                mainBao.write(generateImportPrologue(result, resolver).getBytes("UTF-8"));
            if (result.generateCommonJSWrap())
                mainBao.write(generateCommonJSPrologue(f,result, resolver).getBytes("UTF-8"));
            mainBao.write(res);
            if (result.generateESWrap())
                mainBao.write(generateImportEnd(result, resolver).getBytes("UTF-8"));
            if (result.generateCommonJSWrap())
                mainBao.write(generateCommonJSEnd(f,result, resolver).getBytes("UTF-8"));

            String name = constructLibName(f, resolver)+".transpiled";
            resolver.install("/debug/" + name, mainBao.toByteArray());
            baos.write(
                ("document.write( '<script src=\"debug/" + name + "\"></script>');\n")
                    .getBytes("UTF-8")
            );
            if (isInitialIndexJSX) {
                Long tim = (Long) alreadyResolved.get("JSXIndexStart");
                Log.Info(this, "Transpilation time:"+(System.currentTimeMillis()-tim)/1000.0);
                return baos.toByteArray();
            }
            return mainBao.toByteArray();
        } catch (Exception e) {
            Log.Error(this,e);
            StringWriter out = new StringWriter();
            e.printStackTrace(new PrintWriter(out));
            try {
                return out.getBuffer().toString().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
        }
        return new byte[0];
    }

    private File resolveImportSpec(File requiringFile, JSXParser.ImportSpec importSpec, ByteArrayOutputStream hostingBaos, FileResolver resolver, Map<String, Object> alreadyResolved, Set ignoredRequires) throws IOException {
        String from = importSpec.getFrom();
        File toReadFrom = requiringFile;
        String toReadFromName = null; // node package entry processing
        if ( importSpec.isRequire() ) {
            if (ignoredRequires.contains(importSpec.getFrom()) )
                return null;
            String canonicalF = findNodeSubDir(requiringFile);
            if ( canonicalF != null ) {
                String key = "browser_" + canonicalF + "_" + from;
                JsonValue o = (JsonValue) alreadyResolved.get(key);
                if (o != null) {
                    if (o.isString()) {
                        String oldFrom = from;
                        from = o.asString();
                        Log.Info(this,"mapping package.json/browser:"+oldFrom+" to "+from);
                    } else if (o.isBoolean()) {
                        if (!o.asBoolean()) {
                            Log.Info(this,"ignoring because of package.json/browser:"+from);
                            return null;
                        }
                    } else
                        Log.Warn(this, "unrecognized browser entry in package.json:" + o + ". file:" + requiringFile.getAbsolutePath());
                } else {
//                            System.out.println("key lookup == null for browser setting :"+key);
                }
            } else {
                Log.Warn(this,"node module dir could not be resolved "+ requiringFile.getAbsolutePath());
                return null;
            }
        }
        File resolvedFile = resolver.resolveFile(requiringFile.getParentFile(), from);
        File resolvedNodeDir = null;
        if ( resolvedFile != null && resolvedFile.isDirectory() ) {
            resolvedNodeDir = resolvedFile;
            if (from.indexOf("warning") == 0) {
                int debug = 1;
            }
            File indexFile = processNodeDir(resolvedFile, resolver, alreadyResolved);
            if ( indexFile == falseFile ) {
                return null;
            }
            if ( indexFile == null )
            {
                Log.Warn(this,"node directory could not be resolved to a resource :"+resolvedFile.getCanonicalPath());
                return null;
            } else {
                toReadFrom = indexFile;
                toReadFromName = indexFile.getName();
            }
        } else {
            // fixme: hasExtension
            if (!from.endsWith(".js") && !from.endsWith(".jsx") && !from.endsWith(".json")) {
                from += ".js";
            }
        }
        byte resolved[] = resolver.resolve(toReadFrom.getParentFile(), toReadFromName != null ? toReadFromName : from, alreadyResolved);
        if ( resolved == null && from.endsWith(".js") ) {
            // try jsx
            from = from.substring(0,from.length()-3)+".jsx";
            resolved = resolver.resolve(requiringFile.getParentFile(), from, alreadyResolved);
        }
        if ( resolved != null ) {
            if ( resolved.length > 0 ) {
                // need re-resolve as extension might have changed
                resolvedFile = resolver.resolveFile(toReadFrom.getParentFile(),toReadFromName != null ? toReadFromName : from);
                String name = null;
                if ( resolvedFile.getName().endsWith(".json") ) {
                    name = constructLibName(resolvedFile, resolver) + ".json";
                    ByteArrayOutputStream jsonBao = new ByteArrayOutputStream(resolved.length+100);
                    jsonBao.write("(function(exports, require, module, __filename, __dirname) { module.exports = \n".getBytes("UTF-8"));
                    jsonBao.write(resolved);
                    String s = constructLibName(requiringFile, resolver);
                    jsonBao.write(
                        ("})( kgetModule('"+s+"').exports, krequire, kgetModule('"+s+"'), '', '' );").getBytes("UTF-8"));
                    resolver.install("/debug/" + name, jsonBao.toByteArray());
                }
            }
        }
        else
            Log.Warn(this,importSpec.getFrom()+" not found. requiredBy:"+ requiringFile.getCanonicalPath());
        return requiringFile;
    }

    protected String generateCommonJSPrologue(File f, JSXGenerator.ParseResult result, FileResolver resolver ) {
        return "(function(exports, require, module, __filename, __dirname) {\n";
    }

    protected String generateCommonJSEnd(File f, JSXGenerator.ParseResult result, FileResolver resolver) {
        String s = constructLibName(f, resolver);
        return "})( kgetModule('"+s+"').exports, krequire, kgetModule('"+s+"'), '', '' );";
    }

    protected String generateImportEnd(JSXGenerator.ParseResult result, FileResolver resolver) {
        String s = "\n";
        String exportObject = "kimports['" + constructLibName(result.getFile(), resolver)+"']";
        s += "  "+exportObject+" = {};\n";
        for (int i = 0; i < result.getGlobals().size(); i++) {
            String gl = result.getGlobals().get(i);
            s+="  "+exportObject+"."+gl+" = "+gl+";\n";
        }
        return s+"});";
    }

    protected String generateImportPrologue(JSXGenerator.ParseResult result, FileResolver resolver) {
        String s = "";
        s += "(new function() {\n";
        List<JSXParser.ImportSpec> imports = result.getImports();
        for (int i = 0; i < imports.size(); i++) {
            JSXParser.ImportSpec spec = imports.get(i);
            String libname = createNodeLibNameResolver(resolver).getFinalLibName(result.getFile(),resolver,spec.getFrom());
            String exportObject = "_kresolve('" + libname+"')";
            if ( spec.getAlias() != null ) {
                s+="  const "+spec.getAlias()+" = "+exportObject+";\n";
            }
            for (int j = 0; j < spec.getAliases().size(); j++) {
                String alias = spec.getAliases().get(j);
                s+="  const "+alias+" = _kresolve('"+libname+"', '"+spec.getComponents().get(j)+"');\n";
            }
        }
        s += "\n";
        return s;
    }

    protected String constructLibName(File f, FileResolver resolver) {
        String unique = resolver.resolveUniquePath(f);
        if (unique.startsWith("/"))
            unique = unique.substring(1);
//        System.out.println("unique:"+unique);
        String name = unique;
        if ( name.endsWith(".js") )
            name = name.substring(0,name.length()-3);
        if ( name.endsWith(".jsx") )
            name = name.substring(0,name.length()-4);
        if ( name.endsWith(".json") )
            name = name.substring(0,name.length()-5);
        return name.replace('\\','/');
    }

    protected String getInitialShims() {
        return
            "window.kmodules = {};\n" +
            "\n" +
            "  function kgetModule(name) {\n" +
            "    const res = kmodules[name];\n" +
            "    if ( res == null ) {\n" +
            "      kmodules[name] = { exports: {} };\n" +
            "      return kgetModule(name);\n" +
            "    } else {\n" +
            "      return res;\n" +
            "    }\n" +
            "  }\n" +
            "\n" +
            "  function krequire(name) {\n" +
            "    return kgetModule(name).exports;\n" +
            "  }\n" +
            "\n"+
            "window.klibmap = window.klibmap || {};\nwindow.kimports = window.kimports || {};\n"+
            "window._sprd = function (obj) {\n" +
            "  const copy = Object.assign({},obj);\n" +
            "  Object.keys(obj).forEach( key => {\n" +
            "    if ( key.indexOf(\"...\") == 0 ) {\n" +
            "      const ins = obj[key];\n" +
            "      if ( typeof ins != 'undefined') {\n" +
            "        Object.keys(ins).forEach( ikey => {\n" +
            "          if ( typeof ins[ikey] != 'undefined')\n" +
            "          {\n" +
            "            obj[ikey] = ins[ikey];\n" +
            "          }\n" +
            "        });\n" +
            "        delete obj[key];\n" +
            "      }\n" +
            "    } else {\n" +
            "      obj[key] = copy[key]; // overwrite original value\n" +
            "    }\n" +
            "  });\n" +
            "  return obj;\n" +
            "};\n" +
            "window._kresolve = function (libname,identifier) {\n" +
            "  var res = klibmap[libname] ? klibmap[libname]()[identifier] : (window.kimports[libname] ? window.kimports[libname][identifier] : null);\n" +
            "  if ( ! res ) {\n" +
            "    if ( !identifier)\n"+
            "        res = kmodules[libname] ? kmodules[libname].exports : null;\n" +
            "    else\n"+
            "        res = kmodules[libname] ? kmodules[libname].exports[identifier] : null;\n" +
            "  }\n" +
            "  if ( ! res ) {\n" +
            "    console.error(\"unable to resolve \"+identifier+\" in klibmap['\"+libname+\"'] \")\n" +
            "  }\n" +
            "  return res;\n" +
            "};\n" +
            "window.module = {}; \n" +
            "window.process = { env: {} };"+
        "\n";
    }

}
