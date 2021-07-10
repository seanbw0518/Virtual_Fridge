package com.example.virtual_fridge;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class ObjDetectResultSheet extends BottomSheetDialogFragment implements View.OnClickListener {

    List<String> scanResult;

    TextView name1;
    TextView name2;
    TextView name3;
    TextView name4;
    TextView name5;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // initializing and setting up views
        View view = inflater.inflate(R.layout.obj_detect_result_sheet, container, false);
        IconHelper iconHelper = new IconHelper(view.getContext());

        ImageView icon1 = view.findViewById(R.id.item_1_icon);
        ImageView icon2 = view.findViewById(R.id.item_2_icon);
        ImageView icon3 = view.findViewById(R.id.item_3_icon);
        ImageView icon4 = view.findViewById(R.id.item_4_icon);
        ImageView icon5 = view.findViewById(R.id.item_5_icon);

        name1 = view.findViewById(R.id.item_1_name);
        name2 = view.findViewById(R.id.item_2_name);
        name3 = view.findViewById(R.id.item_3_name);
        name4 = view.findViewById(R.id.item_4_name);
        name5 = view.findViewById(R.id.item_5_name);

        Button button1 = view.findViewById(R.id.item_1_button);
        Button button2 = view.findViewById(R.id.item_2_button);
        Button button3 = view.findViewById(R.id.item_3_button);
        Button button4 = view.findViewById(R.id.item_4_button);
        Button button5 = view.findViewById(R.id.item_5_button);

        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
        button4.setOnClickListener(this);
        button5.setOnClickListener(this);

        TextView inputOptionsHeading = view.findViewById(R.id.input_options_heading);

        Button switchToBarcodeButton = view.findViewById(R.id.try_barcode_button);
        Button switchToManualButton = view.findViewById(R.id.try_manual_button);

        // on click -> go to barcode scan
        switchToBarcodeButton.setOnClickListener(v -> {

            // switch to barcode scan mode
            getParentFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container, BarcodeScanFragment.class, null)
                    .commit();

            // close bottom sheet
            dismiss();
        });

        // on click -> go to manual form
        switchToManualButton.setOnClickListener(v -> {

            // go to manual text input fragment
            getParentFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container, AddKitchenItemFormFragment.class, null)
                    .commit();

            // close bottom sheet
            dismiss();
        });

        // get data regarding clicked item and add its details to fragment
        Bundle bundleIn = this.getArguments();
        if (bundleIn != null) {
            if (bundleIn.getStringArrayList("scanResult") != null) {
                scanResult = bundleIn.getStringArrayList("scanResult");

                name1.setText(scanResult.get(0));
                icon1.setImageResource(iconHelper.getIcon(scanResult.get(0), null));
                name2.setText(scanResult.get(1));
                icon2.setImageResource(iconHelper.getIcon(scanResult.get(1), null));
                name3.setText(scanResult.get(2));
                icon3.setImageResource(iconHelper.getIcon(scanResult.get(2), null));
                name4.setText(scanResult.get(3));
                icon4.setImageResource(iconHelper.getIcon(scanResult.get(3), null));
                name5.setText(scanResult.get(4));
                icon5.setImageResource(iconHelper.getIcon(scanResult.get(4), null));

            } else {

                view.findViewById(R.id.obj_detect_results_heading).setVisibility(View.GONE);

                view.findViewById(R.id.item_1).setVisibility(View.GONE);
                view.findViewById(R.id.item_2).setVisibility(View.GONE);
                view.findViewById(R.id.item_3).setVisibility(View.GONE);
                view.findViewById(R.id.item_4).setVisibility(View.GONE);
                view.findViewById(R.id.item_5).setVisibility(View.GONE);
                view.findViewById(R.id.div).setVisibility(View.GONE);

                inputOptionsHeading.setText(getString(R.string.obj_detect_title_error));
            }
        }

        return view;
    }

    // on click -> add item to list
    @Override
    public void onClick(View v) {

        String itemName;

        switch (v.getId()) {
            case R.id.item_1_button:
                itemName = (String) name1.getText();
                v.setClickable(false);
                v.setEnabled(false);
                break;
            case R.id.item_2_button:
                itemName = (String) name2.getText();
                v.setClickable(false);
                v.setEnabled(false);
                break;
            case R.id.item_3_button:
                itemName = (String) name3.getText();
                v.setClickable(false);
                v.setEnabled(false);
                break;
            case R.id.item_4_button:
                itemName = (String) name4.getText();
                v.setClickable(false);
                v.setEnabled(false);
                break;
            case R.id.item_5_button:
                itemName = (String) name5.getText();
                v.setClickable(false);
                v.setEnabled(false);
                break;
            default:
                itemName = "Error!";
                break;
        }

        KitchenItemViewModel kitchenItemViewModel = new ViewModelProvider(requireActivity()).get(KitchenItemViewModel.class);
        kitchenItemViewModel.insert(new KitchenItem(itemName, 1, null, null, null, null));
        Toast.makeText(getContext(), "Item Added!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        ObjectDetectFragment.takeImageButton.setEnabled(true);
        ObjectDetectFragment.takeImageButton.setSelected(false);
        ObjectDetectFragment.takeImageButton.setImageResource(R.drawable.camera_icon);


    }
}
