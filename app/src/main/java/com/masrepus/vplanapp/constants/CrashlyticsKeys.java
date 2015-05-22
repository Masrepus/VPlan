package com.masrepus.vplanapp.constants;

/**
 * Created by samuel on 22.05.15.
 */
public class CrashlyticsKeys {

    public static final String KEY_VPLAN_MODE = "vplanMode";
    public static final String KEY_APP_MODE = "appMode";

    public static String parseVplanMode(int vplanmode) {

        String modeString = "--";

        switch (vplanmode) {

            case VplanModes.UINFO:
                modeString = "Unterstufe";
                break;
            case VplanModes.MINFO:
                modeString = "Mittelstufe";
                break;
            case VplanModes.OINFO:
                modeString = "Oberstufe";
                break;
        }

        return modeString;
    }

    public static String parseAppMode(int appmode) {

        String modeString = "--";

        switch (appmode) {

            case AppModes.TESTS:
                modeString = "Tests";
                break;
            case AppModes.TIMETABLE:
                modeString = "Timetable";
                break;
            case AppModes.VPLAN:
                modeString = "VPlan";
                break;
        }

        return modeString;
    }
}