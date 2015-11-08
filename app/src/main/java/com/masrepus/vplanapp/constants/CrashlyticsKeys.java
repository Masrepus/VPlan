package com.masrepus.vplanapp.constants;

/**
 * Created by samuel on 22.05.15.
 */
public class CrashlyticsKeys {

    public static final String KEY_VPLAN_MODE = "vplanMode";
    public static final String KEY_APP_MODE = "appMode";
    public static final String EVENT_BG_DOWNLOAD = "Hintergrundupdate";
    public static final String EVENT_REFRESH_VPLAN = "man. Aktualisierung (Vertretungen)";
    public static final String EVENT_REFRESH_EXAMS = "Aktualisierung (Tests)";
    public static final String EVENT_DELETED_OLD_DATA = "Alter SharedPrefs Key gelöscht";
    public static final String EVENT_CUSTOM_CLASS = "Eigene Klasse hinzugefügt";
    public static final String KEY_PREF_KEY = "Key";
    public static final String KEY_USES_FILTER = "Filter";
    public static final String EVENT_WEAR_APP_OPENED = "Wear-App geöffnet";

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
