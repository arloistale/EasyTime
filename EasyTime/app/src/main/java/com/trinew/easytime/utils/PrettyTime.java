package com.trinew.easytime.utils;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jonathanlu on 9/8/15.
 */

public class PrettyTime {

    public final static long ONE_SECOND = 1000;
    public final static long ONE_MINUTE = ONE_SECOND * 60;
    public final static long ONE_HOUR = ONE_MINUTE * 60;

    private PrettyTime() {}

    // duration must be in hours time value
    public static String getPrettyDuration(long duration) {
        String result;

        float durationHours = (float) duration / 3600000f;

        //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        //dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-00:02"));
/*
        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(hours);
        NumberFormat hoursFormat = new DecimalFormat("0");
        NumberFormat minutesFormat = new DecimalFormat("00");
        String hoursStr = hoursFormat.format(hours);
        String minutesStr = minutesFormat.format(minutes);

        result = hoursStr + "h " + minutesStr + "m";
*/

        if (durationHours >= 0.1f) {
            if((int) durationHours == durationHours) {
                result = (int) durationHours + "h";
            } else {
                DecimalFormat resultFormat = new DecimalFormat("0.0");
                resultFormat.setRoundingMode(RoundingMode.DOWN);
                String resultStr = resultFormat.format(durationHours);
                result = resultStr + "h";
            }
        } else {
            result = "< 0.1h";
        }

        return result;
    }

    public static String getPrettyFullDate(Date date) {
        return new SimpleDateFormat("EEEE, MMMM d").format(date);
    }

    // returns a date in Jan 12 string format
    public static String getPrettyShortDate(Date date) {
        return new SimpleDateFormat("MMM d").format(date);
    }

    // returns a date in 9:00 AM string format
    public static String getPrettyTime(Date date) {
        return new SimpleDateFormat("h:mm a").format(date);
    }
}