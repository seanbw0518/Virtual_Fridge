package com.example.virtual_fridge;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class KitchenListFilterFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // initializing views
        View view = inflater.inflate(R.layout.kitchen_list_filter_fragment, container, false);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.kitchen_filters_toolbar);
        toolbar.setNavigationIcon(R.drawable.arrow_back);

        // setting up various chips and slider
        ChipGroup chipGroupType = (ChipGroup) view.findViewById(R.id.kitchen_filters_type);
        ChipGroup chipGroupLocation = (ChipGroup) view.findViewById(R.id.kitchen_filters_location);

        TextView expiryIndicatorText = view.findViewById(R.id.expiry_filter_indicator);
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.expiry_slider);

        Button applyButton = (Button) view.findViewById(R.id.kitchen_filters_save);

        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, KitchenListFragment.class, null)
                .commit());

        String[] types = getResources().getStringArray(R.array.item_types);

        for (String type : types) {
            Chip newChip = (Chip) inflater.inflate(R.layout.single_chip, chipGroupType, false);
            newChip.setText(type);
            newChip.setCheckedIconVisible(true);
            newChip.setCheckable(true);
            newChip.setClickable(true);
            newChip.setCheckedIconResource(R.drawable.check_icon);
            chipGroupType.addView(newChip);
        }

        // type chips
        // if no type filter applied, set all chips to checked & add them all to filter
        if (MainActivity.kitchenTypeWhitelist.isEmpty()) {
            for (int i = 0; i < chipGroupType.getChildCount(); i++) {

                Chip chip = (Chip) chipGroupType.getChildAt(i);
                chip.setChecked(true);
            }
        } else {
            for (int i = 0; i < chipGroupType.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupType.getChildAt(i);

                chip.setChecked(MainActivity.kitchenTypeWhitelist.contains(String.valueOf(chip.getText())));
            }
        }

        // location chips
        // if no location filter applied, set all chips to checked & add them all to filter
        if (MainActivity.kitchenLocationWhitelist.isEmpty()) {
            for (int i = 0; i < chipGroupLocation.getChildCount(); i++) {

                Chip chip = (Chip) chipGroupLocation.getChildAt(i);
                chip.setChecked(true);

            }
        } else {
            for (int i = 0; i < chipGroupLocation.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupLocation.getChildAt(i);

                chip.setChecked(MainActivity.kitchenLocationWhitelist.contains(String.valueOf(chip.getText())));
            }
        }

        // expiry slider
        switch (MainActivity.kitchenExpiryLimit) {
            case 31:
                expiryIndicatorText.setText(R.string.expiry_gt_30);
                seekBar.setProgress(MainActivity.kitchenExpiryLimit);
                break;
            case 0:
                expiryIndicatorText.setText(R.string.expiry_0);
                seekBar.setProgress(MainActivity.kitchenExpiryLimit);
                break;
            case -1:
                expiryIndicatorText.setText(R.string.expiry_gt_30);
                seekBar.setProgress(31);
                break;
            default:
                expiryIndicatorText.setText(MainActivity.kitchenExpiryLimit + " days");
                seekBar.setProgress(MainActivity.kitchenExpiryLimit);
                break;
        }

        // on slide -> set indicator text & change expiry limit
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // set indicator text
                if (progress == 31) {
                    expiryIndicatorText.setText(R.string.expiry_gt_30);
                    MainActivity.kitchenExpiryLimit = -1;
                } else if (progress == 0) {
                    expiryIndicatorText.setText(R.string.expiry_0);
                    MainActivity.kitchenExpiryLimit = progress;
                } else {
                    expiryIndicatorText.setText(progress + " days");
                    MainActivity.kitchenExpiryLimit = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // on click -> apply filters and return to list
        applyButton.setOnClickListener(v -> {

            MainActivity.kitchenLocationWhitelist.clear();
            MainActivity.kitchenTypeWhitelist.clear();

            for (int i = 0; i < chipGroupLocation.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupLocation.getChildAt(i);
                if (chip.isChecked()) {
                    MainActivity.kitchenLocationWhitelist.add(String.valueOf(chip.getText()));
                } else {
                    MainActivity.kitchenLocationWhitelist.remove(String.valueOf(chip.getText()));
                }
            }

            for (int i = 0; i < chipGroupType.getChildCount(); i++) {
                Chip chip = (Chip) chipGroupType.getChildAt(i);
                if (chip.isChecked()) {
                    MainActivity.kitchenTypeWhitelist.add(String.valueOf(chip.getText()));
                } else {
                    MainActivity.kitchenTypeWhitelist.remove(String.valueOf(chip.getText()));
                }
            }

            getParentFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container, KitchenListFragment.class, null)
                    .commit();
            Toast.makeText(getContext(), R.string.filtered, Toast.LENGTH_SHORT).show();

        });

        return view;
    }
}
