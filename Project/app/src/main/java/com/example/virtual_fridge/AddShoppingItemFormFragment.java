package com.example.virtual_fridge;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddShoppingItemFormFragment extends Fragment implements View.OnClickListener, View.OnFocusChangeListener {

    private String mode;
    private ShoppingItem item;

    // When fragment started
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // initialize views
        View view = inflater.inflate(R.layout.add_shopping_item_fragment, container, false);

        Toolbar toolbar = view.findViewById(R.id.add_shopping_item_toolbar);

        AutoCompleteTextView unitsDropdown = view.findViewById(R.id.shopping_units_dropdown);
        AutoCompleteTextView typeDropdown = view.findViewById(R.id.shopping_type_dropdown);

        final Button button = view.findViewById(R.id.shopping_form_save);
        button.setOnClickListener(this);

        // setting up shared preferences
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

        // back button setup
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, ShoppingListFragment.class, null)
                .commit());

        // get data regarding clicked item and add its details to fragment
        Bundle bundleIn = this.getArguments();
        if (bundleIn != null) {
            item = bundleIn.getParcelable("item");
            mode = "update";

            TextInputEditText nameInput = view.findViewById(R.id.shopping_item_name_input);
            nameInput.setText(item.getName());
            TextInputEditText quantityInput = view.findViewById(R.id.shopping_item_quantity_input);
            if (item.getQuantity() != -1) {
                quantityInput.setText(String.valueOf(item.getQuantity()));
            }

            unitsDropdown.setText(item.getUnit());
            typeDropdown.setText(item.getType());

        } else {
            mode = "insert";
        }

        TextInputLayout nameInputParent = view.findViewById(R.id.shopping_item_name_input_parent);
        TextInputLayout quantityInputParent = view.findViewById(R.id.shopping_item_quantity_input_parent);

        TextView nameInput = view.findViewById(R.id.shopping_item_name_input);
        TextView quantityInput = view.findViewById(R.id.shopping_item_quantity_input);

        // to validate name input field
        nameInput.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s) || TextUtils.equals(s, "")) {
                    nameInputParent.setError(getString(R.string.error_empty));
                } else {
                    nameInputParent.setError(null);
                }
            }
        });

        nameInput.setOnFocusChangeListener(this);

        // to validate quantity input field
        quantityInput.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    if (Float.parseFloat(s.toString()) > 999) {
                        quantityInputParent.setError(getString(R.string.error_too_big));
                    } else {
                        quantityInputParent.setError(null);
                    }
                } catch (NumberFormatException nfe) {
                    quantityInputParent.setError(null);
                }

            }
        });

        quantityInput.setOnFocusChangeListener(this);

        // unit selection dropdown menu
        if (!sharedPref.getBoolean(getString(R.string.unit_is_imperial), Boolean.parseBoolean(getString(R.string.unit_is_imperial_def)))) {
            ArrayAdapter<CharSequence> unitsAdapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.units_array_metric, R.layout.dropdown_item);
            unitsDropdown.setAdapter(unitsAdapter);
        } else {
            ArrayAdapter<CharSequence> unitsAdapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.units_array_imperial, R.layout.dropdown_item);
            unitsDropdown.setAdapter(unitsAdapter);
        }

        // type selection dropdown
        ArrayAdapter<CharSequence> dropdownAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.item_types, R.layout.dropdown_item);
        typeDropdown.setAdapter(dropdownAdapter);

        return view;
    }

    // listener for editText focus removal
    // on click -> hide keyboard
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            hideKeyboard(v);
        }
    }

    // listener for save button
    // on click -> check all is valid and then add to list
    @Override
    public void onClick(View v) {
        View parent = (View) v.getParent();
        if (parent != null) {

            // all input areas
            EditText nameForm = parent.findViewById(R.id.shopping_item_name_input);
            EditText quantityForm = parent.findViewById(R.id.shopping_item_quantity_input);
            AutoCompleteTextView unitForm = parent.findViewById(R.id.shopping_units_dropdown);
            AutoCompleteTextView typeForm = parent.findViewById(R.id.shopping_type_dropdown);

            TextInputLayout nameInputParent = parent.findViewById(R.id.shopping_item_name_input_parent);
            TextInputLayout quantityInputParent = parent.findViewById(R.id.shopping_item_quantity_input_parent);

            // if invalid input, i.e. no name or quantity
            float quantityTemp;

            try {
                quantityTemp = Float.parseFloat(quantityForm.getText().toString());
            } catch (NumberFormatException nfe) {
                quantityTemp = 0;
            }

            if (nameForm.getText().toString().equals("") || nameForm.getText().toString() == null || quantityTemp > 999) {
                Toast.makeText(getContext(), getString(R.string.missing_info_add), Toast.LENGTH_SHORT).show();

                if (nameForm.getText().toString().equals("") || nameForm.getText().toString() == null) {
                    nameInputParent.setError(getString(R.string.error_empty));
                }
                if (Float.parseFloat(quantityForm.getText().toString()) > 999) {
                    quantityInputParent.setError(getString(R.string.error_too_big));
                }

                // else, get all inputs and add to list view model
            } else {
                String itemName = nameForm.getText().toString();
                float itemQuantity;
                try {
                    itemQuantity = Float.parseFloat(quantityForm.getText().toString());
                } catch (NumberFormatException ignore) {
                    itemQuantity = -1;
                }

                String itemUnit = unitForm.getText().toString();

                String itemType = typeForm.getText().toString();
                if (itemType.equals("")) {
                    itemType = null;
                }

                ShoppingItemViewModel shoppingItemViewModel = new ViewModelProvider(requireActivity()).get(ShoppingItemViewModel.class);

                if (mode.equals("update")) {
                    item.setName(itemName);
                    item.setQuantity(itemQuantity);
                    item.setUnit(itemUnit);
                    item.setType(itemType);
                    shoppingItemViewModel.update(item);

                } else if (mode.equals("insert")) {
                    item = new ShoppingItem(itemName, itemQuantity, itemUnit, itemType);
                    shoppingItemViewModel.insert(item);
                }

                // hide keyboard and go to list fragment
                hideKeyboard(v);
                getParentFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container, ShoppingListFragment.class, null)
                        .commit();
                Toast.makeText(getContext(), R.string.item_added, Toast.LENGTH_SHORT).show();

            }
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
