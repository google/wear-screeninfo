package net.waynepiekarski.screeninfo;

import android.view.View;
import android.view.ViewGroup;

public class ListenerHelper {

    // The only way to listen to any click that occurs on the display is to add a listener to every
    // view recursively. Just attaching to the top level does nothing if there are child views.
    public static void recursiveSetOnClickListener (ViewGroup viewGroup, View.OnClickListener handler) {
        View v;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            v = viewGroup.getChildAt(i);
            if (v instanceof ViewGroup) {
                recursiveSetOnClickListener((ViewGroup) v, handler);
            } else {
                v.setOnClickListener(handler);
            }
        }
    }

}
