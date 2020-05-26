package com.stamanker.socket;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    public static final String DATE_TIME = "yyyy-MM-dd_HH-mm-ss-SS";
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_TIME);

    public static String getCurrentDateTime() {
        return simpleDateFormat.format(new Date());
    }

}
