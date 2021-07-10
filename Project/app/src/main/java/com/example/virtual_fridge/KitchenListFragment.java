package com.example.virtual_fridge;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class KitchenListFragment extends Fragment {

    private KitchenItemViewModel kitchenItemViewModel;
    private KitchenListAdapter kitchenListAdapter;

    boolean fabMenuOpen;

    // input mode (text, camera, barcode)
    char requestedMode = 'x';

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        // initializing views
        View view = inflater.inflate(R.layout.kitchen_list_fragment, container, false);

        Toolbar toolbar = view.findViewById(R.id.kitchen_list_toolbar);
        View noItemsView = view.findViewById(R.id.no_kitchen_items_view);

        RecyclerView recyclerView = view.findViewById(R.id.kitchen_list_recyclerview);

        TextView heading = view.findViewById(R.id.kitchen_list_heading);
        SearchView searchView = view.findViewById(R.id.kitchen_list_search);

        FloatingActionButton mainFab = view.findViewById(R.id.kitchen_list_fab_main);
        FloatingActionButton cameraFab = view.findViewById(R.id.kitchen_list_fab_camera);
        FloatingActionButton barcodeFab = view.findViewById(R.id.kitchen_list_fab_barcode);
        FloatingActionButton textFab = view.findViewById(R.id.kitchen_list_fab_text);

        // setting up shared preferences
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        // setting up view model
        kitchenItemViewModel = new ViewModelProvider(requireActivity()).get(KitchenItemViewModel.class);
        kitchenListAdapter = new KitchenListAdapter(kitchenItemViewModel, new ViewModelProvider(requireActivity()).get(ShoppingItemViewModel.class));
        kitchenListAdapter.setSharedPref(sharedPref);

        kitchenItemViewModel.getAllKitchenItems().observe(getViewLifecycleOwner(), items -> {

            kitchenListAdapter.setKitchenItems(getContext(), kitchenItemViewModel.getAllKitchenItemsList());

            if (kitchenListAdapter.getItemCount() == 0) {
                noItemsView.setVisibility(View.VISIBLE);
            } else {
                noItemsView.setVisibility(View.GONE);
            }

            kitchenListAdapter.filterByChoice();

            if (kitchenListAdapter.getItemCount() != kitchenListAdapter.itemsCopy.size()) {
                toolbar.findViewById(R.id.kitchen_list_filter).setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark, null));
            } else {
                toolbar.findViewById(R.id.kitchen_list_filter).setBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
            }

            if ((MainActivity.kitchenTypeWhitelist.isEmpty() && MainActivity.kitchenLocationWhitelist.isEmpty() && MainActivity.kitchenExpiryLimit == -1) ||
                    (MainActivity.kitchenTypeWhitelist.size() == getResources().getStringArray(R.array.item_types).length && MainActivity.kitchenLocationWhitelist.size() == 4 && MainActivity.kitchenExpiryLimit == 31)) {

                toolbar.findViewById(R.id.kitchen_list_filter).setBackgroundColor(getResources().getColor(R.color.colorPrimary, null));

            }

            kitchenListAdapter.sortItems(KitchenListAdapter.sortMode, KitchenListAdapter.sortAsc);

        });

        // setting up recyclerview
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setAdapter(kitchenListAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(view.getContext(), DividerItemDecoration.VERTICAL));

        // ----- Listeners ----- //
        // on click -> (sort -> show popup, filter -> go to filter fragment, asc/desc -> reorder and re-sort)
        toolbar.setOnMenuItemClickListener(item -> {

            int pressedItemId = item.getItemId();

            // SORT
            if (pressedItemId == R.id.kitchen_list_sort) {
                View sortButton = view.findViewById(R.id.kitchen_list_sort);

                PopupMenu popup = new PopupMenu(view.getContext(), sortButton);
                popup.getMenuInflater().inflate(R.menu.kitchen_popup_sort_options_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item1 -> {

                    if (item1.getTitle().toString().equals("WHEN ADDED")) {
                        KitchenListAdapter.sortMode = "id";
                    } else if (item1.getTitle().toString().equals("EXPIRY DATE")) {
                        KitchenListAdapter.sortMode = "expiry";
                    } else {
                        KitchenListAdapter.sortMode = item1.getTitle().toString().toLowerCase();
                    }

                    kitchenListAdapter.sortItems(KitchenListAdapter.sortMode, KitchenListAdapter.sortAsc);
                    if (KitchenListAdapter.sortAsc) {
                        Toast.makeText(getContext(), getContext().getString(R.string.sorted) + " " + KitchenListAdapter.sortMode + ", ascending.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), getContext().getString(R.string.sorted) + " " + KitchenListAdapter.sortMode + ", descending.", Toast.LENGTH_SHORT).show();
                    }

                    return true;
                });
                popup.show();

            }

            // FILTER
            else if (pressedItemId == R.id.kitchen_list_filter) {
                getParentFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container, KitchenListFilterFragment.class, null)
                        .commit();
            }

            // ASC/DESC
            else if (pressedItemId == R.id.kitchen_list_order) {
                KitchenListAdapter.sortAsc = !KitchenListAdapter.sortAsc;
                kitchenListAdapter.sortItems(KitchenListAdapter.sortMode, KitchenListAdapter.sortAsc);
                if (KitchenListAdapter.sortAsc) {
                    Toast.makeText(getContext(), getContext().getString(R.string.sorted) + " " + KitchenListAdapter.sortMode + ", ascending.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), getContext().getString(R.string.sorted) + " " + KitchenListAdapter.sortMode + ", descending.", Toast.LENGTH_SHORT).show();
                }

            }

            return false;
        });

        // hide title when search active
        // on click -> hide title
        searchView.setOnSearchClickListener(v -> heading.setVisibility(View.GONE));
        // on close -> reset search filter
        searchView.setOnCloseListener(() -> {
            heading.setVisibility(View.VISIBLE);
            // reset search
            kitchenListAdapter.filterByQuery("");
            return false;
        });
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(v);
                searchView.setIconified(true);
                // reset search
                kitchenListAdapter.filterByQuery("");
            }
        });

        // on query change -> filter by query
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // not used
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // do search
                kitchenListAdapter.filterByQuery(newText);
                return false;
            }
        });

        int fabSize = (int) getResources().getDimension(R.dimen.fab_diameter);

        // on click -> open up other fabs with animation
        mainFab.setOnClickListener(v -> {
            if (fabMenuOpen) {
                fabMenuOpen = false;
                cameraFab.animate().translationY(0);
                barcodeFab.animate().translationY(0);
                textFab.animate().translationY(0);
                mainFab.setImageResource(R.drawable.add_icon);

            } else {
                fabMenuOpen = true;
                cameraFab.animate().translationY(-fabSize - 8);
                barcodeFab.animate().translationY(-(fabSize * 2) - 16);
                textFab.animate().translationY(-(fabSize * 3) - 24);
                mainFab.setImageResource(R.drawable.x_icon);
            }
        });

        // on click -> go to text input form
        textFab.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, AddKitchenItemFormFragment.class, null)
                .commit());

        // ---------------------- Camera based inputs ----------------------

        // to manage permission for camera
        ActivityResultLauncher<String> resultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {

                if (requestedMode == 'o') {
                    getParentFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container, ObjectDetectFragment.class, null)
                            .commit();
                } else if (requestedMode == 'b') {
                    getParentFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container, BarcodeScanFragment.class, null)
                            .commit();
                }

            } else {
                Toast.makeText(getContext(), "Cannot access camera.", Toast.LENGTH_SHORT).show();
            }

        });

        // on click -> check permissions and go to object detect fragment
        cameraFab.setOnClickListener(v -> {

            // check permission
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

                getParentFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container, ObjectDetectFragment.class, null)
                        .commit();

            } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                Toast.makeText(getContext(), "You must allow camera permission to use this feature.", Toast.LENGTH_SHORT).show();
            } else {
                requestedMode = 'o';
                resultLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // on click -> check permissions and go to barcode scan fragment
        barcodeFab.setOnClickListener(v -> {

            // check permission
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

                getParentFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container, BarcodeScanFragment.class, null)
                        .commit();

            } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                Toast.makeText(getContext(), "You must allow camera permission to use this feature.", Toast.LENGTH_SHORT).show();
            } else {
                requestedMode = 'b';
                resultLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // welcome dialog
        // on click "OK" -> close
        MaterialAlertDialogBuilder welcomeDialog = new MaterialAlertDialogBuilder(recyclerView.getContext())
                .setTitle("Welcome!")
                .setMessage("Welcome to your virtual fridge!\n\nThis screen lets you see what food you have in stock. Start by adding a new item using the + button in the bottom-right.")
                .setIcon(R.drawable.food_bank_icon)
                .setNegativeButton(R.string.ok, (dialog, which) -> {
                    dialog.cancel();
                    editor.putBoolean(getString(R.string.kitchen_welcome_seen), true);
                    editor.apply();
                });

        // uncomment below for debugging welcome dialog
        //editor.putBoolean(getString(R.string.kitchen_welcome_seen), false);
        //editor.apply();
        if (!sharedPref.getBoolean(getString(R.string.kitchen_welcome_seen), Boolean.parseBoolean(getString(R.string.kitchen_welcome_seen_def)))) {
            welcomeDialog.create().show();
        }

        AlarmReceiver.sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

        Intent alarmIntent = new Intent(getContext(), AlarmReceiver.class);
        PendingIntent notifyPendingIntent = PendingIntent.getBroadcast(getContext(), 0, alarmIntent, 0);
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);

        // set up to check every 24 hours for item expiry dates
        Calendar firingCal = Calendar.getInstance();
        Calendar currentCal = Calendar.getInstance();

        firingCal.set(Calendar.HOUR_OF_DAY, sharedPref.getInt(getString(R.string.notif_hour), Integer.parseInt(getString(R.string.notif_hour_def))));
        firingCal.set(Calendar.MINUTE, sharedPref.getInt(getString(R.string.notif_minute), Integer.parseInt(getString(R.string.notif_minute_def))));
        firingCal.set(Calendar.SECOND, 0);

        if (firingCal.compareTo(currentCal) < 0) {
            firingCal.add(Calendar.DAY_OF_MONTH, 1);
        }
        long intendedTime = firingCal.getTimeInMillis();

        alarmManager.setRepeating(AlarmManager.RTC, intendedTime, AlarmManager.INTERVAL_DAY, notifyPendingIntent);
        // --- USE LINE ABOVE FOR REAL USE --- //
        // --- USE LINE BELOW FOR TESTING (NOTIFICATION EVERY MINUTE) --- //
        //alarmManager.setRepeating(AlarmManager.RTC, 5000, 60000, notifyPendingIntent);

        return view;
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) view.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
