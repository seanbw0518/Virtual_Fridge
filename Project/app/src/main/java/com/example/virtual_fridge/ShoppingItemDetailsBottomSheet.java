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

public class ShoppingItemDetailsBottomSheet extends BottomSheetDialogFragment {

    ShoppingItem item;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // initializing views
        View view = inflater.inflate(R.layout.shopping_item_details_sheet, container, false);

        Button deleteButton = view.findViewById(R.id.delete_button);
        Button editButton = view.findViewById(R.id.edit_button);
        Button addToKitchenButton = view.findViewById(R.id.add_to_kitchen_button);

        // get data regarding clicked item and add its details to fragment
        Bundle bundleIn = this.getArguments();
        if (bundleIn != null) {
            item = bundleIn.getParcelable("item");

            TextView name = view.findViewById(R.id.shopping_item_name);
            TextView type = view.findViewById(R.id.shopping_item_type);

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

            if (item.getType() == null) {
                type.setVisibility(View.GONE);
            } else {
                type.setText(item.getType());
            }
        }

        // on click -> delete item
        deleteButton.setOnClickListener(v -> {

            ShoppingItemViewModel shoppingItemViewModel = new ViewModelProvider(requireActivity()).get(ShoppingItemViewModel.class);
            ShoppingListAdapter adapter = new ShoppingListAdapter(shoppingItemViewModel);

            adapter.setItemBackup(item);
            shoppingItemViewModel.delete(item);

            // close bottom sheet
            dismiss();

            Snackbar.make(getActivity().findViewById(R.id.shopping_list_cl), "Item deleted.", Snackbar.LENGTH_SHORT)
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

            AddShoppingItemFormFragment addShoppingItemFormFragment = new AddShoppingItemFormFragment();
            addShoppingItemFormFragment.setArguments(bundleOut);

            getParentFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container, addShoppingItemFormFragment, null)
                    .commit();

            // close bottom sheet
            dismiss();
        });

        // on click -> add item to kitchen list
        addToKitchenButton.setOnClickListener(v -> {
            KitchenItemViewModel kitchenItemViewModel = new ViewModelProvider(requireActivity()).get(KitchenItemViewModel.class);

            KitchenItem newKitchenItem = new KitchenItem(item.getName(), item.getQuantity(), item.getUnit(), null, null, item.getType());

            kitchenItemViewModel.insert(newKitchenItem);
            // close bottom sheet
            dismiss();
            Toast.makeText(getContext(), "Item Added!", Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}
