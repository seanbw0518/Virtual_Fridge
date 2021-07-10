package com.example.virtual_fridge;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface KitchenItemDao {

    // Add new item
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(KitchenItem kitchenItem);

    // Delete one item
    @Delete
    void delete(KitchenItem kitchenItem);

    // Update one item
    @Update(onConflict = OnConflictStrategy.IGNORE)
    void update(KitchenItem kitchenItem);

    // Delete all items
    @Query("DELETE FROM kitchen_list_table")
    void deleteAll();

    // QUERIES---:

    // Find ALL ORDER BY ID
    @Query("SELECT * FROM kitchen_list_table ORDER BY id ASC")
    LiveData<List<KitchenItem>> getKitchenItemsById();

    // Find ALL ORDER BY ID (as list)
    @Query("SELECT * FROM kitchen_list_table ORDER BY id ASC")
    List<KitchenItem> getKitchenItemsByIdAsList();
}
