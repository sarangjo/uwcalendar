package com.sarangjoshi.uwcalendar.content;

import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 */

public class Day {
    private List<Marker> markers;

    public Day() {
        this.markers = new ArrayList<>();
    }

    public static Day valueOf(DataSnapshot snapshot) {
        Day d = new Day();

        for (DataSnapshot marker : snapshot.getChildren()) {
            d.markers.add(Marker.valueOf(marker));
        }

        return d;
    }

    public List<Marker> getMarkers() {
        return markers;
    }
}
