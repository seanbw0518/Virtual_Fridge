package com.example.virtual_fridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.text.DecimalFormat;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.ShoppingItemHolder> {

    ShoppingItemViewModel viewModel;
    List<ShoppingItem> shoppingItems = new ArrayList<>();
    List<ShoppingItem> itemsCopy = new ArrayList<>();
    List<ShoppingItem> checkedList = new ArrayList<>();


    ShoppingItem itemBackup;

    // default sorting and filtering settings
    public static String sortMode = "id";
    public static boolean sortAsc = false;

    public SharedPreferences sharedPref;

    // constructor
    public ShoppingListAdapter(ShoppingItemViewModel viewModel) {
        super();
        this.viewModel = viewModel;
    }

    // method to apply shared preferences
    public void setSharedPref(SharedPreferences sharedPref) {
        this.sharedPref = sharedPref;
    }

    // method to set backup item (when deleted)
    public void setItemBackup(ShoppingItem item) {
        itemBackup = item;
    }

    // method to undo the deletion of an item
    public void undoDelete() {
        viewModel.insert(itemBackup);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ShoppingItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.shopping_list_recyclerview_item, parent, false);
        return new ShoppingItemHolder(view);
    }

    // method applies all values to the items based on the values stored in shoppingItems
    @Override
    public void onBindViewHolder(@NonNull ShoppingItemHolder holder, int position) {
        // assuming the list of items is not null (no items in shopping list)
        // set all the textviews on the item appropriately
        if (shoppingItems != null) {
            ShoppingItem current = shoppingItems.get(position);

            // Icon
            IconHelper iconHelper = new IconHelper(holder.shoppingItemIcon.getContext());
            try {
                holder.shoppingItemIcon.setImageResource(iconHelper.getIcon(current.getName(), current.getType()));
            } catch (NullPointerException npe) {
                holder.shoppingItemIcon.setImageResource(iconHelper.getIcon(current.getName(), "Other"));
            }

            // Name
            String name = current.getName();

            // Quantity & Unit
            String unit;
            UnitsHelper unitsHelper = new UnitsHelper(holder.shoppingItemName.getContext());

            if (current.getUnit() == null || current.getUnit().isEmpty()) {
                unit = "";
            } else {
                if (Arrays.toString(holder.shoppingItemName.getContext().getResources().getStringArray(R.array.units_generic)).contains(current.getUnit())) {
                    unit = " " + current.getUnit();
                } else {
                    unit = unitsHelper.convertToAbbreviation(sharedPref.getBoolean(holder.shoppingItemName.getContext().getString(R.string.unit_is_imperial), Boolean.parseBoolean(holder.shoppingItemName.getContext().getString(R.string.unit_is_imperial_def))), current.getUnit());
                }
            }

            String quantityUnit;

            if (current.getQuantity() == -1) {
                quantityUnit = "";

            } else {

                name = " " + name;
                if (current.getQuantity() % 1 == 0) {
                    quantityUnit = (int) current.getQuantity() + unit;

                } else {
                    DecimalFormat rounder = new DecimalFormat("####.#");

                    quantityUnit = rounder.format(current.getQuantity()) + unit;
                }

            }

            SpannableStringBuilder sb = new SpannableStringBuilder(quantityUnit + name);
            StyleSpan boldStyle = new StyleSpan(android.graphics.Typeface.BOLD);

            sb.setSpan(boldStyle, 0, quantityUnit.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

            holder.shoppingItemName.setText(sb);

            // Type
            if (current.getType() == null || current.getType().equals("Other")) {
                holder.shoppingItemType.setVisibility(View.GONE);

            } else {
                holder.shoppingItemType.setText(current.getType());
                holder.shoppingItemType.setVisibility(View.VISIBLE);
            }

            // Checkbox
            holder.shoppingItemCheckbox.setChecked(current.isChecked());

        }
    }

    // get number of items in list - mandatory
    @Override
    public int getItemCount() {
        if (shoppingItems != null)
            return shoppingItems.size();
        else return 0;
    }

    // set items in list
    public void setShoppingItems(Context context, List<ShoppingItem> items) {
        this.shoppingItems = new ArrayList<>(items);
        UnitsHelper unitsHelper = new UnitsHelper(context);

        // set correct units (metric/imperial)
        if (sharedPref.getBoolean(context.getString(R.string.unit_is_imperial), Boolean.parseBoolean(context.getString(R.string.unit_is_imperial_def)))) {
            for (int i = 0; i < shoppingItems.size(); i++) {
                ShoppingItem si = shoppingItems.get(i);
                si.setQuantity(Float.parseFloat(unitsHelper.toImperial(si.getQuantity(), si.getUnit())[0]));
                si.setUnit(unitsHelper.toImperial(si.getQuantity(), si.getUnit())[1]);
            }
        } else {
            for (int i = 0; i < shoppingItems.size(); i++) {
                ShoppingItem si = shoppingItems.get(i);
                si.setQuantity(Float.parseFloat(unitsHelper.toMetric(si.getQuantity(), si.getUnit())[0]));
                si.setUnit(unitsHelper.toMetric(si.getQuantity(), si.getUnit())[1]);
            }
        }

        this.itemsCopy = new ArrayList<>(this.shoppingItems);

        notifyDataSetChanged();
    }

    // filter by search query
    public void filterByQuery(String query) {

        String queryLower = query.toLowerCase();
        String itemName;

        shoppingItems.clear();
        if (queryLower.isEmpty()) {
            shoppingItems.addAll(itemsCopy);
        } else {
            for (ShoppingItem item : itemsCopy) {
                itemName = item.getName().toLowerCase();

                if (itemName.contains(queryLower)) {
                    shoppingItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    // filter by type
    public void filterByChoice() {

        String itemType;

        shoppingItems.clear();
        if (MainActivity.shoppingTypeWhitelist.isEmpty()) {
            shoppingItems.addAll(itemsCopy);
        } else {
            for (ShoppingItem item : itemsCopy) {

                if (item.getType() == null) {
                    itemType = "Other";
                } else {
                    itemType = item.getType();
                }

                if (MainActivity.shoppingTypeWhitelist.contains(itemType)) {
                    shoppingItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    // sort items in list
    public void sortItems(String byAttribute, boolean asc) {

        sortMode = byAttribute;
        sortAsc = asc;

        // get checked items to separate list
        checkedList.clear();

        for (int i = 0; i < shoppingItems.size(); i++) {
            ShoppingItem itemTemp = shoppingItems.get(i);

            if (itemTemp.isChecked()) {
                checkedList.add(itemTemp);
            }
        }

        shoppingItems.sort((o1, o2) -> {
            switch (byAttribute) {
                case "id":
                    if (o1.getId() > o2.getId()) {
                        return 1;
                    } else {
                        return -1;
                    }
                case "name":
                    return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
                case "type":
                    if (o1.getType() == null && o2.getType() != null) {
                        return "".compareTo(o2.getType());
                    } else if (o1.getType() != null && o2.getType() == null) {
                        return o1.getType().compareTo("");
                    } else if (o1.getType() == null && o2.getType() == null) {
                        return 0;
                    } else {
                        return o1.getType().compareTo(o2.getType());
                    }

                default:
                    return 0;
            }
        });
        if (asc) {
            Collections.reverse(shoppingItems);
        }

        // sort checked items separately
        checkedList.sort((o1, o2) -> {
            switch (byAttribute) {
                case "id":
                    if (o1.getId() > o2.getId()) {
                        return 1;
                    } else {
                        return -1;
                    }
                case "name":
                    return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
                case "type":
                    if (o1.getType() == null && o2.getType() != null) {
                        return "".compareTo(o2.getType());
                    } else if (o1.getType() != null && o2.getType() == null) {
                        return o1.getType().compareTo("");
                    } else if (o1.getType() == null && o2.getType() == null) {
                        return 0;
                    } else {
                        return o1.getType().compareTo(o2.getType());
                    }

                default:
                    return 0;
            }
        });
        if (asc) {
            Collections.reverse(checkedList);
        }

        // remove any checked items from main list
        for (int i = 0; i < itemsCopy.size(); i++) {
            ShoppingItem itemTemp = itemsCopy.get(i);
            if (itemTemp.isChecked()) {
                shoppingItems.remove(itemTemp);
            }
        }

        // put checked items onto end of main list
        shoppingItems.addAll(checkedList);

        notifyDataSetChanged();
    }

    // ITEM HOLDER ------------------------------------------------------------------------------ //
    // class for the items in the recyclerview
    class ShoppingItemHolder extends RecyclerView.ViewHolder {
        private final TextView shoppingItemName;
        private final TextView shoppingItemType;
        private final ImageView shoppingItemIcon;
        private final MaterialCheckBox shoppingItemCheckbox;

        private ShoppingItemHolder(View itemView) {
            super(itemView);
            shoppingItemName = itemView.findViewById(R.id.shopping_item_name);
            shoppingItemType = itemView.findViewById(R.id.shopping_item_type);
            shoppingItemIcon = itemView.findViewById(R.id.shopping_item_icon);
            shoppingItemCheckbox = itemView.findViewById(R.id.shopping_item_check);

            // on click -> show details
            itemView.setOnClickListener(v -> {

                AppCompatActivity activity = (AppCompatActivity) v.getContext();
                Bundle bundle = new Bundle();
                bundle.putParcelable("item", shoppingItems.get(getLayoutPosition()));

                ShoppingItemDetailsBottomSheet shoppingItemDetailsBottomSheet = new ShoppingItemDetailsBottomSheet();
                shoppingItemDetailsBottomSheet.setArguments(bundle);

                shoppingItemDetailsBottomSheet.show(activity.getSupportFragmentManager(), "sheet");
            });


            shoppingItemCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                ShoppingItem item = shoppingItems.get(getLayoutPosition());

                if (isChecked) {
                    itemView.setBackgroundColor(itemView.getContext().getColor(R.color.selected_item));
                    item.setChecked(true);

                } else {
                    // -- Code used with thanks from Amit Vaghela at https://stackoverflow.com/questions/37987732/programmatically-set-selectableitembackground-on-android-view
                    TypedValue outValue = new TypedValue();
                    itemView.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                    itemView.setBackgroundResource(outValue.resourceId);
                    // --

                    item.setChecked(false);
                }
                viewModel.update(item);

            });
        }
    }
}
