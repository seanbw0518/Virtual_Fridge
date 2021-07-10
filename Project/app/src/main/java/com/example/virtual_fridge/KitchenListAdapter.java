package com.example.virtual_fridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.icu.text.DecimalFormat;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KitchenListAdapter extends RecyclerView.Adapter<KitchenListAdapter.KitchenItemHolder> {

    KitchenItemViewModel viewModel;
    List<KitchenItem> kitchenItems = new ArrayList<>();
    List<KitchenItem> itemsCopy = new ArrayList<>();

    // default sorting and filtering settings
    public static String sortMode = "id";
    public static boolean sortAsc = false;

    KitchenItem itemBackup;

    ShoppingItemViewModel shoppingItemViewModel;

    public SharedPreferences sharedPref;

    // constructor
    public KitchenListAdapter(KitchenItemViewModel viewModel, ShoppingItemViewModel shoppingItemViewModel) {
        super();
        this.viewModel = viewModel;
        this.shoppingItemViewModel = shoppingItemViewModel;
    }

    // set the shared preferences
    public void setSharedPref(SharedPreferences sharedPref) {
        this.sharedPref = sharedPref;
    }

    // undo delete item
    public void undoDelete() {
        viewModel.insert(itemBackup);
        notifyDataSetChanged();
    }

    // set backup on delete item
    public void setItemBackup(KitchenItem item) {
        itemBackup = item;
    }

    @NonNull
    @Override
    public KitchenItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.kitchen_list_recyclerview_item, parent, false);
        return new KitchenItemHolder(view);
    }

    // method applies all values to the items based on the values stored in kitchenItems
    @Override
    public void onBindViewHolder(@NonNull KitchenItemHolder holder, int position) {
        // assuming the list of items is not null (no items in kitchen list)
        // set all the textviews on the item appropriately
        if (kitchenItems != null) {
            KitchenItem current = kitchenItems.get(position);

            // Icon
            IconHelper iconHelper = new IconHelper(holder.kitchenItemIcon.getContext());
            try {
                holder.kitchenItemIcon.setImageResource(iconHelper.getIcon(current.getName(), current.getType()));
            } catch (NullPointerException npe) {
                holder.kitchenItemIcon.setImageResource(iconHelper.getIcon(current.getName(), "Other"));
            }

            // Name
            String name = current.getName();

            // Quantity & Unit
            String unit;
            UnitsHelper unitsHelper = new UnitsHelper(holder.kitchenItemName.getContext());

            if (current.getUnit() == null || current.getUnit().isEmpty()) {
                unit = "";
            } else {
                if (Arrays.asList(holder.kitchenItemName.getContext().getResources().getStringArray(R.array.units_generic)).contains(current.getUnit())) {
                    unit = " " + current.getUnit();
                } else if (Arrays.asList(holder.kitchenItemName.getContext().getResources().getStringArray(R.array.units_array_metric_abbrev)).contains(current.getUnit()) || Arrays.asList(holder.kitchenItemName.getContext().getResources().getStringArray(R.array.units_array_imperial_abbrev)).contains(current.getUnit())) {
                    current.setUnit(unitsHelper.convertFromAbbreviation(sharedPref.getBoolean(holder.kitchenItemName.getContext().getString(R.string.unit_is_imperial), Boolean.parseBoolean(holder.kitchenItemName.getContext().getString(R.string.unit_is_imperial_def))), current.getUnit()));
                    unit = unitsHelper.convertToAbbreviation(sharedPref.getBoolean(holder.kitchenItemName.getContext().getString(R.string.unit_is_imperial), Boolean.parseBoolean(holder.kitchenItemName.getContext().getString(R.string.unit_is_imperial_def))), current.getUnit());
                } else {
                    unit = unitsHelper.convertToAbbreviation(sharedPref.getBoolean(holder.kitchenItemName.getContext().getString(R.string.unit_is_imperial), Boolean.parseBoolean(holder.kitchenItemName.getContext().getString(R.string.unit_is_imperial_def))), current.getUnit());
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

            holder.kitchenItemName.setText(sb);

            if ((current.getLocation() == null || current.getLocation().equals("Other")) && current.getExpiryDate() == null) {
                holder.kitchenItemName.setMaxLines(2);
            } else {
                holder.kitchenItemName.setMaxLines(1);
            }

            // Location
            if (current.getLocation() == null || current.getLocation().equals("Other")) {
                holder.kitchenItemLocation.setVisibility(View.GONE);

            } else {
                holder.kitchenItemLocation.setText(current.getLocation());
                holder.kitchenItemLocation.setVisibility(View.VISIBLE);
            }

            // Expiry Date
            if (current.getExpiryDate() == null) {
                holder.kitchenItemExpiryDate.setVisibility(View.GONE);
            } else {
                holder.kitchenItemExpiryDate.setText(current.getExpiryDate());
                holder.kitchenItemExpiryDate.setVisibility(View.VISIBLE);
            }

            try {
                String[] expiryDateItemAsArray = holder.kitchenItemExpiryDate.getText().toString().split("/");
                Calendar itemCalendar = Calendar.getInstance();
                itemCalendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(expiryDateItemAsArray[0]));
                itemCalendar.set(Calendar.MONTH, Integer.parseInt(expiryDateItemAsArray[1]));
                itemCalendar.set(Calendar.YEAR, Integer.parseInt(expiryDateItemAsArray[2]));

                LocalDate itemDate = LocalDate.of(itemCalendar.get(Calendar.YEAR), itemCalendar.get(Calendar.MONTH), itemCalendar.get(Calendar.DAY_OF_MONTH));

                Calendar today = Calendar.getInstance();
                LocalDate todayDate = LocalDate.of(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH));

                Calendar oneDayFromToday = Calendar.getInstance();
                oneDayFromToday.add(Calendar.DATE, 1);
                LocalDate oneDayLimit = LocalDate.of(oneDayFromToday.get(Calendar.YEAR), oneDayFromToday.get(Calendar.MONTH) + 1, oneDayFromToday.get(Calendar.DAY_OF_MONTH));

                Calendar oneWeekFromToday = Calendar.getInstance();
                oneWeekFromToday.add(Calendar.DATE, 7);
                LocalDate oneWeekLimit = LocalDate.of(oneWeekFromToday.get(Calendar.YEAR), oneWeekFromToday.get(Calendar.MONTH) + 1, oneWeekFromToday.get(Calendar.DAY_OF_MONTH));

                if (itemDate.isBefore(todayDate) || itemDate.isEqual(todayDate)) {
                    holder.kitchenItemExpiryDate.setTextColor(holder.kitchenItemExpiryDate.getContext().getColor(R.color.expiry_past));
                    holder.kitchenItemExpiryDate.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                } else if (itemDate.isAfter(todayDate) && itemDate.isEqual(oneDayLimit)) {
                    holder.kitchenItemExpiryDate.setTextColor(holder.kitchenItemExpiryDate.getContext().getColor(R.color.expiry_one_day));
                    holder.kitchenItemExpiryDate.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                } else if (itemDate.isAfter(oneDayLimit) && itemDate.isBefore(oneWeekLimit)) {
                    holder.kitchenItemExpiryDate.setTextColor(holder.kitchenItemExpiryDate.getContext().getColor(R.color.expiry_one_week));
                } else if (itemDate.isAfter(oneWeekLimit) || itemDate.isEqual(oneWeekLimit)) {
                    holder.kitchenItemExpiryDate.setTextColor(holder.kitchenItemExpiryDate.getContext().getColor(R.color.expiry_safe));
                }

            } catch (Exception ignore) {
                holder.kitchenItemExpiryDate.setTextColor(holder.kitchenItemExpiryDate.getContext().getColor(R.color.black));
            }
        }
    }

    // get number of items in list - mandatory
    @Override
    public int getItemCount() {
        if (kitchenItems != null)
            return kitchenItems.size();
        else return 0;
    }

    // set items in list
    public void setKitchenItems(Context context, List<KitchenItem> items) {
        this.kitchenItems = new ArrayList<>(items);
        UnitsHelper unitsHelper = new UnitsHelper(context);

        // set correct units (metric/imperial)
        if (sharedPref.getBoolean(context.getString(R.string.unit_is_imperial), Boolean.parseBoolean(context.getString(R.string.unit_is_imperial_def)))) {
            for (int i = 0; i < kitchenItems.size(); i++) {
                KitchenItem ki = kitchenItems.get(i);
                ki.setQuantity(Float.parseFloat(unitsHelper.toImperial(ki.getQuantity(), ki.getUnit())[0]));
                ki.setUnit(unitsHelper.toImperial(ki.getQuantity(), ki.getUnit())[1]);
            }
        } else {
            for (int i = 0; i < kitchenItems.size(); i++) {
                KitchenItem ki = kitchenItems.get(i);
                ki.setQuantity(Float.parseFloat(unitsHelper.toMetric(ki.getQuantity(), ki.getUnit())[0]));
                ki.setUnit(unitsHelper.toMetric(ki.getQuantity(), ki.getUnit())[1]);
            }
        }

        this.itemsCopy = new ArrayList<>(this.kitchenItems);

        notifyDataSetChanged();
    }

    // sort items in list
    public void sortItems(String byAttribute, boolean asc) {

        sortMode = byAttribute;
        sortAsc = asc;

        kitchenItems.sort((o1, o2) -> {
            switch (byAttribute) {
                case "id":
                    if (o1.getId() > o2.getId()) {
                        return 1;
                    } else {
                        return -1;
                    }
                case "name":
                    return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
                case "location":
                    if (o1.getLocation() == null && o2.getLocation() != null) {
                        return "".compareTo(o2.getType());
                    } else if (o1.getLocation() != null && o2.getLocation() == null) {
                        return o1.getLocation().compareTo("");
                    } else if (o1.getLocation() == null && o2.getLocation() == null) {
                        return 0;
                    } else {
                        return o1.getLocation().compareTo(o2.getLocation());
                    }
                case "expiry":

                    String[] o1expiryDateAsArray;
                    String[] o2expiryDateAsArray;

                    if (o1.getExpiryDate() == null) {
                        o1expiryDateAsArray = new String[]{"1", "1", "9999"};
                    } else {
                        o1expiryDateAsArray = o1.getExpiryDate().split("/");
                    }

                    if (o2.getExpiryDate() == null) {
                        o2expiryDateAsArray = new String[]{"1", "1", "9999"};
                    } else {
                        o2expiryDateAsArray = o2.getExpiryDate().split("/");

                    }

                    Calendar o1Calendar = Calendar.getInstance();
                    Calendar o2Calendar = Calendar.getInstance();
                    o1Calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(o1expiryDateAsArray[0]));
                    o1Calendar.set(Calendar.MONTH, Integer.parseInt(o1expiryDateAsArray[1]));
                    o1Calendar.set(Calendar.YEAR, Integer.parseInt(o1expiryDateAsArray[2]));
                    o2Calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(o2expiryDateAsArray[0]));
                    o2Calendar.set(Calendar.MONTH, Integer.parseInt(o2expiryDateAsArray[1]));
                    o2Calendar.set(Calendar.YEAR, Integer.parseInt(o2expiryDateAsArray[2]));

                    return o1Calendar.compareTo(o2Calendar);
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
            Collections.reverse(kitchenItems);
        }
        notifyDataSetChanged();
    }

    // filter by search query
    public void filterByQuery(String query) {

        String queryLower = query.toLowerCase();
        String itemName;

        kitchenItems.clear();
        if (queryLower.isEmpty()) {
            kitchenItems.addAll(itemsCopy);
        } else {
            for (KitchenItem item : itemsCopy) {
                itemName = item.getName().toLowerCase();

                if (itemName.contains(queryLower)) {
                    kitchenItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    // filter by type, expiry, location
    public void filterByChoice() {

        boolean expiryFilterApplicable;
        String itemType;
        String itemLocation;

        kitchenItems.clear();

        if (MainActivity.kitchenTypeWhitelist.isEmpty() && MainActivity.kitchenLocationWhitelist.isEmpty() && (MainActivity.kitchenExpiryLimit == -1 || MainActivity.kitchenExpiryLimit == 31)) {
            kitchenItems.addAll(itemsCopy);
        } else {

            if (MainActivity.kitchenExpiryLimit == -1) {
                MainActivity.kitchenExpiryLimit = 31;
                expiryFilterApplicable = false;
            } else {
                expiryFilterApplicable = true;
            }

            Calendar limitCalendar = Calendar.getInstance();
            limitCalendar.add(Calendar.DATE, MainActivity.kitchenExpiryLimit);

            LocalDate limitDate = LocalDate.of(limitCalendar.get(Calendar.YEAR), limitCalendar.get(Calendar.MONTH) + 1, limitCalendar.get(Calendar.DAY_OF_MONTH));

            for (KitchenItem item : itemsCopy) {

                if (item.getType() == null) {
                    itemType = "Other";
                } else {
                    itemType = item.getType();
                }

                if (item.getLocation() == null) {
                    itemLocation = "Other";
                } else {
                    itemLocation = item.getLocation();
                }

                if (item.getExpiryDate() == null) {
                    if (MainActivity.kitchenTypeWhitelist.contains(itemType) && MainActivity.kitchenLocationWhitelist.contains(itemLocation)) {
                        kitchenItems.add(item);
                    }
                } else {
                    String[] expiryDateItemAsArray = item.getExpiryDate().split("/");

                    Calendar itemCalendar = Calendar.getInstance();
                    itemCalendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(expiryDateItemAsArray[0]));
                    itemCalendar.set(Calendar.MONTH, Integer.parseInt(expiryDateItemAsArray[1]));
                    itemCalendar.set(Calendar.YEAR, Integer.parseInt(expiryDateItemAsArray[2]));

                    LocalDate itemDate = LocalDate.of(itemCalendar.get(Calendar.YEAR), itemCalendar.get(Calendar.MONTH), itemCalendar.get(Calendar.DAY_OF_MONTH));

                    if (MainActivity.kitchenTypeWhitelist.contains(itemType) && MainActivity.kitchenLocationWhitelist.contains(itemLocation) && (itemDate.isBefore(limitDate) || !expiryFilterApplicable)) {
                        kitchenItems.add(item);
                    }
                }


            }
        }
        notifyDataSetChanged();
    }

    // ITEM HOLDER ------------------------------------------------------------------------------ //
    // class for the items in the recyclerview
    public class KitchenItemHolder extends RecyclerView.ViewHolder {
        private final TextView kitchenItemName;
        private final TextView kitchenItemExpiryDate;
        private final TextView kitchenItemLocation;
        private final ImageView kitchenItemIcon;

        private KitchenItemHolder(View itemView) {
            super(itemView);

            kitchenItemName = itemView.findViewById(R.id.kitchen_item_name);
            kitchenItemExpiryDate = itemView.findViewById(R.id.kitchen_item_expiry);
            kitchenItemLocation = itemView.findViewById(R.id.kitchen_item_location);
            kitchenItemIcon = itemView.findViewById(R.id.kitchen_item_icon);

            // dialog that shows when user tries to make <0 of an item
            // on click "DELETE" -> delete item
            // on click "MOVE..." -> delete and add to shopping list
            // on click "Cancel" -> reset quantity and close
            MaterialAlertDialogBuilder tooLowAlert = new MaterialAlertDialogBuilder(itemView.getContext())
                    .setTitle("You've run out of this item!")
                    .setMessage("What do you want to do?")
                    .setIcon(R.drawable.warning_alert_icon)
                    .setPositiveButton(R.string.button_delete, (dialog, which) -> {
                        int position = getLayoutPosition();
                        KitchenItem item = kitchenItems.get(position);
                        viewModel.delete(item);
                        dialog.dismiss();
                        Toast.makeText(itemView.getContext(), R.string.item_deleted, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.button_move_to_shopping, (dialog, which) -> {
                        int position = getLayoutPosition();
                        KitchenItem item = kitchenItems.get(position);

                        ShoppingItem newShoppingItem = new ShoppingItem(item.getName(), -1, null, item.getType(), false);
                        shoppingItemViewModel.insert(newShoppingItem);
                        viewModel.delete(item);

                        Toast.makeText(itemView.getContext(), "Moved to shopping list.", Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                    }).setNeutralButton(R.string.cancel, (dialog, which) -> {
                        dialog.cancel();
                    });

            // on click -> show details
            itemView.setOnClickListener(v -> {

                AppCompatActivity activity = (AppCompatActivity) v.getContext();
                Bundle bundle = new Bundle();
                bundle.putParcelable("item", kitchenItems.get(getLayoutPosition()));

                KitchenListDetailsBottomSheet kitchenDetailsSheet = new KitchenListDetailsBottomSheet();
                kitchenDetailsSheet.setArguments(bundle);
                kitchenDetailsSheet.show(activity.getSupportFragmentManager(), "sheet");

            });

            // decrease quantity listener
            // on click -> decrease quantity
            itemView.findViewById(R.id.kitchen_item_decrease_button).setOnClickListener(v -> {
                KitchenItem item = kitchenItems.get(getAdapterPosition());
                float newQuantity = changeQuantity(item.getQuantity(), true);
                if (newQuantity > 0) {
                    item.setQuantity(newQuantity);
                } else {
                    tooLowAlert.setTitle("You've run out of " + item.getName() + "!");
                    tooLowAlert.create().show();
                }
                viewModel.update(item);
            });

            // increase quantity listener
            // on click -> increase quantity
            itemView.findViewById(R.id.kitchen_item_increase_button).setOnClickListener(v -> {
                KitchenItem item = kitchenItems.get(getLayoutPosition());
                float newQuantity = changeQuantity(item.getQuantity(), false);
                if (newQuantity < 999) {
                    item.setQuantity(newQuantity);
                } else {
                    item.setQuantity(999);
                    Toast.makeText(itemView.getContext(), "Quantity cannot be higher than 999.", Toast.LENGTH_SHORT).show();
                }
                viewModel.update(item);
            });
        }

        // method for "smartly" changing quantity according to initial quantity
        public float changeQuantity(float startQuantity, boolean isMinus) {
            if (startQuantity < 10) {
                if (isMinus) {
                    return (float) (startQuantity - 0.5);
                } else {
                    return (float) (startQuantity + 0.5);
                }
            } else if (startQuantity >= 10 && startQuantity < 20) {
                if (isMinus) {
                    return startQuantity - 1;
                } else {
                    return startQuantity + 1;
                }
            } else if (startQuantity >= 20 && startQuantity < 100) {
                if (isMinus) {
                    return startQuantity - 5;
                } else {
                    return startQuantity + 5;
                }
            } else if (startQuantity >= 100 && startQuantity < 250) {
                if (isMinus) {
                    return startQuantity - 10;
                } else {
                    return startQuantity + 10;
                }
            } else if (startQuantity >= 250 && startQuantity < 1000) {
                if (isMinus) {
                    return startQuantity - 50;
                } else {
                    return startQuantity + 50;
                }
                // -vvv- Shouldn't be reached -vvv- //
            } else {
                if (isMinus) {
                    return startQuantity - 1;
                } else {
                    return startQuantity + 1;
                }
            }
        }
    }
}
