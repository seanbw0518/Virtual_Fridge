package com.example.virtual_fridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ShoppingListFragment extends Fragment {

    private ShoppingItemViewModel shoppingItemViewModel;
    private ShoppingListAdapter shoppingListAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // initializing views
        View view = inflater.inflate(R.layout.shopping_list_fragment, container, false);

        Toolbar toolbar = view.findViewById(R.id.shopping_list_toolbar);
        View noItemsView = view.findViewById(R.id.no_shopping_items_view);
        RecyclerView recyclerView = view.findViewById(R.id.shopping_list_recyclerview);

        TextView heading = view.findViewById(R.id.shopping_list_heading);
        SearchView searchView = (SearchView) view.findViewById(R.id.shopping_list_search);

        FloatingActionButton fab = view.findViewById(R.id.shopping_list_fab);

        // setting up shared preferences
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        // setting up view model & adapter
        shoppingItemViewModel = new ViewModelProvider(requireActivity()).get(ShoppingItemViewModel.class);
        shoppingListAdapter = new ShoppingListAdapter(shoppingItemViewModel);
        shoppingListAdapter.setSharedPref(sharedPref);

        shoppingItemViewModel.getAllShoppingItems().observe(getViewLifecycleOwner(), items -> {
            // set the list of items in the adapter whenever the view model changes
            shoppingListAdapter.setShoppingItems(getContext(), shoppingItemViewModel.getAllShoppingItemsList());

            if (shoppingListAdapter.getItemCount() == 0) {
                noItemsView.setVisibility(View.VISIBLE);
            } else {
                noItemsView.setVisibility(View.GONE);
            }

            shoppingListAdapter.filterByChoice();

            if (shoppingListAdapter.getItemCount() != shoppingListAdapter.itemsCopy.size()) {
                toolbar.findViewById(R.id.shopping_list_filter).setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark, null));
            } else {
                toolbar.findViewById(R.id.shopping_list_filter).setBackgroundColor(getResources().getColor(R.color.colorPrimary, null));
            }

            if (MainActivity.shoppingTypeWhitelist.isEmpty() || MainActivity.shoppingTypeWhitelist.size() == getResources().getStringArray(R.array.item_types).length) {

                toolbar.findViewById(R.id.shopping_list_filter).setBackgroundColor(getResources().getColor(R.color.colorPrimary, null));

            }

            shoppingListAdapter.sortItems(ShoppingListAdapter.sortMode, ShoppingListAdapter.sortAsc);

        });

        // setting up recyclerview
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setAdapter(shoppingListAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        // ------ Listeners ----- //

        // on click -> (sort -> show popup menu, filter -> go to filter fragment, asc/desc -> switch order and re-sort)
        toolbar.setOnMenuItemClickListener(item -> {

            int pressedItemId = item.getItemId();

            // SORT
            if (pressedItemId == R.id.shopping_list_sort) {
                View sortButton = view.findViewById(R.id.shopping_list_sort);

                PopupMenu popup = new PopupMenu(getContext(), sortButton);
                popup.getMenuInflater().inflate(R.menu.shopping_popup_sort_options_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item1 -> {

                    if (item1.getTitle().toString().equals("WHEN ADDED")) {
                        ShoppingListAdapter.sortMode = "id";
                    } else {
                        ShoppingListAdapter.sortMode = item1.getTitle().toString().toLowerCase();
                    }

                    shoppingListAdapter.sortItems(ShoppingListAdapter.sortMode, ShoppingListAdapter.sortAsc);
                    if (ShoppingListAdapter.sortAsc) {
                        Toast.makeText(getContext(), getContext().getString(R.string.sorted) + " " + ShoppingListAdapter.sortMode + ", ascending.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), getContext().getString(R.string.sorted) + " " + ShoppingListAdapter.sortMode + ", descending.", Toast.LENGTH_SHORT).show();
                    }

                    return true;
                });
                popup.show();

                //FILTER
            } else if (pressedItemId == R.id.shopping_list_filter) {

                getParentFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container, ShoppingListFilterFragment.class, null)
                        .commit();
            }

            // ASC/DESC
            else if (pressedItemId == R.id.shopping_list_order) {
                ShoppingListAdapter.sortAsc = !ShoppingListAdapter.sortAsc;

                shoppingListAdapter.sortItems(ShoppingListAdapter.sortMode, ShoppingListAdapter.sortAsc);
                if (ShoppingListAdapter.sortAsc) {
                    Toast.makeText(getContext(), getContext().getString(R.string.sorted) + " " + ShoppingListAdapter.sortMode + ", ascending.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), getContext().getString(R.string.sorted) + " " + ShoppingListAdapter.sortMode + ", descending.", Toast.LENGTH_SHORT).show();
                }
            }

            return false;
        });

        // hide title when search active
        // on click -> hide toolbar title
        searchView.setOnSearchClickListener(v -> heading.setVisibility(View.GONE));
        // on close -> reset search filter
        searchView.setOnCloseListener(() -> {
            // reset search
            shoppingListAdapter.filterByQuery("");
            heading.setVisibility(View.VISIBLE);
            return false;
        });

        // when search query entered
        // on query changed -> apply search filter
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // not used
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // do search
                shoppingListAdapter.filterByQuery(newText);
                return false;
            }
        });

        // on click -> go to add item form
        fab.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, AddShoppingItemFormFragment.class, null)
                .commit());

        // welcome dialog
        // on "OK" click -> close dialog
        MaterialAlertDialogBuilder welcomeDialog = new MaterialAlertDialogBuilder(recyclerView.getContext())
                .setTitle("Shopping List")
                .setMessage("This is your shopping list.\n\nHere you can add items directly, or by tapping on an item in your kitchen list.")
                .setIcon(R.drawable.cart_icon)
                .setNegativeButton(R.string.ok, (dialog, which) -> {
                    editor.putBoolean(getString(R.string.shopping_welcome_seen), true);
                    editor.apply();
                    dialog.cancel();
                });

        // uncomment below for debugging welcome dialog
        //editor.putBoolean(getString(R.string.shopping_welcome_seen), false);
        //editor.apply();
        if (!sharedPref.getBoolean(getString(R.string.shopping_welcome_seen), Boolean.parseBoolean(getString(R.string.shopping_welcome_seen_def)))) {
            welcomeDialog.create().show();
        }

        return view;
    }
}