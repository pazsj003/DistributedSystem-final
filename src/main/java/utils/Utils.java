package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    private static boolean initialized = false;
    private static Logger logger;

    private static void initLogger() {
        String roleName = "CentralServer";
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        // the log file will be stored as CentralServer/2020/04/21.log
        System.setProperty("logfile.name", roleName + "/" + dateFormat.format(date) + ".log");

        logger = LoggerFactory.getLogger(Utils.class);
        initialized = true;
    }

    public static void timedLog(String msg) {
        if (!initialized) {
            initLogger();
        }
        logger.info(msg);
    }

    public static void errorLog(String msg) {
        if (!initialized) {
            initLogger();
        }
        logger.error(msg);
    }
}
