package com.sarangjoshi.uwcalendar.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.sarangjoshi.uwcalendar.content.Day;
import com.sarangjoshi.uwcalendar.content.Marker;
import com.sarangjoshi.uwcalendar.content.SingleClass;

import java.util.List;

/**
 * TODO: add class comment
 *
 * @author Sarang Joshi
 */
public class DayListAdapter extends BaseExpandableListAdapter {
    private Context mContext;
    private List<Day> mWeek;

    public DayListAdapter(Context c, List<Day> week) {
        this.mContext = c;
        this.mWeek = week;
    }

    @Override
    public int getGroupCount() {
        return mWeek.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mWeek.get(groupPosition).getMarkers().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mWeek.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mWeek.get(groupPosition).getMarkers().get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) this.mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(android.R.layout.simple_list_item_1, null);
        }

        TextView groupHeader = (TextView) convertView;
        groupHeader.setTypeface(null, Typeface.BOLD);
        groupHeader.setText(SingleClass.RECURRENCE_DAYS[groupPosition]);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Marker marker = (Marker) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) this.mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(android.R.layout.simple_list_item_1, null);
        }

        TextView childView = (TextView) convertView;
        childView.setText(marker.toString());

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true; // TODO ?
    }
}
