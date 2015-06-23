// ---------------------------------------------------------------------
// Copyright 2015 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ---------------------------------------------------------------------

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
