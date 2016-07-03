package com.sarangjoshi.uwcalendar.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    private ScheduleData() {
        quarterInfo = new HashMap<>();
        quarterInfo.put("wi16", new String[]{"2016-01-04", "2016-03-12"});
        quarterInfo.put("sp16", new String[]{"2016-03-28", "2016-06-04"});
    }

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
     * TODO implement
     */
    public String getCurrentQuarter() {
        return "sp16";
    }

    public Set<String> getQuarters() {
        return Collections.unmodifiableSet(quarterInfo.keySet());
    }
}
