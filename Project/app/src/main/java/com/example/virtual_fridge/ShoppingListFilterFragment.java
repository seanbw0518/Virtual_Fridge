package com.example.virtual_fridge;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class ShoppingListFilterFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // initializing all views
        View view = inflater.inflate(R.layout.shopping_list_filter_fragment, container, false);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.shopping_filters_toolbar);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, ShoppingListFragment.class, null)
                .commit());

        ChipGroup chipGroupType = (ChipGroup) view.findViewById(R.id.shopping_filters_type);

        Button applyButton = (Button) view.findViewById(R.id.shopping_filters_save);

        // setting applied filters to chips
        String[] types = getResources().getStringArray(R.array.item_types);

        for (String type : types) {
            Chip newChip = (Chip) inflater.inflate(R.layout.single_chip, chipGroupType, false);
            newChip.setText(type);
            newChip.setCheckedIconVisible(true);
            newChip.setChecked(true);
            newChip.setCheckable(true);
            newChip.setClickable(true);
            newChip.setCheckedIconResource(R.drawable.check_icon);
            chipGroupType.addView(newChip);
        }

        // type chips
        // if no type filter applied, set all chips to checked & add them all to filter
        if (MainActivity.shoppingTypeWhitelist.isEmpty()) {
            for (int i = 0; i < chipGroupType.getChildCount(); i++) {

                Chip chip = (Chip) chipGroupType.getChildAt(i);
                chip.setChecked(true);
            }
        } else {
            for (int i = 0; i < chipGroupType.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupType.getChildAt(i);

                chip.setChecked(MainActivity.shoppingTypeWhitelist.contains(String.valueOf(chip.getText())));
            }
        }

        // on click -> apply filters and go back to list fragment
        applyButton.setOnClickListener(v -> {

            MainActivity.shoppingTypeWhitelist.clear();

            for (int i = 0; i < chipGroupType.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupType.getChildAt(i);
                if (chip.isChecked()) {
                    MainActivity.shoppingTypeWhitelist.add(String.valueOf(chip.getText()));
                } else {
                    MainActivity.shoppingTypeWhitelist.remove(String.valueOf(chip.getText()));
                }
            }

            getParentFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container, ShoppingListFragment.class, null)
                    .commit();
            Toast.makeText(getContext(), R.string.filtered, Toast.LENGTH_SHORT).show();

        });

        return view;
    }
}
