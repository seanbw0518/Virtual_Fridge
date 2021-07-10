package com.example.virtual_fridge;

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
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

public class KitchenListDetailsBottomSheet extends BottomSheetDialogFragment {

    KitchenItem item;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // initializing views
        View view = inflater.inflate(R.layout.kitchen_item_details_sheet, container, false);

        Button deleteButton = view.findViewById(R.id.delete_button);
        Button editButton = view.findViewById(R.id.edit_button);
        Button addToShoppingButton = view.findViewById(R.id.add_to_shopping_button);

        // get data regarding clicked item and add its details to fragment
        Bundle bundleIn = this.getArguments();
        if (bundleIn != null) {
            item = bundleIn.getParcelable("item");

            TextView name = view.findViewById(R.id.kitchen_item_name);
            TextView location = view.findViewById(R.id.kitchen_item_location);
            TextView type = view.findViewById(R.id.kitchen_item_type);
            TextView expiry = view.findViewById(R.id.kitchen_item_expiry);

            String headingString;

            if (item.getUnit() == null || item.getUnit().equals("")) {
                if (item.getQuantity() == -1) {
                    headingString = item.getName();
                } else if (item.getQuantity() % 1 == 0) {
                    headingString = ((int) item.getQuantity()) + " " + item.getName();
                } else {
                    headingString = item.getQuantity() + " " + item.getName();
                }
            } else {
                if (item.getQuantity() % 1 == 0) {
                    headingString = ((int) item.getQuantity()) + " " + item.getUnit().toLowerCase() + " of " + item.getName();
                } else {
                    headingString = item.getQuantity() + " " + item.getUnit().toLowerCase() + " of " + item.getName();
                }
            }

            name.setText(headingString);

            if (item.getExpiryDate() == null) {
                expiry.setVisibility(View.GONE);
            } else {
                expiry.setText("Expires on: " + item.getExpiryDate());

            }

            if (item.getLocation() == null) {
                location.setVisibility(View.GONE);
            } else {
                if (item.getLocation().equals("Other")) {
                    location.setText("Location: Unknown");
                } else {
                    location.setText("Location: " + item.getLocation());
                }
            }

            if (item.getType() == null) {
                type.setVisibility(View.GONE);
            } else {
                type.setText("Type: " + item.getType());
            }
        }

        // on click -> delete item
        deleteButton.setOnClickListener(v -> {

            KitchenItemViewModel kitchenItemViewModel = new ViewModelProvider(requireActivity()).get(KitchenItemViewModel.class);
            KitchenListAdapter adapter = new KitchenListAdapter(kitchenItemViewModel, new ViewModelProvider(requireActivity()).get(ShoppingItemViewModel.class));

            adapter.setItemBackup(item);
            kitchenItemViewModel.delete(item);

            // close bottom sheet
            dismiss();

            Snackbar.make(getActivity().findViewById(R.id.kitchen_list_cl), "Item deleted.", Snackbar.LENGTH_SHORT)
                    .setAnchorView(getActivity().findViewById(R.id.bottom_navigation))
                    .setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE)
                    .setAction(R.string.undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dismiss();
                            adapter.undoDelete();
                        }
                    })
                    .show();
        });

        // on click -> go to edit form
        editButton.setOnClickListener(v -> {
            Bundle bundleOut = new Bundle();
            bundleOut.putParcelable("item", item);

            AddKitchenItemFormFragment addKitchenItemFormFragment = new AddKitchenItemFormFragment();
            addKitchenItemFormFragment.setArguments(bundleOut);

            getParentFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container, addKitchenItemFormFragment, null)
                    .commit();

            // close bottom sheet
            dismiss();
        });

        // on click -> add to shopping list
        addToShoppingButton.setOnClickListener(v -> {
            ShoppingItemViewModel shoppingItemViewModel = new ViewModelProvider(requireActivity()).get(ShoppingItemViewModel.class);

            ShoppingItem newShoppingItem = new ShoppingItem(item.getName(), -1, null, item.getType(), false);

            shoppingItemViewModel.insert(newShoppingItem);
            // close bottom sheet
            dismiss();
            Toast.makeText(getContext(), "Item Added!", Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}
