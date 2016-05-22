package org.nustaq.kontraktor.remoting.http.javascript;

import org.nustaq.kontraktor.util.Log;

import java.io.*;
import java.util.List;

/**
 * Created by ruedi on 21.05.16.
 */
public abstract class CLICommandTranspiler implements TranspilerHook {

    String endingToLookFor;

    public CLICommandTranspiler(String endingToLookFor) {
        this.endingToLookFor = endingToLookFor;
    }

    @Override
    public byte[] transpile(File f) throws TranspileException {
        File dir = f.getParentFile();
        String name = f.getName();
        int idx = name.indexOf('.');
        if ( idx <= 0 )
            return null;
        File source = new File(dir,name.substring(0,idx)+endingToLookFor);
        if ( source.exists() && shouldUpdate(f,source) ) {
            ProcessBuilder p = new ProcessBuilder(createCMDLine(f,source)).directory(dir);
            final Process proc;
            try {
                proc = p.start();
            } catch (IOException e) {
                throw new TranspileException(e);
            }
            ioPoller(System.out, proc.getInputStream(), proc);
            ioPoller(System.err, proc.getErrorStream(), proc);
            try {
                int res = proc.waitFor();
                if (res != 0 ) {
                    throw new TranspileException("transpiler returned "+res+" on "+source.getAbsolutePath());
                } else {
                    Log.Info(this, "success transpiling " + source.getAbsolutePath() + " to " + f.getAbsolutePath());
                }
            } catch (InterruptedException e) {
                throw new TranspileException(e);
            }
        }
        return null;
    }

    protected boolean shouldUpdate(File targetJSFile, File source) {
        return !targetJSFile.exists() || source.lastModified() > targetJSFile.lastModified();
    }

    protected abstract String[] createCMDLine(File targetJSFile, File source);

    public void ioPoller( OutputStream fout, InputStream in, Process proc ) {
        final OutputStream finalFout = fout;
        new Thread("io poll "+proc) {
            public void run() {
                try {
                    while( proc.isAlive() ) {
                        int read = in.read();
                        if ( read >= 32 || read == 9 || read == 10 || read == 13 ) {
                            if ( finalFout != null ) {
                                finalFout.write(read);
                            } else
                                System.out.write(read);
                        }
                        else if ( read < 0 ) {
                            break;
                        } else {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException e)  {
                    e.printStackTrace();
                }
            }
        }.start();
    }

}
