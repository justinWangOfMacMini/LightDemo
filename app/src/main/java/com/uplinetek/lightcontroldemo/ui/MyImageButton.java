package com.uplinetek.lightcontroldemo.ui;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;

/**
 * Created by cooper on 2016/11/15.
 */

public class MyImageButton extends AppCompatImageButton {



    private int buttonStatus;

    public MyImageButton(Context context) {
        super(context);
        this.setBackgroundColor(Color.TRANSPARENT);
    }

    public MyImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setBackgroundColor(Color.TRANSPARENT);
    }

    public int getButtonStatus() {
        return buttonStatus;
    }

    public void setButtonStatus(int buttonStatus) {
        this.buttonStatus = buttonStatus;

    }

}
