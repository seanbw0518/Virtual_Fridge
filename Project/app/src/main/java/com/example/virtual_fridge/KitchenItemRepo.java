package com.example.virtual_fridge;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class KitchenItemRepo {
    private final KitchenItemDao kitchenItemDao;
    private final LiveData<List<KitchenItem>> allKitchenItems;

    // constructor
    KitchenItemRepo(Application application) {
        MainDatabase db = MainDatabase.getDatabase(application);
        kitchenItemDao = db.kitchenItemDao();
        allKitchenItems = kitchenItemDao.getKitchenItemsById();
    }

    // returns all items in db as LiveData
    LiveData<List<KitchenItem>> getAllKitchenItems() {
        return allKitchenItems;
    }

    // returns all items in db as List
    List<KitchenItem> getAllKitchenItemsAsList() {
        return kitchenItemDao.getKitchenItemsByIdAsList();
    }

    // inserts item into db using dao
    void insert(KitchenItem kitchenItem) {
        MainDatabase.databaseWriteExecutor.execute(() -> {
            kitchenItemDao.insert(kitchenItem);
        });
    }

    // deletes item from db using dao
    void delete(KitchenItem kitchenItem) {
        MainDatabase.databaseWriteExecutor.execute(() -> {
            kitchenItemDao.delete(kitchenItem);
        });
    }

    // updates item in db using dao
    void update(KitchenItem kitchenItem) {
        MainDatabase.databaseWriteExecutor.execute(() -> {
            kitchenItemDao.update(kitchenItem);
        });
    }
}
