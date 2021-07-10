package com.example.virtual_fridge;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BarcodeScanResultSheet extends BottomSheetDialogFragment {

    String[] scanResult;
    String scannedItemName;
    float scannedItemQuantity;
    String scannedItemUnit;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // used to track whether this sheet is fully open (so as to not keep spawning new ones when the barcode scans continuously
        BarcodeScanFragment.resultsOpen = true;

        // initialize views
        View view = inflater.inflate(R.layout.barcode_scan_result_sheet, container, false);
        TextView nameView = view.findViewById(R.id.scan_name);
        Button editButton = view.findViewById(R.id.edit_button);
        Button saveButton = view.findViewById(R.id.save_button);

        // setting up shared preferences
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

        // get data regarding clicked item and add its details to fragment
        Bundle bundleIn = this.getArguments();
        UnitsHelper unitsHelper = new UnitsHelper(getContext());
        if (bundleIn != null) {
            scanResult = bundleIn.getStringArray("scanResult");

            if (scanResult[0].equals("success")) {

                scannedItemName = scanResult[1];

                if (!scanResult[2].equals("quantityNotFound")) {
                    String scannedItemQuantityUnit = scanResult[2].replace(" ", "");
                    scannedItemQuantity = Float.parseFloat(scannedItemQuantityUnit.replaceAll("[^0-9]", ""));
                    scannedItemUnit = scannedItemQuantityUnit.replaceAll("[0-9]| ", "");

                    nameView.setText(scannedItemName + ", " + scannedItemQuantityUnit);
                } else {
                    scannedItemQuantity = 1;
                    scannedItemUnit = null;

                    nameView.setText(scannedItemName);
                }

                // on click -> go to edit form
                editButton.setOnClickListener(v -> {
                    Bundle bundleOut = new Bundle();

                    boolean unitIsMetric = true;

                    for (int i = 0; i < getResources().getStringArray(R.array.units_array_imperial_abbrev).length; i++) {
                        if (scannedItemUnit.toLowerCase().equals(getResources().getStringArray(R.array.units_array_imperial_abbrev)[i])) {
                            unitIsMetric = false;
                            break;
                        }
                    }

                    if (sharedPref.getBoolean(getString(R.string.unit_is_imperial), Boolean.parseBoolean(getString(R.string.unit_is_imperial_def)))) {

                        String[] quantityDetails = unitsHelper.toImperial(scannedItemQuantity, unitsHelper.convertFromAbbreviation(!unitIsMetric, scannedItemUnit.trim()));

                        scannedItemQuantity = Float.parseFloat(quantityDetails[0]);
                        scannedItemUnit = quantityDetails[1].trim();
                    } else {

                        String[] quantityDetails = unitsHelper.toMetric(scannedItemQuantity, unitsHelper.convertFromAbbreviation(!unitIsMetric, scannedItemUnit.trim()));

                        scannedItemQuantity = Float.parseFloat(quantityDetails[0]);
                        scannedItemUnit = quantityDetails[1].trim();
                    }

                    bundleOut.putParcelable("item", new KitchenItem(scannedItemName, scannedItemQuantity, scannedItemUnit, null, null, null));
                    bundleOut.putString("mode", "editFromBarcode");

                    AddKitchenItemFormFragment addKitchenItemFormFragment = new AddKitchenItemFormFragment();
                    addKitchenItemFormFragment.setArguments(bundleOut);

                    getParentFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container, addKitchenItemFormFragment, null)
                            .commit();

                    // close bottom sheet
                    dismiss();
                });

                // on click -> add item to list
                saveButton.setOnClickListener(v -> {
                    KitchenItemViewModel kitchenItemViewModel = new ViewModelProvider(requireActivity()).get(KitchenItemViewModel.class);

                    if (scannedItemUnit == null) {
                        kitchenItemViewModel.insert(new KitchenItem(scannedItemName, scannedItemQuantity, scannedItemUnit, null, null, null));
                    } else {
                        kitchenItemViewModel.insert(new KitchenItem(scannedItemName, scannedItemQuantity, scannedItemUnit.replace(" ", ""), null, null, null));
                    }

                    // close bottom sheet
                    dismiss();
                    Toast.makeText(getContext(), "Item Added!", Toast.LENGTH_SHORT).show();
                });

            } else {
                nameView.setText(getString(R.string.barcode_title_error));

                editButton.setText(getString(R.string.button_obj_detect_mode));
                saveButton.setText(getString(R.string.button_manual_mode));

                // on click -> go to object detect mode
                editButton.setOnClickListener(v -> {

                    // switch to object detect mode
                    getParentFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container, ObjectDetectFragment.class, null)
                            .commit();

                    // close bottom sheet
                    dismiss();
                });

                // on click -> go to manual input form
                saveButton.setOnClickListener(v -> {

                    // go to manual text input fragment
                    getParentFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container, AddKitchenItemFormFragment.class, null)
                            .commit();

                    // close bottom sheet
                    dismiss();
                });
            }
        }


        return view;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        BarcodeScanFragment.resultsOpen = false;
    }
}
