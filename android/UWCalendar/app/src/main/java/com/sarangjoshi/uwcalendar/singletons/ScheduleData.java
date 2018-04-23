package com.sarangjoshi.uwcalendar.singletons;

import com.sarangjoshi.uwcalendar.models.QuarterName;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * TODO: add class comment
 * Singleton
 *
 * @author Sarang Joshi
 */
public class ScheduleData {
    public static final String CLASS_ID_KEY = "classId";
    private static ScheduleData ourInstance = new ScheduleData();

    public static ScheduleData getInstance() {
        return ourInstance;
    }

    // TODO get this from an online API somehow
    private ScheduleData() {
        quarterInfo = new TreeMap<>();
        quarterInfo.put(new QuarterName("wi16"), new String[]{"2016-01-04", "2016-03-12"});
        quarterInfo.put(new QuarterName("sp16"), new String[]{"2016-03-28", "2016-06-04"});
        quarterInfo.put(new QuarterName("au16"), new String[]{"2016-09-28", "2016-12-09"});
        quarterInfo.put(new QuarterName("sp17"), new String[]{"2017-03-27", "2017-06-02"});
        quarterInfo.put(new QuarterName("au17"), new String[]{"2017-09-27", "2017-12-08"});
        quarterInfo.put(new QuarterName("wi18"), new String[]{"2018-01-03", "2018-03-09"});
        quarterInfo.put(new QuarterName("sp18"), new String[]{"2018-03-26", "2018-06-01"});
    }

    // TODO: change String[] to class
    private Map<QuarterName, String[]> quarterInfo;

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
        return quarterInfo.get(new QuarterName(qtr));
    }

    /**
     * Returns the latest quarter
     */
    public String getCurrentQuarter() {
        return getQuarters()[quarterInfo.size() - 1];
    }

    public String[] getQuarters() {
        Set<String> quarterNames = new TreeSet<>();
        for (QuarterName q : quarterInfo.keySet()) {
            quarterNames.add(q.name);
        }
        return quarterNames.toArray(new String[quarterInfo.size()]);
        //Arrays.copyOf(quarterCodes, quarterCodes.length);
    }
}
