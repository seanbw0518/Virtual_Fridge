package com.example.virtual_fridge;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class ShoppingItemRepo {
    private final ShoppingItemDao shoppingItemDao;
    private final LiveData<List<ShoppingItem>> allShoppingItems;

    // constructor
    ShoppingItemRepo(Application application) {
        MainDatabase db = MainDatabase.getDatabase(application);
        shoppingItemDao = db.shoppingItemDao();
        allShoppingItems = shoppingItemDao.getShoppingItemsById();
    }

    // returns all items in db as LiveData
    LiveData<List<ShoppingItem>> getAllShoppingItems() {
        return allShoppingItems;
    }

    // inserts item into db using dao
    void insert(ShoppingItem shoppingItem) {
        MainDatabase.databaseWriteExecutor.execute(() -> {
            shoppingItemDao.insert(shoppingItem);
        });
    }

    // deletes item from db using dao
    void delete(ShoppingItem shoppingItem) {
        MainDatabase.databaseWriteExecutor.execute(() -> {
            shoppingItemDao.delete(shoppingItem);
        });
    }

    // updates item in db using dao
    void update(ShoppingItem shoppingItem) {
        MainDatabase.databaseWriteExecutor.execute(() -> {
            shoppingItemDao.update(shoppingItem);
        });
    }
}
