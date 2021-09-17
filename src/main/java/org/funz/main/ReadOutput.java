package org.funz.main;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.funz.api.Funz_v1;
import org.funz.api.Utils;
import static org.funz.main.MainUtils.S;
import org.funz.util.ASCII;
import static org.funz.util.Data.newMap;
import static org.funz.util.Format.MapToCSVString;
import static org.funz.util.Format.MapToJSONString;
import static org.funz.util.Format.MapToMDString;
import static org.funz.util.Format.MapToXMLString;

/**
 *
 * @author richet
 */
public class ReadOutput extends MainUtils{

    static String name = "ReadOutput";

    static {
        init(name,10);
    }

    static enum Option {

        HELP("Help", "help", "h", "Display help"),
        MODEL("Model", "model", "m", "Code output to parse (requires input files):\n" + S + ASCII.cat("\n" + S, Funz_v1.getModelList())),
        INPUT_FILES("Input files", "input_files", "if", "List of input files for code"),
        VERBOSITY("Verbosity", "verbosity", "v", "Verbosity level in 0-10"),
        GET_EXPRESSION("Get expression", "get_expression", "ge", "Parsing sequence to get an output (eg. lines(\"out.txt\")>>get(0)>>after(\"=\")>>asNumeric())"),
        OUTPUT_DIR("Output directory", "output_dir", "od", "Directory where to output files are stored"),
        PRINT("Print file", "print", "p", "Filename with data format: xml json or csv (default if not recognized)");

        String name;
        String key;
        String short_key;
        String help;

        Option(String name, String key, String short_key, String help) {
            this.name = name;
            this.key = key;
            this.short_key = short_key;
            this.help = help;
        }

        public boolean is(String word) {
            return (word.equals("--" + key) | word.equals("-" + short_key));

        }

        @Override
        public String toString() {
            return StringUtils.rightPad("--" + key + ", -" + short_key, 30) + name + "\n" + StringUtils.rightPad("", 40) + help;
        }

    }

    private static boolean isOption(String word) {
        for (Option o : Option.values()) {
            if (o.is(word)) {
                return true;
            }
        }
        return false;
    }

    private static String help() {
        String h = "Funz.(sh|bat) " + name + " ...\n";
        for (Option o : Option.values()) {
            h = h + "\n" + o.toString();
        }
        return h;
    }

    private static String help(String word) {
        for (Option o : Option.values()) {
            if (o.is(word)) {
                return o.toString();
            }
        }
        return null;
    }

    public static void main(String[] args) {
        if (args.length == 1 || Option.HELP.is(args[1])) {
            System.out.println(help());
            System.exit(0);
        }

        //tic("options");
        String _model = null;
        String _get = null;
        File[] _input = null;
        File _print = null;
        int verb = -666;
        File _archiveDir = null;

        try {
            for (int i = 1; i < args.length; i++) {
                //System.err.print(args[i] + ": ");

                if (Option.MODEL.is(args[i])) {
                    if (_model != null) throw new Exception("[ERROR] Option "+Option.MODEL.key+" already set!");
                    _model = args[i + 1];
                    //System.err.println(_model);
                    i++;
                } else if (Option.INPUT_FILES.is(args[i])) {
                    if (_input != null) throw new Exception("[ERROR] Option "+Option.INPUT_FILES.key+" already set!");
                    List<File> files = new LinkedList<>();
                    int j = 1;
                    while (i + j < args.length && !isOption(args[i + j])) {
                        File f = new File(args[i + j]);
                        if (!f.exists()) {
                            System.err.println("[WARNING] Cannot access file " + f.getAbsolutePath());
                        }
                        files.add(new File(args[i + j]));
                        j++;
                    }
                    _input = files.toArray(new File[files.size()]);
                    //System.err.println(_input);
                    i += j - 1;
                } else if (Option.PRINT.is(args[i])) {
                    if (_print != null) throw new Exception("[ERROR] Option "+Option.PRINT.key+" already set!");
                    _print = new File(args[i + 1]);
                    //System.err.println(_print);
                    i++;
                } else if (Option.GET_EXPRESSION.is(args[i])) {
                    if (_get != null) throw new Exception("[ERROR] Option "+Option.GET_EXPRESSION.key+" already set!");
                    _get = args[i + 1];
                    //System.err.println(_get);
                    i++;
                } else if (Option.OUTPUT_DIR.is(args[i])) {
                    if (_archiveDir != null) throw new Exception("[ERROR] Option "+Option.OUTPUT_DIR.key+" already set!");
                    _archiveDir = new File(args[i + 1]);
                    //System.err.println(_archiveDir);
                    i++;
                } else if (Option.VERBOSITY.is(args[i])) {
                    if (verb != -666) throw new Exception("[ERROR] Option "+Option.VERBOSITY.key+" already set!");
                    verb = Integer.parseInt(args[i + 1]);
                    //System.err.println(verb);
                    i++;
                    //System.err.println("VERBOSITY "+verb);
                } else {
                    throw new Exception("[ERROR] Unknwon option " + args[i]);
                }
            }
            if (_print == null) _print = new File(name + ".csv");
            if (verb == -666) verb = 3;
            if (_archiveDir == null) _archiveDir = new File(".");
        } catch (Exception e) {
            System.err.println("[ERROR] failed to parse options: " + e.getMessage());
            System.err.println(help());
            System.exit(PARSE_ERROR);
        }

        //toc("options");        //toc("options");
        if (_input == null && _get==null) {
            System.err.println("[ERROR] Input files or Get expression not defined.\n" + help("--input_files") + "\n" + help("--get_expression"));
            System.exit(INPUT_FILE_ERROR);
        }

        //tic("setVerbosity");
        Funz_v1.setVerbosity(verb);
        //toc("setVerbosity");        //toc("setVerbosity");

        Map outs = null;
        try {
            if (_input!=null)
            //tic("compileVariables");
                outs = Utils.readOutputs(_model, _input, _archiveDir);
            //toc("compileVariables");
            
            if (_get!=null)
                outs = newMap(_get,Utils.readOutput(_get, _archiveDir));
        } catch (Exception e) {
            System.err.println("[ERROR] failed to READ: " + e.getMessage());
            if (verb>=10) e.printStackTrace();
            System.exit(READ_ERROR);
        }

        System.out.println("Read Output:\n" + MapToMDString(outs));

        if (_print.getName().endsWith(".xml")) {
            ASCII.saveFile(_print, MapToXMLString(outs, true));
        } else if (_print.getName().endsWith(".json")) {
            ASCII.saveFile(_print, MapToJSONString(outs));
        } else {
            ASCII.saveFile(_print, MapToCSVString(outs));
        }

        System.exit(0);
    }
}
