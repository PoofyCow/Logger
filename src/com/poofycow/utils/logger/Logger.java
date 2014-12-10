package com.poofycow.utils.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
/**
 * Logs to specified logfiles
 * 
 * @author Poofy Cow
 * @version 2.0
 * @since 2.0
 * @date Nov 4, 2014
 */
public class Logger {
    private final static String PREFIX = "";
    private static Logger       instance;
    
    /**
     * Logs a message with the given loglevel
     * 
     * @param lvl
     *            The loglevel
     * @param msg
     *            The message
     */
    public static void log(LogLevel lvl, String tag, String msg) {
        if (instance == null)
            instance = new Logger();
        
        instance.actualLog(lvl, tag, msg);
    }
    
    /**
     * Logs a message with the given loglevel and the stacktrace of the given
     * exception
     * 
     * @param lvl
     *            The loglevel
     * @param msg
     *            The message
     * @param e
     *            The exception
     */
    public static void log(LogLevel lvl, String tag, String msg, Exception e) {
        if (instance == null)
            instance = new Logger();
        
        instance.actualLog(lvl, tag, msg, e);
    }
    
    /**
     * Logs the given loglevel and the stacktrace of the given exception
     * 
     * @param lvl
     *            The loglevel
     * @param e
     *            The exception
     */
    public static void log(LogLevel lvl, String tag, Exception e) {
        if (instance == null)
            instance = new Logger();
        
        instance.actualLog(lvl, tag, e.getMessage(), e);
    }
    
    private HashMap<LogLevel, List<LogData>> streams;
    private boolean                          console;
    private boolean                          syslog;
    private DateFormat                       format;
    private LogLevel                         consoleMin;
    private LogLevel                         consoleMax;
    
    /**
     * Constructor
     */
    private Logger() {
        // DEBUG:FATAL:log/all.log:1
        
        streams = new HashMap<LogLevel, List<LogData>>();
        
        console = true;
        syslog = false;
        
        String consoleLevels = "DEBUG:FATAL";
        
        String[] lvls = consoleLevels.split(":");
        consoleMin = LogLevel.valueOf(lvls[0]);
        consoleMax = LogLevel.valueOf(lvls[1]);
        
        String filestring = "";
        String[] files = filestring.split(",");
        
        for (String file : files) {
            if (file.length() <= 0)
                continue;
            
            String[] sFile = file.split(":");
            
            LogLevel lvl = LogLevel.valueOf(sFile[0]);
            LogLevel maxLvl = LogLevel.valueOf(sFile[1]);
            
            boolean fancy = false;
            if (sFile.length >= 4 && sFile[3].equals("1")) {
                fancy = true;
            }
            
            try {
                this.addFile(lvl, maxLvl, sFile[2], fancy);
            } catch (IOException e) {
                System.out.println("Could not load file: " + sFile[2]);
            }
            
        }
        
        format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    }
    
    /**
     * Adds a file to log to
     * 
     * @param minLevel
     *            The minimum log level
     * @param path
     *            The path of the file to write to
     * @throws IOException
     *             Thrown when the file could not be created/read
     */
    public void addFile(LogLevel minLevel, LogLevel maxLevel, String path, boolean fancy) throws IOException {
        File file = new File(path);
        file.getParentFile().mkdirs();
        
        LogData data = new LogData();
        data.writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
        data.fancy = fancy;
        data.maxLevel = maxLevel;
        
        if (!streams.containsKey(minLevel))
            this.streams.put(minLevel, new ArrayList<LogData>());
        
        this.streams.get(minLevel).add(data);
    }
    
