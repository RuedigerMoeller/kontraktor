package org.nustaq.kontraktor.webapp.transpiler;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.webapp.javascript.FileResolver;
import org.nustaq.kontraktor.webapp.javascript.jsmin.JSMin;
import org.nustaq.kontraktor.webapp.transpiler.jsx.JSXGenerator;
import org.nustaq.kontraktor.webapp.transpiler.jsx.JSXParser;

import java.io.*;
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
            JSXGenerator.ParseResult result = JSXGenerator.process(f,false);
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

    private File processNodeDir(File file, FileResolver resolver, Map<String, Object> alreadyResolved) {
        File jfi = new File(file, "package.json");
        if ( jfi.exists() ) {
            try {
                JsonObject pkg = Json.parse(new FileReader(jfi)).asObject();
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
        }
        return null;
    }

    protected byte[] processJSX_Dev(File f, FileResolver resolver, Map<String, Object> alreadyResolved) {
        try {
            JSXGenerator.ParseResult result = JSXGenerator.process(f,true);
            List<JSXParser.ImportSpec> specs = result.getImports();
            byte[] res = result.getFiledata();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(10_000);

            if ( "index.jsx".equals(f.getName()) ) {
                baos.write((getInitialShims()+"\n").getBytes("UTF-8"));
                alreadyResolved.put("JSXIndex",Boolean.TRUE);
            }
            if ( alreadyResolved.get("_Ignored") == null ) {
                alreadyResolved.put("_Ignored",result.getIgnoredRequires());
            }
            Set ignoredRequires = (Set) alreadyResolved.get("_Ignored");
            ignoredRequires.addAll(result.getIgnoredRequires());

            for (int i = 0; i < specs.size(); i++) {
                JSXParser.ImportSpec importSpec = specs.get(i);
                String from = importSpec.getFrom();
                if ( importSpec.isRequire() && ignoredRequires.contains(importSpec.getFrom()) )
                    continue;
                if (from.indexOf("iconv-lite")>=0) {
                    int debug = 1;
                }
                File resolvedFile = resolver.resolveFile(f.getParentFile(), from);
                if ( resolvedFile != null && resolvedFile.isDirectory() ) {
                    File indexFile = processNodeDir(resolvedFile, resolver, alreadyResolved);
                    if ( indexFile == null )
                    {
                        Log.Warn(this,"node directory could not be resolved to a resource :"+resolvedFile.getCanonicalPath());
                        continue;
                    } else {
                        f = indexFile;
                        resolvedFile = indexFile;
                        from = indexFile.getName();
                    }
                }
                if ( ! from.endsWith(".js") && ! from.endsWith(".jsx") ) {
                    from += ".js";
                }
                byte resolved[] = resolver.resolve(f.getParentFile(), from, alreadyResolved);
                if ( resolved == null && from.endsWith(".js") ) {
                    // try jsx
                    from = from.substring(0,from.length()-3)+".jsx";
                    resolved = resolver.resolve(f.getParentFile(), from, alreadyResolved);
                }
                if ( resolved != null ) {
                    if ( resolved.length > 0 ) {
                        // need re-resolve as extension might have changed
                        resolvedFile = resolver.resolveFile(f.getParentFile(),from);
                        String name = constructLibName(resolvedFile, resolver)+".js";
//                        if ( from.endsWith(".jsx") )
                        {
                            name = "dependencies/"+name;
                        }
                        resolver.install("/debug/" + name, resolved);
                        baos.write(
                            ("document.write( '<script src=\"debug/" + name + "\"></script>');\n")
                                .getBytes("UTF-8")
                        );
                    }
                }
                else
                    Log.Warn(this,importSpec.getFrom()+" not found");
            }
            if ( f.getAbsolutePath().indexOf("promise") >= 0)
            {
                int debug=1;
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

            String name = constructLibName(f, resolver)+".jsx_transpiled";
            resolver.install("/debug/" + name, mainBao.toByteArray());
            baos.write(
                ("document.write( '<script src=\"debug/" + name + "\"></script>');\n")
                    .getBytes("UTF-8")
            );
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
        File basedir = result.getFile().getParentFile();
        for (int i = 0; i < imports.size(); i++) {
            JSXParser.ImportSpec spec = imports.get(i);
            File libFile = resolver.resolveFile(basedir, spec.getFrom());
            if ( libFile == null )
                libFile = resolver.resolveFile(basedir, spec.getFrom()+".jsx");
            if ( libFile == null )
                libFile = resolver.resolveFile(basedir, spec.getFrom()+".js");
            if ( libFile == null ) {
                Log.Error(this,"unable to resolve import '"+spec.getFrom()+"' in file "+result.getFile());
            }
            String libname = constructLibName(libFile,resolver);
            String exportObject = "kimports['" + libname+"']";
            if ( spec.getAlias() != null ) {
                s+="  const "+spec.getAlias()+" = "+exportObject+"."+spec.getComponent()+";\n";
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
        String name = JSXGenerator.camelCase(unique);
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
            "  const res = klibmap[libname] ? klibmap[libname]()[identifier] : (window.kimports[libname] ? window.kimports[libname][identifier] : null);\n" +
            "  if ( ! res ) {\n" +
            "    console.error(\"unable to resolve \"+identifier+\" in klibmap['\"+libname+\"'] \")\n" +
            "  }\n" +
            "  return res;\n" +
            "};\n" +
            "window.module = {}; // fake nodejs. this can be problematic if libs detect node by checking for module === undefined\n" +
            "\n";
    }

}
