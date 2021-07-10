package com.example.virtual_fridge;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class KitchenItemViewModel extends AndroidViewModel {
    private final KitchenItemRepo kitchenItemRepo;
    private final LiveData<List<KitchenItem>> allKitchenItems;

    // constructor
    public KitchenItemViewModel(Application application) {
        super(application);
        kitchenItemRepo = new KitchenItemRepo(application);
        allKitchenItems = kitchenItemRepo.getAllKitchenItems();
    }

    // gets all the items stored in repository, but as regular list
    List<KitchenItem> getAllKitchenItemsList() {
        return allKitchenItems.getValue();
    }

    // gets all the items stored in repository, as LiveData
    LiveData<List<KitchenItem>> getAllKitchenItems() {
        return allKitchenItems;
    }

    // tells repository to insert an item
    public void insert(KitchenItem kitchenItem) {
        kitchenItemRepo.insert(kitchenItem);
    }

    // tells repository to delete an item
    public void delete(KitchenItem kitchenItem) {
        kitchenItemRepo.delete(kitchenItem);
    }

    // tells repository to update an item
    public void update(KitchenItem kitchenItem) {
        kitchenItemRepo.update(kitchenItem);
    }
}
