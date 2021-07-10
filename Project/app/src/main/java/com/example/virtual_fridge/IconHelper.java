package com.example.virtual_fridge;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// class to try to get most appropriate icon for an item based on its name and type
public class IconHelper {

    private final Context context;

    // constructor
    public IconHelper(Context context) {
        this.context = context;
    }

    // method to get icon based on name and type.
    public int getIcon(String itemName, String itemType) {
        int imageId;

        if (itemType == null) {
            itemType = "Other";
        }

        String[] filenamesForName = context.getResources().getStringArray(R.array.icon_filenames_for_name);
        List<String> nameSplit = Arrays.asList(itemName.trim().toLowerCase().split("\\s|:|-|_|,|;|\\(|\\)|\\|/|\\[|]|&|\"|'"));

        ArrayList<String> potentialMatches = new ArrayList<>();
        String bestMatch = "";

        // find potentially matching icons
        for (String s : filenamesForName) {
            List<String> filename = Arrays.asList(s.split("_"));

            for (int i = 0; i < nameSplit.size(); i++) {
                if (filename.contains(nameSplit.get(i))) {
                    // CHECK 1:
                    // for duplicate - that's the best match (i.e. it matches the most words as possible)
                    if (potentialMatches.contains(s)) {
                        bestMatch = s;
                    }

                    potentialMatches.add(s);
                }
            }
        }

        // DEFAULT:
        // if there are no potential matches, use type
        if (potentialMatches.size() == 0) {
            String type = itemType.replace(" ", "_");
            type = type.replace("/", "_").toLowerCase();
            imageId = context.getResources().getIdentifier(type, "drawable", context.getPackageName());
        } else {

            // CHECK 2:
            // if there's only one potential match, then that's the best match
            if (potentialMatches.size() == 1) {
                bestMatch = potentialMatches.get(0);
            } else {
                if (bestMatch.equals("")) {
                    for (int i = 0; i < potentialMatches.size(); i++) {

                        // CHECK 3:
                        // if there's an exact one-word match (or plural type)
                        if (nameSplit.get(0).equals(potentialMatches.get(i))) {
                            bestMatch = potentialMatches.get(i);
                            break;

                            // CHECK 4:
                            // if there's an exact one-word match, but where plural is included in potential match
                        } else if (potentialMatches.get(i).split("_")[0].contains(nameSplit.get(0)) && potentialMatches.get(i).split("_")[1].contains(nameSplit.get(0))) {

                            // CHECK 4.1:
                            // if the last word of the item name is in one of the potential matches, use that one
                            if (potentialMatches.size() > 1) {
                                for (int j = 0; j < potentialMatches.size(); j++) {
                                    if (Arrays.asList(potentialMatches.get(j).split("_")).contains(nameSplit.get(nameSplit.size() - 1))) {
                                        bestMatch = potentialMatches.get(j);
                                        break;
                                    }
                                }

                                // CHECK 4.2:
                                // if not above, choose first in potential match list
                            } else {
                                bestMatch = potentialMatches.get(i);
                            }
                            break;
                        }
                    }
                }
            }

            // CHECK 5:
            // if final word in name (e.g. "juice" in "orange juice") matches with a potential match
            // more useful than if it were to match "orange juice" with icon for orange
            if (bestMatch.equals("")) {
                for (int i = 0; i < potentialMatches.size(); i++) {
                    if (Arrays.asList(potentialMatches.get(i).split("_")).contains(nameSplit.get(nameSplit.size() - 1))) {
                        bestMatch = potentialMatches.get(i);
                        break;
                    }
                }
            }

            // CHECK 6:
            // if still no good match, use 1st of potential matches
            if (bestMatch.equals("")) {
                bestMatch = potentialMatches.get(0);
                imageId = context.getResources().getIdentifier(bestMatch, "drawable", context.getPackageName());

            } else {
                imageId = context.getResources().getIdentifier(bestMatch, "drawable", context.getPackageName());
            }
        }
        return imageId;
    }
}
