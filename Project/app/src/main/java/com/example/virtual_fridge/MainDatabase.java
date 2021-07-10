package com.example.virtual_fridge;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {KitchenItem.class, ShoppingItem.class}, version = 1)
public abstract class MainDatabase extends RoomDatabase {
    public abstract KitchenItemDao kitchenItemDao();

    public abstract ShoppingItemDao shoppingItemDao();

    private static volatile MainDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // -- Below code used to migrate database to new version if changes are made to the schema
    /*
    static final Migration MIGRATE_UP = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Any changes to database goes here
        }
    };
     */

    static MainDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (MainDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            MainDatabase.class, "lists_database")
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            // If you want to keep data through app restart comment out the following block
            /*
            databaseWriteExecutor.execute(() -> {
                KitchenItemDao kitchenItemDao = INSTANCE.kitchenItemDao();
                kitchenItemDao.deleteAll();
            });
             */
        }

    };


}
