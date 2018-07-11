package ru.neoflex.vak.fiasParser.fiasApi;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

class Utils {
    private Utils() {
    }

    static String getTableName(String text) {
        return text.replaceFirst("\\d{0,}\\.[^.]+$", "");
    }

    static String getExt(String fileName) {
        return fileName.replaceAll("^.*\\.(.*)$", "$1");
    }

    static String getTimeFromDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        SimpleDateFormat format = new SimpleDateFormat();
        format.applyPattern("dd.MM.yyyy");
        String s = format.format(cal.getTime());
        return format.format(cal.getTime());
    }

    static String getTimeFromString(String date) {
        DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy", Locale.ENGLISH);
        Date result = null;
        try {
            result = df.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return getTimeFromDate(result);
    }
}
