package hu.narnik.myapplication;

import java.util.ArrayList;
import java.util.List;

public class AppState {

    public static String currentNoteType = ""; // "PRIVÁT" vagy "CSOPORT"
    public static String currentGroupId = null;
    public static List<String> products = new ArrayList<>();

    public static boolean isGroupView = false;
}