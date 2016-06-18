package com.sarangjoshi.uwcalendar.content;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single Day in a schedule.
 */
public class Day {
    /**
     * A sorted list of the segments making up the day.
     */
    private List<Segment> segments;

    public Day() {
        segments = new ArrayList<>();
        segments.add(Segment.FREE_DAY);
    }

    /**
     * Adds a class to this day, appropriately reorganizing the existing segments in this day.
     * Assumes that the given class occurs on this day.
     */
    public void add(SingleClass c) {
        Segment seg = new Segment(c.getStart(), c.getEnd(), c);

        // Find the first segment that overlaps with seg
        Segment curr = null;
        int i;
        for (i = 0; i < segments.size(); ++i) {
            curr = segments.get(i);
            if (Segment.compare(curr.endHr, curr.endMin, seg.startHr, seg.startMin) > 0) {
                break;
            }
        }

        // Break off the part of curr that is before seg
        if (Segment.compare(curr.startHr, curr.startMin, seg.startHr, seg.startMin) != 0) {
            Segment beforeSeg = new Segment(curr.startHr, curr.startMin, seg.startHr, seg.startMin, null);
            beforeSeg.classes.addAll(curr.classes);
            segments.add(i, beforeSeg);

            segments.remove(curr);

            // Restore curr to be whatever is left over
            Segment beginningOfSeg = new Segment(seg.startHr, seg.startMin, curr.endHr, curr.endMin, null);
            beginningOfSeg.classes.addAll(curr.classes);
            segments.add(i + 1, beginningOfSeg);

            i++;
            curr = beginningOfSeg;
        }

        // Overlap all segments that are completely within seg
        while (Segment.compare(curr.endHr, curr.endMin, seg.endHr, seg.endMin) <= 0) {
            // Don't actually split any times, just add the new class
            curr.classes.add(c);

            ++i;
            curr = segments.get(i);
        }

        if (Segment.compare(curr.startHr, curr.startMin, seg.endHr, seg.endMin) != 0) {
            // What overlaps with seg
            Segment endOfSeg = new Segment(curr.startHr, curr.startMin, seg.endHr, seg.endMin, null);
            endOfSeg.classes.addAll(seg.classes);
            segments.add(i, endOfSeg);

            segments.remove(curr);

            // What doesn't overlap
            Segment afterSeg = new Segment(seg.endHr, seg.endMin, curr.endHr, curr.endMin, null);
            afterSeg.classes.addAll(curr.classes);
            segments.add(i + 1, afterSeg);
        }

        // TODO: Ensure that the segments are always sorted
    }

    public List<Segment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    public String toString() {
        return segments.toString();
    }

    /**
     * Combines the given day with this day.
     */
    public void combine(Day otherDay) {
        for (Segment seg : otherDay.getSegments()) {
            if (!seg.classes.isEmpty()) {
                for (SingleClass c : seg.classes) add(c);
            }
        }
    }

    public static Day valueOf(DataSnapshot snapshot) {
        Day d = new Day();
        d.segments.clear();

        for (DataSnapshot seg : snapshot.getChildren()) {
            d.segments.add(Segment.valueOf(seg));
        }

        return d;
    }
}
