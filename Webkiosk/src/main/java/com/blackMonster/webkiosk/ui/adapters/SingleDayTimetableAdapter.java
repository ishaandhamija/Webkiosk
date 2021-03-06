package com.blackMonster.webkiosk.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blackMonster.webkiosk.SharedPrefs.RefreshDBPrefs;
import com.blackMonster.webkiosk.controller.Timetable.TimetableUtils;
import com.blackMonster.webkiosk.controller.model.SingleClass;
import com.blackMonster.webkiosk.ui.TimeLTP;
import com.blackMonster.webkiosk.ui.UIUtils;
import com.blackMonster.webkioskApp.R;

import java.util.Calendar;
import java.util.List;

/**
 * Created by akshansh on 26/07/15.
 */
public class SingleDayTimetableAdapter extends ArrayAdapter<SingleClass> {
    int currentDay;                                //Day whose data is to be displayed(Mon, Tue etc.)
    Context context;
    List<SingleClass> values;                      //Data to be displayed.

    public SingleDayTimetableAdapter(int currentDay, List<SingleClass> objects, Context context) {
        super(context, R.layout.activity_timetable_row, objects);
        this.currentDay = currentDay;
        this.context = context;
        values = objects;
    }

    public void updateDataSet(List<SingleClass> list) {
        values = list;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return values.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.activity_timetable_row,
                parent, false);

        SingleClass singleClass = values.get(position);

        setProgressCircle(singleClass, rowView);
        ((TextView) rowView.findViewById(R.id.timetable_Sub_name))
                .setText(singleClass.getSubjectName());
        ((TextView) rowView.findViewById(R.id.timetable_venue))
                .setText(singleClass.getVenue());
        ((TextView) rowView.findViewById(R.id.timetable_class_time))
                .setText(TimetableUtils.getFormattedTime(singleClass
                        .getTime()));


        //Shows recently updated tag.
        if (!(RefreshDBPrefs.getRecentlyUpdatedTagVisibility(context) && singleClass.isAtndModified() == 1))
            ((TextView) rowView.findViewById(R.id.timetable_updated_tag))
                    .setVisibility(View.GONE);

        highlightCurrentClass(singleClass, rowView);


        ProgressBar pb = ((ProgressBar) rowView
                .findViewById(R.id.timetable_attendence_progressBar));
        UIUtils.setProgressBarColor(pb,
                singleClass.getOverallAttendence(), context);
        if (singleClass.getOverallAttendence() == -1) {
            ((TextView) rowView.findViewById(R.id.timetable_attendence))
                    .setText(UIUtils.ATND_NA);
            pb.setProgress(0);
        } else {
            ((TextView) rowView.findViewById(R.id.timetable_attendence))
                    .setText(singleClass.getOverallAttendence().toString()
                            + "%");
            pb.setProgress(singleClass.getOverallAttendence());
        }

        return rowView;

    }

    //Sets time and type("L","T","P") of clock on left of every class.
    private void setProgressCircle(SingleClass singleClass, View view) {
        int t2;
        if (TimetableUtils.isOfTwoHr(singleClass.getClassType(),
                singleClass.getSubjectCode()))
            t2 = singleClass.getTime() + 2;
        else
            t2 = singleClass.getTime() + 1;

        ((TimeLTP) view.findViewById(R.id.timetable_TimeLTP)).setParams(
                singleClass.getTime(), t2, singleClass.getClassType());
    }

    //Highlight ongoing class.
    private void highlightCurrentClass(SingleClass singleClass, View rowView) {
        Calendar calender = Calendar.getInstance();
        boolean isCurrentClass = (calender.get(Calendar.HOUR_OF_DAY) == singleClass
                .getTime())
                || (TimetableUtils.isOfTwoHr(singleClass.getClassType(),
                singleClass.getSubjectCode()) && calender
                .get(Calendar.HOUR_OF_DAY) == singleClass.getTime() + 1);

        if (calender.get(Calendar.DAY_OF_WEEK) == currentDay
                && isCurrentClass) {

            ((RelativeLayout) rowView.findViewById(R.id.timetable_row))
                    .setBackgroundColor(Color.rgb(216, 216, 216));
        }
    }

}
