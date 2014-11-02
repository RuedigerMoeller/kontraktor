package minbin.gen;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.lukehutch.fastclasspathscanner.FastClasspathScanner;
import org.nustaq.kontraktor.annotations.GenRemote;
import org.nustaq.kontraktor.remoting.minbingen.AbstractGen;
import org.nustaq.kontraktor.remoting.minbingen.GenClazzInfo;
import org.nustaq.kontraktor.remoting.minbingen.GenContext;
import org.nustaq.kontraktor.remoting.minbingen.MsgInfo;
import de.ruedigermoeller.template.TemplateExecutor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * Created by ruedi on 26.05.14.
 */
public class MBGen extends AbstractGen {

	protected void generate(String outFile) throws ClassNotFoundException {
        System.out.println("generating to "+new File(outFile).getAbsolutePath());
		GenContext ctx = new GenContext();
        genClzList(outFile, new ArrayList<String>(clazzSet), ctx, infoMap, templateFile);

        try {
            // generate classmapping kson
            File f = new File(outFile);
            if ( !f.isDirectory() )
                f = f.getParentFile();
            f = new File(f,"name-map.kson");
            PrintStream pout = new PrintStream( new FileOutputStream(f) );
            pout.println("{");
            clazzSet.stream().forEach(clzStr -> {
                Class clz = null;
                try {
                    clz = Class.forName(clzStr);
                    String simpleName = clz.getSimpleName();
                    while ( simpleName.length() < 20 )
                        simpleName+=" ";
                    pout.println("    " + simpleName + " : '" + clz.getName()+"'");
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

	protected void genClzList(String outFile, ArrayList<String> finallist, GenContext ctx, HashMap<Class, List<MsgInfo>> infoMap, String templateFile) throws ClassNotFoundException {
		GenClazzInfo infos[] = new GenClazzInfo[finallist.size()];
		for (int i = 0; i < infos.length; i++) {
		    infos[i] = new GenClazzInfo( conf.getClassInfo(Class.forName(finallist.get(i))) );
            infos[i].setMsgs(infoMap.get(infos[i].getClzInfo().getClazz()));
		    if ( infos[i] != null )
		        System.out.println("generating clz "+finallist.get(i));
		}
		ctx.clazzInfos = infos;
		if ( lang == Lang.javascript ) {
			TemplateExecutor.Run(outFile, templateFile, ctx);
		}
	}

    @Override
    public String getTemplateFileOrClazz() {
        return templateFile;
    }

	public static enum Lang {
        javascript,
        dart
    }

    @Parameter( names={"-lang", "-l" }, description = "target language javascript|dart" )
    Lang lang = Lang.javascript;

    @Parameter( names={"-class", "-c"}, description = "class containing generation description (must implement GenMeta) " )
    String clazz = null; //"org.rm.testserver.protocol.Meta";

    @Parameter( names={"-f"}, description = "output dir/file" )
    String out;

    @Parameter( names={"-p"}, description = "',' separated list of whitelist packages" )
    String pack;

    @Parameter( names={"-t"}, description = "templatefile to use" )
    String templateFile = "/js/js.jsp";

    public static void main(String arg[]) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        MBGen gen = new MBGen();
	    JCommander jCommander = new JCommander(gen, arg);
	    if ( (gen.pack == null && gen.clazz == null) || gen.out == null) {
		    jCommander.usage();
		    System.exit(-1);
	    }
        // fixme check args
	    if ( gen.clazz == null ) {
			MBGen mbGen = new MBGen();
			mbGen.lang = gen.lang;
			mbGen.out = gen.out;
		    System.out.println("no class arg given ... scanning classpath for @GenRemote. whitelist:"+gen.pack);
		    new FastClasspathScanner( gen.pack.split(",") )
				.matchClassesWithAnnotation( GenRemote.class, (clazz) -> {
					try {
						mbGen.addTopLevelClass(clazz.getName());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}).scan();

            if (mbGen.clazzSet.size()>0) {
                mbGen.generate(gen.out);
            } else
                System.out.println("no @GenRemote classes found in given packages");
        } else {
            gen.addTopLevelClass(gen.clazz);
            gen.generate(gen.out);
        }
        //gen.addTopLevelClass("org.rm.testserver.protocol.Meta","../testshell/src/main/javascript/js/model.js");

    }

}
