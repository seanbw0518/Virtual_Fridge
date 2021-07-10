package com.example.virtual_fridge;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddKitchenItemFormFragment extends Fragment implements View.OnClickListener, DatePickerDialog.OnDateSetListener, View.OnFocusChangeListener {

    private String mode;
    private KitchenItem item;

    // When fragment started
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // initialize views
        View view = inflater.inflate(R.layout.add_kitchen_item_fragment, container, false);

        Toolbar toolbar = view.findViewById(R.id.add_kitchen_item_toolbar);

        AutoCompleteTextView unitsDropdown = view.findViewById(R.id.kitchen_units_dropdown);
        AutoCompleteTextView typeDropdown = view.findViewById(R.id.kitchen_type_dropdown);

        Button expiryInputButton = view.findViewById(R.id.expiry_input_button);

        final Button button = view.findViewById(R.id.kitchen_form_save);
        button.setOnClickListener(this);

        // setting up shared preferences
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

        // back button setup
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, KitchenListFragment.class, null)
                .commit());

        int initYear;
        int initMonth;
        int initDay;

        // get data regarding clicked item and add its details to fragment
        Bundle bundleIn = this.getArguments();
        if (bundleIn != null) {
            item = bundleIn.getParcelable("item");
            try {
                if (bundleIn.getString("mode").equals("editFromBarcode")) {
                    mode = "insert";
                }
            } catch (Exception e) {
                mode = "update";
            }

            TextInputEditText nameInput = view.findViewById(R.id.kitchen_item_name_input);
            nameInput.setText(item.getName());
            TextInputEditText quantityInput = view.findViewById(R.id.kitchen_item_quantity_input);
            quantityInput.setText(String.valueOf(item.getQuantity()));

            if (item.getExpiryDate() == null) {
                expiryInputButton.setText(R.string.no_exp_date_label);
            } else {
                expiryInputButton.setText(item.getExpiryDate());
            }

            unitsDropdown.setText(item.getUnit());
            typeDropdown.setText(item.getType());

            RadioGroup locationOptions = view.findViewById(R.id.location_options_group);

            try {
                if (item.getLocation().equals("Fridge")) {
                    locationOptions.check(R.id.location_option_fridge);
                } else if (item.getLocation().equals("Pantry")) {
                    locationOptions.check(R.id.location_option_pantry);
                } else if (item.getLocation().equals("Freezer")) {
                    locationOptions.check(R.id.location_option_freezer);
                } else if (item.getLocation().equals("Other")) {
                    locationOptions.check(R.id.location_option_other);
                }
            } catch (NullPointerException npe) {
                locationOptions.clearCheck();
            }

        } else {
            mode = "insert";
        }

        TextInputLayout nameInputParent = view.findViewById(R.id.kitchen_item_name_input_parent);
        TextInputLayout quantityInputParent = view.findViewById(R.id.kitchen_item_quantity_input_parent);

        TextView nameInput = view.findViewById(R.id.kitchen_item_name_input);
        TextView quantityInput = view.findViewById(R.id.kitchen_item_quantity_input);

        // Calendar object to use for expiry date picker
        Calendar cal = Calendar.getInstance();

        // getting just the dd/mm/yyyy bit
        if (!expiryInputButton.getText().toString().equals(getResources().getString(R.string.no_exp_date_label))) {

            try {
                String[] expiryDateAsArray = expiryInputButton.getText().toString().split("/");

                initDay = Integer.parseInt(expiryDateAsArray[0]);
                initMonth = Integer.parseInt(expiryDateAsArray[1]) - 1; // minus 1 because Calendar months start at 0
                initYear = Integer.parseInt(expiryDateAsArray[2]);

                cal.set(Calendar.DAY_OF_MONTH, initDay);
                cal.set(Calendar.MONTH, initMonth);
                cal.set(Calendar.YEAR, initYear);

            } catch (ArrayIndexOutOfBoundsException ignored) {
            }
        }

        // dialog for selecting expiry date
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view1, year, month, dayOfMonth) -> expiryInputButton.setText(
                dayOfMonth + "/" + (month + 1) + "/" + year),
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));

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
                    if (TextUtils.isEmpty(s) || TextUtils.equals(s, "")) {
                        quantityInputParent.setError(getString(R.string.error_empty));
                    } else if (Float.parseFloat(s.toString()) > 999) {
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

        // show calendar input for expiry date
        expiryInputButton.setOnClickListener(v -> datePickerDialog.show());

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

        // type selection dropdown menu
        ArrayAdapter<CharSequence> dropdownAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.item_types, R.layout.dropdown_item);
        typeDropdown.setAdapter(dropdownAdapter);

        return view;
    }

    // listener for editText focus removal
    // on click -> close keyboard
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            hideKeyboard(v);
        }
    }

    // listener for save button press
    // on click -> check all is valid then add to list
    @Override
    public void onClick(View v) {

        View parent = (View) v.getParent();
        if (parent != null) {

            // all input areas
            EditText nameForm = parent.findViewById(R.id.kitchen_item_name_input);
            EditText quantityForm = parent.findViewById(R.id.kitchen_item_quantity_input);
            EditText unitForm = parent.findViewById(R.id.kitchen_units_dropdown);
            Button expiryForm = parent.findViewById(R.id.expiry_input_button);
            RadioGroup locationForm = parent.findViewById(R.id.location_options_group);
            EditText typeForm = parent.findViewById(R.id.kitchen_type_dropdown);

            TextInputLayout nameInputParent = parent.findViewById(R.id.kitchen_item_name_input_parent);
            TextInputLayout quantityInputParent = parent.findViewById(R.id.kitchen_item_quantity_input_parent);

            // if invalid input, i.e. no name or quantity
            if (nameForm.getText().toString().equals("") || nameForm.getText().toString() == null || quantityForm.getText().toString().equals("") || quantityForm.getText().toString() == null || Float.parseFloat(quantityForm.getText().toString()) > 999) {
                Toast.makeText(getContext(), getString(R.string.missing_info_add), Toast.LENGTH_SHORT).show();

                if (nameForm.getText().toString().equals("") || nameForm.getText().toString() == null) {
                    nameInputParent.setError(getString(R.string.error_empty));
                }
                if (quantityForm.getText().toString().equals("") || quantityForm.getText().toString() == null) {
                    quantityInputParent.setError(getString(R.string.error_empty));
                }
                try {
                    if (Float.parseFloat(quantityForm.getText().toString()) > 999) {
                        quantityInputParent.setError(getString(R.string.error_too_big));
                    }
                } catch (NumberFormatException ignore) {

                }

                // else, get all inputs and add to list view model
            } else {
                String itemName = nameForm.getText().toString();
                float itemQuantity = Float.parseFloat(quantityForm.getText().toString());
                String itemUnit = unitForm.getText().toString();
                String itemExpiry;
                try {
                    if (expiryForm.getText().equals(getResources().getString(R.string.no_exp_date_label))) {
                        itemExpiry = null;
                    } else {
                        itemExpiry = expiryForm.getText().toString();
                    }
                } catch (ArrayIndexOutOfBoundsException ignore) {
                    itemExpiry = null;
                }
                RadioButton selectedRadio = parent.findViewById(locationForm.getCheckedRadioButtonId());
                String itemLocation = "Other";
                if (selectedRadio != null) {
                    itemLocation = selectedRadio.getText().toString();
                }

                String itemType = typeForm.getText().toString();
                if (itemType.equals("")) {
                    itemType = null;
                }

                KitchenItemViewModel kitchenItemViewModel = new ViewModelProvider(requireActivity()).get(KitchenItemViewModel.class);

                if (mode.equals("update")) {
                    item.setName(itemName);
                    item.setQuantity(itemQuantity);
                    item.setExpiryDate(itemExpiry);
                    item.setUnit(itemUnit);
                    item.setLocation(itemLocation);
                    item.setType(itemType);

                    kitchenItemViewModel.update(item);

                } else if (mode.equals("insert")) {
                    item = new KitchenItem(itemName, itemQuantity, itemUnit, itemExpiry, itemLocation, itemType);
                    kitchenItemViewModel.insert(item);
                }

                // hide keyboard and go to list fragment
                hideKeyboard(v);
                getParentFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container, KitchenListFragment.class, null)
                        .commit();
                Toast.makeText(getContext(), R.string.item_added, Toast.LENGTH_SHORT).show();

            }
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
    }
}
