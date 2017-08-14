package org.nustaq.kontraktor.webapp.transpiler;

import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.webapp.javascript.FileResolver;
import org.nustaq.kontraktor.webapp.javascript.jsmin.JSMin;
import org.nustaq.kontraktor.webapp.transpiler.jsx.JSXGenerator;
import org.nustaq.kontraktor.webapp.transpiler.jsx.JSXParser;

import java.io.*;
import java.util.List;
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
    public byte[] transpile(File f, FileResolver resolver, Set<String> alreadyResolved) {
        if ( dev )
            return processJSX_Dev(f,resolver, alreadyResolved);
        else
            return processJSX_Prod(f,resolver,alreadyResolved);
    }

    protected byte[] processJSX_Prod(File f, FileResolver resolver, Set<String> alreadyResolved) {
        try {
            JSXGenerator.ParseResult result = JSXGenerator.process(f,false);
            List<JSXParser.ImportSpec> specs = result.getImports();
            byte[] res = result.getFiledata();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1_000_000);
            if ( "index.jsx".equals(f.getName()) ) {
                baos.write((getInitialShims()+"\n").getBytes("UTF-8"));
            }
            for (int i = 0; i < specs.size(); i++) {
                JSXParser.ImportSpec importSpec = specs.get(i);
                String from = importSpec.getFrom();
                if ( ! from.endsWith(".js") && ! from.endsWith(".jsx") ) {
                    from += ".js";
                }
                byte[] resolved = resolver.resolve(f.getParentFile(), from, alreadyResolved);
                if ( resolved != null )
                    baos.write(resolved);
                else
                    Log.Warn(this,from+" not found");
            }
            baos.write(generateImportPrologue(f.getName(),result).getBytes("UTF-8"));
            if ( minifyjsx ) {
                res = JSMin.minify(res);
            }
            baos.write(res);
            baos.write(generateImportEnd(f.getName(),result).getBytes("UTF-8"));
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

    protected byte[] processJSX_Dev(File f, FileResolver resolver, Set<String> alreadyResolved) {
        try {
            JSXGenerator.ParseResult result = JSXGenerator.process(f,true);
            List<JSXParser.ImportSpec> specs = result.getImports();
            byte[] res = result.getFiledata();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(10_000);

            if ( "index.jsx".equals(f.getName()) ) {
                baos.write((getInitialShims()+"\n").getBytes("UTF-8"));
            }

            for (int i = 0; i < specs.size(); i++) {
                JSXParser.ImportSpec importSpec = specs.get(i);
                String from = importSpec.getFrom();
                if ( ! from.endsWith(".js") && ! from.endsWith(".jsx") ) {
                    from += ".js";
                }
                byte[] resolved = resolver.resolve(f.getParentFile(), from, alreadyResolved);
                if ( resolved != null ) {
                    System.out.println(""+from+" length:"+resolved.length);
                    if ( resolved.length > 0 ) {
                        String name = constructLibName(from)+".js";
                        if ( from.endsWith(".jsx") ) {
                            name = "dummy/"+name;
                        }
                        resolver.install("/debug/" + name, resolved);
                        baos.write(
                            ("document.write( '<script src=\"debug/" + name + "\"></script>');\n")
                                .getBytes("UTF-8")
                        );
                    }
                }
                else
                    Log.Warn(this,from+" not found");
            }
            ByteArrayOutputStream mainBao = new ByteArrayOutputStream(10_000);
            mainBao.write(generateImportPrologue(f.getName(),result).getBytes("UTF-8"));
            mainBao.write(res);
            mainBao.write(generateImportEnd(f.getName(),result).getBytes("UTF-8"));

            String name = constructLibName(f.getName())+".jsx_transpiled";
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

    protected String generateImportEnd(String name, JSXGenerator.ParseResult result) {
        String s = "\n";
        String exportObject = "kimports." + constructLibName(name);
        s += "  "+exportObject+" = {};\n";
        for (int i = 0; i < result.getGlobals().size(); i++) {
            String gl = result.getGlobals().get(i);
            s+="  "+exportObject+"."+gl+" = "+gl+";\n";
        }
        return s+"});";
    }

    protected String generateImportPrologue(String name, JSXGenerator.ParseResult result) {
        String s = "window.klibmap = window.klibmap || {};\nwindow.kimports = window.kimports || {};\n";
        s += "(new function() {\n";
        List<JSXParser.ImportSpec> imports = result.getImports();
        for (int i = 0; i < imports.size(); i++) {
            JSXParser.ImportSpec spec = imports.get(i);
            String libname = constructLibName(spec.getFrom());
            String exportObject = "kimports." + libname;
            if ( spec.getAlias() != null ) {
                s+="  const "+spec.getAlias()+" = "+exportObject+"."+spec.getComponent()+";\n";
            }
            for (int j = 0; j < spec.getAliases().size(); j++) {
                String alias = spec.getAliases().get(j);
                s+="  const "+alias+" = _kresolve('"+libname+"', '"+spec.getComponents().get(j)+"');\n";
//                s+="  const "+alias+" = klibmap."+libname+"? klibmap."+libname+"()"+"."+spec.getComponents().get(j)
//                    +":"+exportObject+"."+spec.getComponents().get(j)+";\n";
            }
        }
        s += "\n";
        return s;
    }

    protected String constructLibName(String name) {
        name = JSXGenerator.camelCase(new File(name).getName());
        return name.replace(".jsx","").replace(".js","");
    }

    protected String getInitialShims() {
        return "window._sprd = function (obj) {\n" +
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
            "  const res = klibmap[libname] ? klibmap[libname]()[identifier] : window.kimports[libname][identifier];\n" +
            "  if ( ! res ) {\n" +
            "    console.error(\"unable to resolve \"+identifier+\" in klibmap.\"+libname+\" .\")\n" +
            "  }\n" +
            "  return res;\n" +
            "};\n" +
            "window.module = {}; // fake nodejs. this can be problematic if libs detect node by checking for module === undefined\n" +
            "\n";
    }

}
