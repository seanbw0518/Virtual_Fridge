package com.example.virtual_fridge;

import javax.inject.Singleton;

// class to manage bytes shared between UI thread and separate thread using Clarifai API
@Singleton
class SharedThreadData {

    public byte[] bytes;

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
