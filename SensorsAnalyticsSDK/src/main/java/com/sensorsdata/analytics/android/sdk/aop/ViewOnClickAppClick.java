package com.sensorsdata.analytics.android.sdk.aop;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataIgnoreTrackOnClick;
import com.sensorsdata.analytics.android.sdk.util.AopUtil;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.json.JSONObject;

import java.lang.reflect.Method;

/**
 * Created by 王灼洲 on 2017/8/26
 */

public class ViewOnClickAppClick {
    private final static String TAG = "ViewOnClickAppClick";

    public static void onAppClick(JoinPoint joinPoint) {
        try {
            //基本校验
            if (joinPoint.getArgs() == null || joinPoint.getArgs().length != 1) {
                return;
            }

            //关闭 AutoTrack
            if (!SensorsDataAPI.sharedInstance().isAutoTrackEnabled()) {
                return;
            }

            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

            //忽略 onClick
            Method method = methodSignature.getMethod();
            SensorsDataIgnoreTrackOnClick trackEvent = method.getAnnotation(SensorsDataIgnoreTrackOnClick.class);
            if (trackEvent != null) {
                return;
            }

            //$AppClick 被过滤
            if (SensorsDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取被点击的 View
            View view = (View) joinPoint.getArgs()[0];
            if (view == null) {
                return;
            }

            //获取所在的 Context
            Context context = view.getContext();
            if (context == null) {
//                        return;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, view);

            //Activity 被忽略
            if (activity != null) {
                if (SensorsDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            long currentOnClickTimestamp = System.currentTimeMillis();
            String tag = (String) view.getTag(R.id.sensors_analytics_tag_view_onclick_timestamp);
            if (!TextUtils.isEmpty(tag)) {
                try {
                    long lastOnClickTimestamp = Long.parseLong(tag);
                    if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                        SALog.i(TAG, "This onClick maybe extends from super, IGNORE");
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            view.setTag(R.id.sensors_analytics_tag_view_onclick_timestamp, String.valueOf(currentOnClickTimestamp));

            JSONObject properties = new JSONObject();

            //ViewId
            String idString = AopUtil.getViewId(view);
            if (!TextUtils.isEmpty(idString)) {
                properties.put(AopConstants.ELEMENT_ID, idString);
            }

            //Action
//                    properties.put(AopConstants.ELEMENT_ACTION, "onClick");

            //$screen_name & $title
            if (activity != null) {
                properties.put(AopConstants.SCREEN_NAME, activity.getClass().getCanonicalName());
                String activityTitle = AopUtil.getActivityTitle(activity);
                if (!TextUtils.isEmpty(activityTitle)) {
                    properties.put(AopConstants.TITLE, activityTitle);
                }
            }

            String viewType = view.getClass().getCanonicalName();
            CharSequence viewText = null;
            if (view instanceof CheckBox) { // CheckBox
                viewType = "CheckBox";
                CheckBox checkBox = (CheckBox) view;
                viewText = checkBox.getText();
//                        if (checkBox.isChecked()) {
//                            properties.put(AopConstants.ELEMENT_ACTION, "checked");
//                        } else {
//                            properties.put(AopConstants.ELEMENT_ACTION, "unchecked");
//                        }
            } else if (view instanceof SwitchCompat) {
                viewType = "SwitchCompat";
                SwitchCompat switchCompat = (SwitchCompat) view;
                boolean isChecked = switchCompat.isChecked();
                viewText = switchCompat.getTextOn();
//                        if (isChecked) {
//                            properties.put(AopConstants.ELEMENT_ACTION, "checked");
//                        } else {
//                            properties.put(AopConstants.ELEMENT_ACTION, "unchecked");
//                        }
            } else if (view instanceof RadioButton) { // RadioButton
                viewType = "RadioButton";
                RadioButton radioButton = (RadioButton) view;
                viewText = radioButton.getText();
//                        if (radioButton.isChecked()) {
//                            properties.put(AopConstants.ELEMENT_ACTION, "checked");
//                        } else {
//                            properties.put(AopConstants.ELEMENT_ACTION, "unchecked");
//                        }
            } else if (view instanceof ToggleButton) { // ToggleButton
                viewType = "ToggleButton";
                ToggleButton toggleButton = (ToggleButton) view;
                boolean isChecked = toggleButton.isChecked();
                if (isChecked) {
                    viewText = toggleButton.getTextOn();
//                            properties.put(AopConstants.ELEMENT_ACTION, "checked");
                } else {
                    viewText = toggleButton.getTextOff();
//                            properties.put(AopConstants.ELEMENT_ACTION, "unchecked");
                }
            } else if (view instanceof Button) { // Button
                viewType = "Button";
                Button button = (Button) view;
                viewText = button.getText();
            } else if (view instanceof CheckedTextView) { // CheckedTextView
                viewType = "CheckedTextView";
                CheckedTextView textView = (CheckedTextView) view;
                viewText = textView.getText();
//                        if (textView.isChecked()) {
//                            properties.put(AopConstants.ELEMENT_ACTION, "checked");
//                        } else {
//                            properties.put(AopConstants.ELEMENT_ACTION, "unchecked");
//                        }
            } else if (view instanceof TextView) { // TextView
                viewType = "TextView";
                TextView textView = (TextView) view;
                viewText = textView.getText();
            } else if (view instanceof ImageButton) { // ImageButton
                viewType = "ImageButton";
            } else if (view instanceof ImageView) { // ImageView
                viewType = "ImageView";
            } else if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.toString().substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText.toString());
            }

            //$element_type
            properties.put(AopConstants.ELEMENT_TYPE, viewType);

            //fragmentName
            AopUtil.getFragmentNameFromView(view, properties);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.sensors_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            SensorsDataAPI.sharedInstance().track(AopConstants.APP_CLICK_EVENT_NAME, properties);
        } catch (Exception e) {
            e.printStackTrace();
            SALog.i(TAG, "onViewClickMethod error: " + e.getMessage());
        }
    }
}
