package tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 * @author Usuario
 */
public class LoggGen {

//    protected static String defaultLogFile = "C:/demo/MyLogFile.log";
//    static Logger logger = Logger.getLogger("MyLog");
//    static FileHandler fh;
    static Logger logger;
    public Handler fileHandler;
    Formatter plainText;

    private LoggGen() throws IOException {
        //instance the logger
        logger = Logger.getLogger(LoggGen.class.getName());
        Path defaultPath = Paths.get("./Log/LogApp.log");
        if(!Files.exists(defaultPath.getParent().toAbsolutePath())){
            Files.createDirectories(defaultPath.getParent().toAbsolutePath());
        }
        fileHandler = new FileHandler(defaultPath.toAbsolutePath().toString(), true);
        //instance formatter, set formatting, and handler
        fileHandler.setFormatter(new MyFormatter());
        logger.addHandler(fileHandler);

    }

    private static Logger getLogger() {
        if (logger == null) {
            try {
                new LoggGen();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return logger;
    }

    public static void log(LogRecord record){
        getLogger().log(record);
    }
}
