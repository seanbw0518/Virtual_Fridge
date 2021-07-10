package com.example.virtual_fridge;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class ShoppingItemViewModel extends AndroidViewModel {
    private final ShoppingItemRepo shoppingItemRepo;
    private final LiveData<List<ShoppingItem>> allShoppingItems;

    // constructor
    public ShoppingItemViewModel(Application application) {
        super(application);
        shoppingItemRepo = new ShoppingItemRepo(application);
        allShoppingItems = shoppingItemRepo.getAllShoppingItems();
    }

    // gets all the items stored in repository, as LiveData
    LiveData<List<ShoppingItem>> getAllShoppingItems() {
        return allShoppingItems;
    }

    // gets all the items stored in repository, but as regular list
    List<ShoppingItem> getAllShoppingItemsList() {
        return allShoppingItems.getValue();
    }

    // tells repository to insert an item
    public void insert(ShoppingItem shoppingItem) {
        shoppingItemRepo.insert(shoppingItem);
    }

    // tells repository to delete an item
    public void delete(ShoppingItem shoppingItem) {
        shoppingItemRepo.delete(shoppingItem);
    }

    // tells repository to update an item
    public void update(ShoppingItem shoppingItem) {
        shoppingItemRepo.update(shoppingItem);
    }
}
