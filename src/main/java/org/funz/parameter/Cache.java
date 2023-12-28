
package org.funz.parameter;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.funz.Constants;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.util.Digest;
import org.funz.util.Disk;

public class Cache {

    public interface CacheActivityReport {
        public void report(String s);
    }

    static File OutputDir(File in) {
        return new File(in.getParentFile(), Constants.OUTPUT_DIR);
    }

    static File InfoFile(File in) {
        File infoFile = new File(in.getParentFile(), Case.FILE_INFO);
        if(!infoFile.exists()) {
            File oldInfoFile = new File(in.getParentFile(), Case.OLD_FILE_INFO);
            if(oldInfoFile.exists()) {
                infoFile = oldInfoFile;
            }
        }
        return infoFile;
    }

    static boolean IsOutputDir(File odir) {
        return odir.exists() && odir.listFiles() != null && odir.listFiles().length > 0;
    }

    static byte[][] Sums(File dir) {
        //System.out.println("Suming "+dir.getPath());
        File[] f = Disk.listRecursiveFiles(dir);
        byte[][] md5sums = new byte[f.length][];
        for (int i = 0; i < f.length; i++) {
            md5sums[i] = Digest.getSum(f[i]);
        }
        return md5sums;
    }

    abstract class InputDir {

        File dir;
        byte[][] md5sums;

        InputDir(File d) {
            dir = d;
        }

