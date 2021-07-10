package com.example.virtual_fridge;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;

// handles notifications
public class NotificationBroadcastReceiver extends BroadcastReceiver {

    KitchenItem itemOneDay;
    KitchenItem itemTwoDay;
    KitchenItem itemOneWeek;

    ArrayList<KitchenItem> itemsOneDay;
    ArrayList<KitchenItem> itemsTwoDay;
    ArrayList<KitchenItem> itemsOneWeek;

    int notifyIdOneDay;
    int notifyIdTwoDay;
    int notifyIdOneWeek;

    @Override
    public void onReceive(Context context, Intent intent) {

        new Thread(() -> {

            // getting information from Alarm
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            ShoppingItemRepo shoppingItemRepo = new ShoppingItemRepo((Application) context.getApplicationContext());

            String action = intent.getAction();

            itemOneDay = intent.getParcelableExtra("itemOneDay");
            itemTwoDay = intent.getParcelableExtra("itemTwoDay");
            itemOneWeek = intent.getParcelableExtra("itemOneWeek");

            itemsOneDay = intent.getParcelableArrayListExtra("itemsOneDay");
            itemsTwoDay = intent.getParcelableArrayListExtra("itemsTwoDay");
            itemsOneWeek = intent.getParcelableArrayListExtra("itemsOneWeek");

            notifyIdOneDay = intent.getIntExtra("notifyIdOneDay", -1);
            notifyIdTwoDay = intent.getIntExtra("notifyIdTwoDay", -1);
            notifyIdOneWeek = intent.getIntExtra("notifyIdOneWeek", -1);

            // manage different button presses
            if (action != null) {
                switch (action) {
                    case ("insertOneDay"):
                        shoppingItemRepo.insert(new ShoppingItem(itemOneDay.getName(), -1, null, itemOneDay.getType()));
                        notificationManager.cancel(notifyIdOneDay);
                        break;
                    case ("insertTwoDay"):
                        shoppingItemRepo.insert(new ShoppingItem(itemTwoDay.getName(), -1, null, itemTwoDay.getType()));
                        notificationManager.cancel(notifyIdTwoDay);
                        break;
                    case ("insertOneWeek"):
                        shoppingItemRepo.insert(new ShoppingItem(itemOneWeek.getName(), -1, null, itemOneWeek.getType()));
                        notificationManager.cancel(notifyIdOneWeek);
                        break;
                    case ("insertAllOneDay"):
                        for (KitchenItem item : itemsOneDay) {
                            shoppingItemRepo.insert(new ShoppingItem(item.getName(), -1, null, item.getType()));
                        }
                        notificationManager.cancel(notifyIdOneDay);
                        break;
                    case ("insertAllTwoDay"):
                        for (KitchenItem item : itemsTwoDay) {
                            shoppingItemRepo.insert(new ShoppingItem(item.getName(), -1, null, item.getType()));
                        }
                        notificationManager.cancel(notifyIdTwoDay);
                        break;
                    case ("insertAllOneWeek"):
                        for (KitchenItem item : itemsOneWeek) {
                            shoppingItemRepo.insert(new ShoppingItem(item.getName(), -1, null, item.getType()));
                        }
                        notificationManager.cancel(notifyIdOneWeek);
                        break;
                }
            }
        }).start();
    }
}