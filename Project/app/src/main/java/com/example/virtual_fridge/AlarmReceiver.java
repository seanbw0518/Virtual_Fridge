package com.example.virtual_fridge;

import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// class used to alarm the notifications to call every 24 hours
public class AlarmReceiver extends BroadcastReceiver {

    private static final int NOTIFY_ONE_DAY = 0;
    private static final int NOTIFY_TWO_DAY = 1;
    private static final int NOTIFY_ONE_WEEK = 2;

    public static SharedPreferences sharedPref;

    @Override
    public void onReceive(Context context, Intent intent) {

        UnitsHelper unitsHelper = new UnitsHelper(context);

        // thread to get items from database via repository (off of ui thread)
        new Thread(() -> {
            ArrayList<KitchenItem> oneDayItems = new ArrayList<>();
            ArrayList<KitchenItem> twoDayItems = new ArrayList<>();
            ArrayList<KitchenItem> oneWeekItems = new ArrayList<>();

            KitchenItemRepo repo = new KitchenItemRepo((Application) context.getApplicationContext());
            List<KitchenItem> items = repo.getAllKitchenItemsAsList();

            String exp;

            for (KitchenItem item : items) {
                exp = item.getExpiryDate();

                // Setting up dates for comparison
                if (exp != null) {
                    String[] expiryDateAsArray = exp.split("/");
                    Calendar itemCalendar = Calendar.getInstance();
                    itemCalendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(expiryDateAsArray[0]));
                    itemCalendar.set(Calendar.MONTH, Integer.parseInt(expiryDateAsArray[1]));
                    itemCalendar.set(Calendar.YEAR, Integer.parseInt(expiryDateAsArray[2]));

                    LocalDate itemDate = LocalDate.of(itemCalendar.get(Calendar.YEAR), itemCalendar.get(Calendar.MONTH), itemCalendar.get(Calendar.DAY_OF_MONTH));

                    Calendar today = Calendar.getInstance();
                    LocalDate todayDate = LocalDate.of(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH));

                    Calendar oneDayFromToday = Calendar.getInstance();
                    oneDayFromToday.add(Calendar.DATE, 1);
                    LocalDate oneDayLimit = LocalDate.of(oneDayFromToday.get(Calendar.YEAR), oneDayFromToday.get(Calendar.MONTH) + 1, oneDayFromToday.get(Calendar.DAY_OF_MONTH));

                    Calendar oneWeekFromToday = Calendar.getInstance();
                    oneWeekFromToday.add(Calendar.DATE, 7);
                    LocalDate oneWeekLimit = LocalDate.of(oneWeekFromToday.get(Calendar.YEAR), oneWeekFromToday.get(Calendar.MONTH) + 1, oneWeekFromToday.get(Calendar.DAY_OF_MONTH));

                    if (itemDate.isEqual(todayDate)) {
                        // expires in <=1 day
                        oneDayItems.add(item);
                    } else if (itemDate.isEqual(oneDayLimit)) {
                        // expires in <=2 days
                        twoDayItems.add(item);
                    } else if (itemDate.isEqual(oneWeekLimit)) {
                        // expires in <= 1 week
                        oneWeekItems.add(item);
                    }
                }
            }

            // Notifications
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            Intent startAppIntent = new Intent(context, MainActivity.class);
            PendingIntent startAppPendingIntent = PendingIntent.getActivity(context, 0, startAppIntent, 0);

            // One-Day Notification
            if (sharedPref.getBoolean(context.getString(R.string.one_day_notifs_active), Boolean.parseBoolean(context.getString(R.string.one_day_notifs_active_def)))) {
                NotificationCompat.Builder notifyOneDay = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                        .setSmallIcon(R.drawable.apple_apples)
                        .setColor(context.getColor(R.color.colorPrimary))
                        .setContentIntent(startAppPendingIntent)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                if (oneDayItems.size() > 1) {
                    notifyOneDay.setContentTitle("Items expiring soon!");
                    notifyOneDay.setContentText("Multiple items in the kitchen expire today.");

                    Intent addMultipleToShoppingIntentOneDay = new Intent(context, NotificationBroadcastReceiver.class);
                    addMultipleToShoppingIntentOneDay.setAction("insertAllOneDay");

                    addMultipleToShoppingIntentOneDay.putParcelableArrayListExtra("itemsOneDay", oneDayItems);
                    addMultipleToShoppingIntentOneDay.putExtra("notifyIdOneDay", NOTIFY_ONE_DAY);

                    PendingIntent addMultipleToShoppingPendingIntentOneDay = PendingIntent.getBroadcast(context, 0, addMultipleToShoppingIntentOneDay, PendingIntent.FLAG_CANCEL_CURRENT);

                    notifyOneDay.addAction(R.drawable.cart_icon, context.getString(R.string.button_add_all_to_shopping), addMultipleToShoppingPendingIntentOneDay);

                    notificationManager.notify(NOTIFY_ONE_DAY, notifyOneDay.build());
                } else if (oneDayItems.size() == 1) {
                    KitchenItem item = oneDayItems.get(0);
                    notifyOneDay.setContentTitle("Your " + "\"" + item.getName() + "\"" + " expires soon!");
                    notifyOneDay.setContentText(buildMessage(context, unitsHelper, item.getName(), item.getQuantity(), item.getUnit(), item.getLocation(), 1));
                    notifyOneDay.setStyle(new NotificationCompat.BigTextStyle().bigText(buildMessage(context, unitsHelper, item.getName(), item.getQuantity(), item.getUnit(), item.getLocation(), 1)));

                    Intent addToShoppingIntentOneDay = new Intent(context, NotificationBroadcastReceiver.class);
                    addToShoppingIntentOneDay.setAction("insertOneDay");
                    addToShoppingIntentOneDay.putExtra("itemOneDay", item);
                    addToShoppingIntentOneDay.putExtra("notifyIdOneDay", NOTIFY_ONE_DAY);
                    PendingIntent addToShoppingPendingIntentOneDay = PendingIntent.getBroadcast(context, 0, addToShoppingIntentOneDay, PendingIntent.FLAG_CANCEL_CURRENT);

                    notifyOneDay.addAction(R.drawable.cart_icon, context.getString(R.string.button_add_to_shopping), addToShoppingPendingIntentOneDay);

                    notificationManager.notify(NOTIFY_ONE_DAY, notifyOneDay.build());
                }
            }

            // Two-Day Notification
            if (sharedPref.getBoolean(context.getString(R.string.two_day_notifs_active), Boolean.parseBoolean(context.getString(R.string.two_day_notifs_active_def)))) {
                NotificationCompat.Builder notifyTwoDay = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                        .setSmallIcon(R.drawable.apple_apples)
                        .setColor(context.getColor(R.color.colorPrimary))
                        .setContentIntent(startAppPendingIntent)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                if (twoDayItems.size() > 1) {
                    notifyTwoDay.setContentTitle("Items expiring soon!");
                    notifyTwoDay.setContentText("Multiple items in the kitchen expire tomorrow.");

                    Intent addMultipleToShoppingIntentTwoDay = new Intent(context, NotificationBroadcastReceiver.class);
                    addMultipleToShoppingIntentTwoDay.setAction("insertAllTwoDay");

                    addMultipleToShoppingIntentTwoDay.putParcelableArrayListExtra("itemsTwoDay", twoDayItems);
                    addMultipleToShoppingIntentTwoDay.putExtra("notifyIdTwoDay", NOTIFY_TWO_DAY);

                    PendingIntent addMultipleToShoppingPendingIntentTwoDay = PendingIntent.getBroadcast(context, 0, addMultipleToShoppingIntentTwoDay, PendingIntent.FLAG_CANCEL_CURRENT);

                    notifyTwoDay.addAction(R.drawable.cart_icon, context.getString(R.string.button_add_all_to_shopping), addMultipleToShoppingPendingIntentTwoDay);

                    notificationManager.notify(NOTIFY_TWO_DAY, notifyTwoDay.build());

                } else if (twoDayItems.size() == 1) {
                    KitchenItem item = twoDayItems.get(0);
                    notifyTwoDay.setContentTitle("Your " + "\"" + item.getName() + "\"" + " expires soon!");
                    notifyTwoDay.setContentText(buildMessage(context, unitsHelper, item.getName(), item.getQuantity(), item.getUnit(), item.getLocation(), 2));
                    notifyTwoDay.setStyle(new NotificationCompat.BigTextStyle().bigText(buildMessage(context, unitsHelper, item.getName(), item.getQuantity(), item.getUnit(), item.getLocation(), 2)));

                    Intent addToShoppingIntentTwoDay = new Intent(context, NotificationBroadcastReceiver.class);
                    addToShoppingIntentTwoDay.setAction("insertTwoDay");
                    addToShoppingIntentTwoDay.putExtra("itemTwoDay", item);
                    addToShoppingIntentTwoDay.putExtra("notifyIdTwoDay", NOTIFY_TWO_DAY);
                    PendingIntent addToShoppingPendingIntentTwoDay = PendingIntent.getBroadcast(context, 0, addToShoppingIntentTwoDay, PendingIntent.FLAG_CANCEL_CURRENT);

                    notifyTwoDay.addAction(R.drawable.cart_icon, context.getString(R.string.button_add_to_shopping), addToShoppingPendingIntentTwoDay);

                    notificationManager.notify(NOTIFY_TWO_DAY, notifyTwoDay.build());

                }
            }

            // One-Week Notification
            if (sharedPref.getBoolean(context.getString(R.string.one_week_notifs_active), Boolean.parseBoolean(context.getString(R.string.one_week_notifs_active_def)))) {
                NotificationCompat.Builder notifyOneWeek = new NotificationCompat.Builder(context, MainActivity.CHANNEL_ID)
                        .setSmallIcon(R.drawable.apple_apples)
                        .setColor(context.getColor(R.color.colorPrimary))
                        .setContentIntent(startAppPendingIntent)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                if (oneWeekItems.size() > 1) {
                    notifyOneWeek.setContentTitle("Items expiring soon!");
                    notifyOneWeek.setContentText("Multiple items in the kitchen expire in a week.");

                    Intent addMultipleToShoppingIntentOneWeek = new Intent(context, NotificationBroadcastReceiver.class);
                    addMultipleToShoppingIntentOneWeek.setAction("insertAllOneWeek");

                    addMultipleToShoppingIntentOneWeek.putParcelableArrayListExtra("itemsOneWeek", oneWeekItems);
                    addMultipleToShoppingIntentOneWeek.putExtra("notifyIdOneWeek", NOTIFY_ONE_WEEK);

                    PendingIntent addMultipleToShoppingPendingIntentOneWeek = PendingIntent.getBroadcast(context, 0, addMultipleToShoppingIntentOneWeek, PendingIntent.FLAG_CANCEL_CURRENT);

                    notifyOneWeek.addAction(R.drawable.cart_icon, context.getString(R.string.button_add_all_to_shopping), addMultipleToShoppingPendingIntentOneWeek);

                    notificationManager.notify(NOTIFY_ONE_WEEK, notifyOneWeek.build());

                } else if (oneWeekItems.size() == 1) {
                    KitchenItem item = oneWeekItems.get(0);
                    notifyOneWeek.setContentTitle("Your " + "\"" + item.getName() + "\"" + " expires in a week.");
                    notifyOneWeek.setContentText(buildMessage(context, unitsHelper, item.getName(), item.getQuantity(), item.getUnit(), item.getLocation(), 7));
                    notifyOneWeek.setStyle(new NotificationCompat.BigTextStyle().bigText(buildMessage(context, unitsHelper, item.getName(), item.getQuantity(), item.getUnit(), item.getLocation(), 7)));

                    Intent addToShoppingIntentOneWeek = new Intent(context, NotificationBroadcastReceiver.class);
                    addToShoppingIntentOneWeek.setAction("insertOneWeek");
                    addToShoppingIntentOneWeek.putExtra("itemOneWeek", item);
                    addToShoppingIntentOneWeek.putExtra("notifyIdOneWeek", NOTIFY_ONE_WEEK);
                    PendingIntent addToShoppingPendingIntentOneWeek = PendingIntent.getBroadcast(context, 0, addToShoppingIntentOneWeek, PendingIntent.FLAG_CANCEL_CURRENT);

                    notifyOneWeek.addAction(R.drawable.cart_icon, context.getString(R.string.button_add_to_shopping), addToShoppingPendingIntentOneWeek);

                    notificationManager.notify(NOTIFY_ONE_WEEK, notifyOneWeek.build());

                }
            }

        }).start();
    }

    // method to build the message displayed on the notification based on what the item is and when it expires
    public String buildMessage(Context context, UnitsHelper unitsHelper, String name, float quantity, String unit, String location, int daysUntilExpiry) {

        String expiryDays;
        String quantityUnit;

        switch (daysUntilExpiry) {
            case (1):
                expiryDays = " today.";
                break;
            case (2):
                expiryDays = " tomorrow.";
                break;
            case (7):
                expiryDays = " in a week.";
                break;
            default:
                expiryDays = " in " + daysUntilExpiry + " days.";
                break;
        }

        if (unit == null || unit.equals("")) {
            quantityUnit = unit;
        } else {
            quantityUnit = quantity + unitsHelper.convertToAbbreviation(sharedPref.getBoolean(context.getString(R.string.unit_is_imperial), Boolean.parseBoolean(context.getString(R.string.unit_is_imperial_def))), unit) + " ";
        }


        if (location.equals("Other")) {
            return "Your " + quantityUnit + name + " expires" + expiryDays;
        } else {
            return "Your " + quantityUnit + name + " in the " + location + " expires" + expiryDays;
        }
    }
}