        boolean contentEquals(InputDir idir) {
            if (!dir.exists()) {
                return false;
            }

            if (idir.md5sums.length != md5sums.length) {
                return false;
            }

            boolean found;
            for (int i = 0; i < md5sums.length; i++) {
                found = false;
                for (int j = 0; j < idir.md5sums.length; j++) {
                    if (Digest.matches(md5sums[i], idir.md5sums[j])) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }

        boolean contentIncludes(InputDir idir) {
            if (idir.md5sums.length >= md5sums.length) {
                return false;
            }

            boolean found;
            for (int i = 0; i < md5sums.length; i++) {
                found = false;
                for (int j = 0; j < idir.md5sums.length; j++) {
                    if (Digest.matches(md5sums[i], idir.md5sums[j])) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            return true;
        }

        public boolean hasCheckSum(byte[] wantedCheckSum) {
            for (int i = 0; i < this.md5sums.length; i++) {
                if (Digest.matches(md5sums[i], wantedCheckSum)) {
                    return true;
                }
            }
            return false;
        }
    }

    class NewInputDir extends InputDir {

        public NewInputDir(File d) {
            super(d);
            md5sums = Sums(dir);
        }
    }

    class CacheInputDir extends InputDir {

        File odir;
        File info;
        boolean hasOutput = false;
        boolean hasInfo = false;
        String code = "?";
        boolean mayHaveFailed = false;

        @Override
        public String toString() {
            return "{" + dir + ";" + odir + "}";
        }

        public CacheInputDir(File d) {
            super(d);
            odir = OutputDir(dir);
            hasOutput = IsOutputDir(odir);
            info = InfoFile(dir);
            hasInfo = info.exists();

            if (hasOutput) {
                md5sums = Sums(dir);
            }


            if (hasInfo) {
                Properties infos = Case.readInfoFile(info);
                code = info.exists() ? infos.getProperty(Case.PROP_CODE) : "?";

                Set<String> vals = infos.stringPropertyNames();
                mayHaveFailed = true;
                for (String v : vals) {
                    if (v.startsWith(Case.PROP_OUT) && (infos.getProperty(v) != null && infos.getProperty(v).length() > 0)) {
                        mayHaveFailed = false;
                        break;
                    }
                }
            }
        }

        File getOutputDir() {
            if (hasOutput) {
                return odir;
            } else {
                return null;
            }
        }

        File getInfoFile() {
            if (hasInfo) {
                return info;
            } else {
                return null;
            }
        }

        String getCode() {
            if (hasInfo) {
                return code;
            } else {
                return null;
            }
        }

        boolean mayHaveFailed() {
            return mayHaveFailed;
        }
    }
    public final List<CacheInputDir> poolInputDirs = new LinkedList<CacheInputDir>();
    private List<File> resultsPool;
    public volatile boolean initialized = false;
    CacheActivityReport[] reporters;

    @Override
    public String toString() {
        return resultsPool.toString();
    }
    
    public Cache(List<File> resultsPool, CacheActivityReport... reporters) {
        this(resultsPool, true, false, reporters);
    }

    public Cache(List<File> resultsPool, final boolean cleanDirs, boolean sync, CacheActivityReport... reporters) {
        this.resultsPool = resultsPool;
        this.reporters = reporters;

        if (cleanDirs) {
            for (File f : resultsPool) {
                try {
                    Disk.removeEmptyDirs(f, 10);
                } catch (IOException e) {
                    Log.logException(false, e);
                }
            }
        }

        if (!sync) {
            new Thread() {

                @Override
                public void run() {
                    init(cleanDirs);
                }
            }.start();
        } else {
            init(cleanDirs);
        }
    }

    public void init(boolean cleanup) {
        LinkedList<File> inputDirs = new LinkedList<File>();
        for (int i = 0; i < resultsPool.size(); i++) {
            if (resultsPool.get(i).exists()) {
                for (CacheActivityReport r : reporters) {
                    r.report("adding " + resultsPool.get(i).getName());
                }
                inputDirs.addAll(findInputDirs(resultsPool.get(i), cleanup));
            }
        }
        synchronized (poolInputDirs) {
            for (int i = 0; i < inputDirs.size(); i++) {
                for (CacheActivityReport r : reporters) {
                    r.report("caching " + inputDirs.get(i).getPath() + " ...");
                }
                poolInputDirs.add(new CacheInputDir(inputDirs.get(i)));
            }
        }
        for (CacheActivityReport r : reporters) {
            r.report("caching done.");
        }
        initialized = true;
    }

    public int size() {
        synchronized (poolInputDirs) {
            return poolInputDirs.size();
        }
    }

    static LinkedList<File> findInputDirs(File root, boolean cleanup) {
        LinkedList<File> inputDirs = new LinkedList<File>();
        File[] children = root.listFiles();
        for (int i = 0; i < children.length; i++) {
            if (children[i].isDirectory()) {
                if (children[i].getName().equals(Constants.INPUT_DIR)) {
                    if (IsOutputDir(OutputDir(children[i]))) {
                        inputDirs.add(children[i]);
                    } else {
                        if (cleanup) {
                            children[i].getParentFile().delete();
                        }
                    }
                } else {
                    inputDirs.addAll(findInputDirs(children[i], cleanup));
                }
            }
        }
        return inputDirs;
    }

    public File getMatchingOutputDirinCache(File tmpinputDir, String code) {
        return getMatchingOutputDirinCache(tmpinputDir, code);
    }

    /**
     * Return matching output directory if the input dir is the same (same md5 for all files)
     * If wantedCheckSum!=null, we only check if there is a file in "input" that has this md5. Other files can be differents
     *
     * @param tmpinputDir - the input directory to check
     * @param code - the code to match
     * @param wantedCheckSum - md5sum of a unique file we want to find in "input" directory
     *
     * @return the "output" directory if we found a match
     */
    public File getMatchingOutputDirinCache(File tmpinputDir, String code, byte[] wantedCheckSum) {
        assert tmpinputDir != null;

        while (!initialized) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        NewInputDir idir = new NewInputDir(tmpinputDir);

        synchronized (poolInputDirs) {
            //byte[] wantedCheckSum = Digest.getSum(fileToMatch);
            for (int i = 0; i < poolInputDirs.size(); i++) {
                CacheInputDir cacheInputDir = poolInputDirs.get(i);
                if (!cacheInputDir.mayHaveFailed()
                        && cacheInputDir.getCode() != null
                        && cacheInputDir.getCode().equals(code)) {

                    boolean foundDirMath = false;
                    if(wantedCheckSum != null) {
                        foundDirMath = cacheInputDir.hasCheckSum(wantedCheckSum);
                    } else {
                        foundDirMath = cacheInputDir.contentEquals(idir);
                    }
                    if(foundDirMath) {
                        File possible = poolInputDirs.get(i).getOutputDir();
                        File[] possible_content = possible.listFiles();
                        if (possible_content.length > 0) {
                            boolean out = false;
                            boolean err = false;
                            for (File file : possible_content) {
                                if (file.getName().equals("out.txt")) {
                                    out = true;
                                }
                                if (file.getName().equals("err.txt")) {
                                    err = true;
                                }
                            }
                            if (out && err) {
                                assert possible != null : "The output dir of input pool " + poolInputDirs.get(i).dir.getAbsolutePath() + " is empty !";
                                Log.logMessage(this, SeverityLevel.INFO, false, "Identical case found: " + poolInputDirs.get(i).dir.getAbsolutePath());
                                //synchronized (poolInputDirs) {
                                poolInputDirs.remove(i);
                                //}
                                return possible;
                            } else {
                                poolInputDirs.remove(i);
                            }
                        }
                    }

                }
            }
        }
        return null;
    }
}
