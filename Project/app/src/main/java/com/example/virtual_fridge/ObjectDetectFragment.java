package com.example.virtual_fridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.clarifai.channel.ClarifaiChannel;
import com.clarifai.credentials.ClarifaiCallCredentials;
import com.clarifai.grpc.api.Concept;
import com.clarifai.grpc.api.Data;
import com.clarifai.grpc.api.Input;
import com.clarifai.grpc.api.MultiOutputResponse;
import com.clarifai.grpc.api.Output;
import com.clarifai.grpc.api.PostModelOutputsRequest;
import com.clarifai.grpc.api.V2Grpc;
import com.clarifai.grpc.api.status.StatusCode;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.grpc.Channel;

public class ObjectDetectFragment extends Fragment {

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    // for Clarifai API
    private final String API_KEY = "2050bc7a52c14288aae711c7659269f8";

    SharedThreadData sharedThreadData;

    public static FloatingActionButton takeImageButton;

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // initializing views
        View view = inflater.inflate(R.layout.camera_fragment, container, false);

        takeImageButton = view.findViewById(R.id.take_image_button);
        takeImageButton.setVisibility(View.VISIBLE);

        Toolbar toolbar = view.findViewById(R.id.camera_toolbar);
        TextView helperText = view.findViewById(R.id.camera_helper_text);

        MaterialButtonToggleGroup toggleButtons = view.findViewById(R.id.scan_mode_buttons);

        previewView = view.findViewById(R.id.camera_preview);

        // setting up shared preferences
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        sharedThreadData = new SharedThreadData();

        // toolbar setup
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, KitchenListFragment.class, null)
                .commit());

        // toggle buttons setup
        toggleButtons.findViewById(R.id.obj_detect_button).setEnabled(false);
        toggleButtons.findViewById(R.id.barcode_button).setEnabled(true);

        // on click -> switch to barcode scan mode
        toggleButtons.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (checkedId == R.id.barcode_button) {

                // switch to barcode mode
                getParentFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container, BarcodeScanFragment.class, null)
                        .commit();
            }
        });

        // welcome dialog
        // on click "OK" -> close dialog
        MaterialAlertDialogBuilder welcomeDialog = new MaterialAlertDialogBuilder(view.getContext())
                .setTitle("Food Detection")
                .setMessage("Recognizes up to 5 foods at once without barcodes.\n\nPoint at some food and press the camera button at the bottom of the screen to analyze the image.\n" +
                        "For best results, use in bright lighting with a blank background.")
                .setIcon(R.drawable.camera_icon)
                .setNegativeButton(R.string.ok, (dialog, which) -> {
                    editor.putBoolean(getString(R.string.obj_detect_welcome_seen), true);
                    editor.apply();
                    dialog.cancel();
                });

        // uncomment below for debugging welcome dialog
        //editor.putBoolean(getString(R.string.obj_detect_welcome_seen), false);
        //editor.apply();
        if (!sharedPref.getBoolean(getString(R.string.obj_detect_welcome_seen), Boolean.parseBoolean(getString(R.string.obj_detect_welcome_seen_def)))) {
            welcomeDialog.create().show();
        }

        helperText.setText(getString(R.string.obj_detect_hint));

        // setting up camera
        cameraProviderFuture = ProcessCameraProvider.getInstance(view.getContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider, takeImageButton, view);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(view.getContext()));

        return view;
    }

    // camera processing in this method
    @SuppressLint("UnsafeExperimentalUsageError")
    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider, FloatingActionButton takeImageButton, View view) {

        // Setting up camera preview & analysis
        Preview preview = new Preview.Builder().build();
        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, analysis);

        ExecutorService analysisExecutor = Executors.newSingleThreadExecutor();

        // on click -> do analysis
        takeImageButton.setOnClickListener(v -> {
            takeImageButton.setEnabled(false);
            takeImageButton.setSelected(true);
            takeImageButton.setImageResource(R.drawable.processing_icon);
            analysis.setAnalyzer(analysisExecutor, imageProxy -> {

                sharedThreadData.setBytes(getBytes(imageProxy.getImage()));

                List<Concept> predictions;

                // thread to use API
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<List<Concept>> result = executor.submit(() -> {

                    // setup for Clarifai
                    String MODEL_ID = "bd367be194cf45149e75f01d59f77ba7";
                    Channel channel = ClarifaiChannel.INSTANCE.getJsonChannel();
                    V2Grpc.V2BlockingStub stub = V2Grpc.newBlockingStub(channel)
                            .withCallCredentials(new ClarifaiCallCredentials(API_KEY));

                    MultiOutputResponse postModelOutputsResponse = stub.postModelOutputs(
                            PostModelOutputsRequest.newBuilder()
                                    .setModelId(MODEL_ID)
                                    .addInputs(
                                            Input.newBuilder().setData(
                                                    Data.newBuilder().setImage(
                                                            com.clarifai.grpc.api.Image.newBuilder()
                                                                    .setBase64(ByteString.copyFrom(sharedThreadData.getBytes()))
                                                    )
                                            )
                                    )
                                    .build()
                    );

                    if (postModelOutputsResponse.getStatus().getCode() != StatusCode.SUCCESS) {
                        throw new RuntimeException("Post model outputs failed, status: " + postModelOutputsResponse.getStatus());
                    }

                    Output output = postModelOutputsResponse.getOutputs(0);

                    return output.getData().getConceptsList();
                });

                AppCompatActivity activity = (AppCompatActivity) view.getContext();
                Bundle bundle = new Bundle();

                ArrayList<String> predictionsList = new ArrayList<>();
                List<String> tooGeneric = Arrays.asList("vegetable", "pasture", "barbecue", "bird", "cooked meat", "dairy", "dairy product", "feast",
                        "fowl", "gastronomy", "legume", "meat", "sauce", "seafood", "shellfish", "spatula", "unleavened");

                // getting results from API and converting to usable format to send to sheet
                try {
                    predictions = result.get();

                    if (predictions != null && !predictions.isEmpty()) {

                        for (int i = 0; i < predictions.size(); i++) {

                            if (predictionsList.size() == 5) {
                                break;
                            } else {

                                if (tooGeneric.contains(predictions.get(i).getName())) {
                                    Log.i("", "generic !");
                                } else {
                                    predictionsList.add(predictions.get(i).getName());
                                }
                            }
                        }

                        bundle.putStringArrayList("scanResult", predictionsList);
                    }

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }

                ObjDetectResultSheet objDetectResultSheet = new ObjDetectResultSheet();
                objDetectResultSheet.setArguments(bundle);
                objDetectResultSheet.show(activity.getSupportFragmentManager(), "sheet");

                imageProxy.close();
                analysis.clearAnalyzer();
            });
        });
    }

    // All credit for method to Mike A & Ahwar at https://stackoverflow.com/questions/56772967/converting-imageproxy-to-bitmap/56812799#56812799
    public byte[] getBytes(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);

        return out.toByteArray();
    }
}