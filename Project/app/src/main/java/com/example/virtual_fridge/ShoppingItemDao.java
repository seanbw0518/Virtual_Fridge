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
public interface ShoppingItemDao {

    // Add new item
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ShoppingItem shoppingItem);

    // Delete one item
    @Delete
    void delete(ShoppingItem shoppingItem);

    // Update one item
    @Update(onConflict = OnConflictStrategy.IGNORE)
    void update(ShoppingItem shoppingItem);

    // Delete all items
    @Query("DELETE FROM kitchen_list_table")
    void deleteAll();

    // QUERIES---:

    // Find ALL ORDER BY ID
    @Query("SELECT * FROM shopping_list_table ORDER BY id ASC")
    LiveData<List<ShoppingItem>> getShoppingItemsById();
}