    /**
     * Adds a stream to log to
     * 
     * @param minLevel
     *            The minimum log level
     * @param stream
     *            The stream to log to
     * @throws IOException
     *             Thrown when the file could not be created/read
     */
    public void addStream(LogLevel minLevel, LogLevel maxLevel, OutputStream stream, boolean fancy) {
        PrintWriter writer = new PrintWriter(stream);
        
        LogData data = new LogData();
        data.writer = writer;
        data.fancy = fancy;
        data.maxLevel = maxLevel;
        
        if (!streams.containsKey(minLevel))
            this.streams.put(minLevel, new ArrayList<LogData>());
        
        this.streams.get(minLevel).add(data);
    }
    
    /**
     * Adds a stream to log to
     * 
     * @param minLevel
     *            The minimum log level
     * @param writer
     *            The printwriter to log to
     * @throws IOException
     *             Thrown when the file could not be created/read
     */
    public void addStream(LogLevel minLevel, LogLevel maxLevel, PrintWriter writer, boolean fancy) {
        LogData data = new LogData();
        data.writer = writer;
        data.fancy = fancy;
        data.maxLevel = maxLevel;
        
        if (!streams.containsKey(minLevel))
            this.streams.put(minLevel, new ArrayList<LogData>());
        
        this.streams.get(minLevel).add(data);
    }
    
    private void actualLog(LogLevel lvl, String tag, String msg) {
        
        String log = "[" + format.format(new Date()) + "]\t" + ((syslog) ? "[" + PREFIX + "]\t" : "") + "[" + lvl.toString() + "]\t[" + tag + "]\t" + msg;
        
        for (int i = lvl.ordinal(); i < LogLevel.values().length; i++) {
            if (streams.containsKey(LogLevel.values()[i])) {
                List<LogData> listData = streams.get(LogLevel.values()[i]);
                
                for (LogData data : listData) {
                    
                    if (data.maxLevel.ordinal() <= lvl.ordinal()) {
                        PrintWriter writer = data.writer;
                        if (!data.fancy)
                            writer.write(log + "\n");
                        else
                            writer.write(lvl.getConsoleColor() + log + "\u001B[0m\n");
                        writer.flush();
                    }
                }
            }
        }
        
        if (console && lvl.ordinal() <= this.consoleMin.ordinal() && lvl.ordinal() >= this.consoleMax.ordinal()) {
            if (syslog || System.getProperty("os.name").toLowerCase().contains("win") || System.getenv("IDE") != null) {
                System.out.println(((syslog) ? "[" + PREFIX + "]" : "") + log);
            } else {
                System.out.println(lvl.getConsoleColor() + log + "\u001B[0m");
            }
        }
    }
    
    private void actualLog(LogLevel lvl, String tag, String msg, Exception e) {
        String log = "[" + format.format(new Date()) + "]\t" + ((syslog) ? "[" + PREFIX + "]\t" : "") + "[" + lvl.toString() + "]\t[" + tag + "]\t" + msg;
        
        for (int i = lvl.ordinal(); i < LogLevel.values().length; i++) {
            if (streams.containsKey(LogLevel.values()[i])) {
                List<LogData> listData = streams.get(LogLevel.values()[i]);
                
                for (LogData data : listData) {
                    
                    if (data.maxLevel.ordinal() <= lvl.ordinal()) {
                        PrintWriter writer = data.writer;
                        if (!data.fancy)
                            writer.write(log + "\n");
                        else
                            writer.write(lvl.getConsoleColor() + log + "\n");
                        e.printStackTrace(writer);
                        if (data.fancy)
                            writer.write("\u001B[0m");
                        writer.flush();
                    }
                }
            }
        }
        
        if (console && lvl.ordinal() <= this.consoleMin.ordinal() && lvl.ordinal() >= this.consoleMax.ordinal()) {
            if (syslog || System.getProperty("os.name").toLowerCase().contains("win") || System.getenv("IDE") != null) {
                System.out.println(log);
            } else {
                System.out.println(lvl.getConsoleColor() + log + "\u001B[0m");
            }
            e.printStackTrace(System.err);
        }
    }
    
    private class LogData {
        protected LogLevel    maxLevel;
        protected PrintWriter writer;
        protected boolean     fancy;
    }
    
}
