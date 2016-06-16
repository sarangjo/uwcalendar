package com.sarangjoshi.uwcalendar;

import com.sarangjoshi.uwcalendar.content.*;
import com.sarangjoshi.uwcalendar.content.Quarter.*;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class QuarterTest {
    private static final SingleClass TEST1 = new SingleClass("Test 1", "Test Location 1", 1, "09:30", "10:20");
    private static final SingleClass TEST2 = new SingleClass("Test 2", "Test Location 2", 1, "10:30", "11:20");
    private static final SingleClass TEST3 = new SingleClass("Test 3", "Test Location 3", 1, "10:00", "11:00");

    @Test
    public void testSimpleAddition() {
        Day d = new Day();
        d.add(TEST1);
        List<Segment> segs = d.getSegments();
        assertEquals(segs.size(), 3);
        assertTrue(segs.get(0).getClasses().isEmpty());
        assertEquals(segs.get(0).endHr, 9);
        assertEquals(segs.get(0).endMin, 30);
        assertEquals(segs.get(1).startHr, 9);
        assertEquals(segs.get(1).startMin, 30);
        assertEquals(segs.get(1).endHr, 10);
        assertEquals(segs.get(1).endMin, 20);
        assertEquals(segs.get(2).startHr, 10);
        assertEquals(segs.get(2).startMin, 20);
    }

    @Test
    public void testSimpleOverlap() {
        Day d = new Day();
        d.add(TEST1);
        d.add(TEST3);
    }
}