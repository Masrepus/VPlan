package com.masrepus.vplanapp;

import android.content.Context;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by samuel on 29.12.14.
 */
public class VplanListItemLayout extends RelativeLayout implements WearableListView.OnCenterProximityListener {

    private final float fadedTextAlpha;
    private final int fadedCircleColor;
    private final int chosenCircleColor;

    private CircledImageView circle;
    private TextView hour;
    private TextView grade;
    private TextView status;
    private RelativeLayout circleLayout;

    public VplanListItemLayout(Context context) {
        this(context, null);
    }

    public VplanListItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VplanListItemLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        fadedTextAlpha = 50 / 100f;
        fadedCircleColor = getResources().getColor(R.color.grey);
        chosenCircleColor = getResources().getColor(R.color.blue);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        //get the view references from the layout
        circle = (CircledImageView) findViewById(R.id.circle);
        hour = (TextView) findViewById(R.id.hour);
        grade = (TextView) findViewById(R.id.grade);
        status = (TextView) findViewById(R.id.status);
        circleLayout = (RelativeLayout) findViewById(R.id.circleLayout);
    }

    @Override
    public void onCenterPosition(boolean b) {
        hour.setAlpha(1f);
        grade.setAlpha(1f);
        status.setAlpha(1f);
        circle.setCircleColor(chosenCircleColor);
    }

    @Override
    public void onNonCenterPosition(boolean b) {
        circle.setCircleColor(fadedCircleColor);
        hour.setAlpha(fadedTextAlpha);
        grade.setAlpha(fadedTextAlpha);
        status.setAlpha(fadedTextAlpha);
    }
}
