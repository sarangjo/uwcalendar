package com.sarangjoshi.uwcalendar.singletons;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO: add class comment
 * Singleton
 *
 * @author Sarang Joshi
 */
public class ScheduleData {
    private static ScheduleData ourInstance = new ScheduleData();

    public static ScheduleData getInstance() {
        return ourInstance;
    }

    // TODO get this from an onlnie API somehow
    private ScheduleData() {
        quarterCodes = new String[]{"wi16", "sp16", "au16"};
        quarterInfo = new HashMap<>();
        quarterInfo.put(quarterCodes[0], new String[]{"2016-01-04", "2016-03-12"});
        quarterInfo.put(quarterCodes[1], new String[]{"2016-03-28", "2016-06-04"});
        quarterInfo.put(quarterCodes[2], new String[]{"2016-09-28", "2016-12-09"});
    }

    private String[] quarterCodes;

    // TODO: change String[] to class
    private Map<String, String[]> quarterInfo;

    // M, T, W, Th, F
    public static final int[] DAYS_MAP = {1, 2, 4, 8, 16};

    /**
     * Gets the quarter information given its code.
     *
     * @param qtr a code for the quarter, of the form "wi16" or "au09"
     * @return a two-element String array, with the first element being the start date and the
     * second element being the end date
     */
    public String[] getQuarterInfo(String qtr) {
        return quarterInfo.get(qtr);
    }

    /**
     * Returns the latest quarter
     */
    public String getCurrentQuarter() {
        return getQuarters()[quarterCodes.length - 1];
    }

    public String[] getQuarters() {
        return Arrays.copyOf(quarterCodes, quarterCodes.length);
    }
}
