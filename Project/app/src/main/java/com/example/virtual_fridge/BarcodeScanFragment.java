package com.example.virtual_fridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;


// Setup code for camera largely used from https://medium.com/swlh/introduction-to-androids-camerax-with-java-ca384c522c5
// Thanks to M. Van Luke

public class BarcodeScanFragment extends Fragment {

    // used to track whether this sheet is fully open (so as to not keep spawning new ones when the barcode scans continuously
    static boolean resultsOpen = false;

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    BarcodeScanner barcodeScanner = null;

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // initialize views
        View view = inflater.inflate(R.layout.camera_fragment, container, false);

        FloatingActionButton takeImageButton = view.findViewById(R.id.take_image_button);
        takeImageButton.setVisibility(View.INVISIBLE);

        Toolbar toolbar = view.findViewById(R.id.camera_toolbar);
        TextView helperText = view.findViewById(R.id.camera_helper_text);

        MaterialButtonToggleGroup toggleButtons = view.findViewById(R.id.scan_mode_buttons);

        // setting up shared preferences
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        // toolbar setup
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, KitchenListFragment.class, null)
                .commit());

        toggleButtons.findViewById(R.id.barcode_button).setEnabled(false);
        toggleButtons.findViewById(R.id.obj_detect_button).setEnabled(true);

        // on click -> go to object detect mode
        toggleButtons.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (checkedId == R.id.obj_detect_button) {

                // switch to object detect mode
                getParentFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragment_container, ObjectDetectFragment.class, null)
                        .commit();
            }
        });

        // welcome dialog
        // on click "OK" -> close
        MaterialAlertDialogBuilder welcomeDialog = new MaterialAlertDialogBuilder(view.getContext())
                .setTitle("Barcode scan")
                .setMessage("Scans common grocery product barcodes.\n\nPoint at a barcode to scan automatically. Results are not guaranteed to be accurate or complete.")
                .setIcon(R.drawable.camera_icon)
                .setNegativeButton(R.string.ok, (dialog, which) -> {
                    editor.putBoolean(getString(R.string.barcode_welcome_seen), true);
                    editor.apply();
                    dialog.cancel();
                });

        // uncomment below 2 lines for debugging welcome dialog
        //editor.putBoolean(getString(R.string.shopping_welcome_seen), false);
        //editor.apply();
        if (!sharedPref.getBoolean(getString(R.string.barcode_welcome_seen), Boolean.parseBoolean(getString(R.string.barcode_welcome_seen_def)))) {
            welcomeDialog.create().show();
        }

        helperText.setText(getString(R.string.barcode_scan_hint));

        // setting up barcode scanner and camera
        BarcodeScannerOptions barcodeScannerOptions = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E, Barcode.FORMAT_EAN_8, Barcode.FORMAT_EAN_13, Barcode.FORMAT_ITF, Barcode.FORMAT_CODE_93)
                .build();
        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions);

        previewView = view.findViewById(R.id.camera_preview);
        cameraProviderFuture = ProcessCameraProvider.getInstance(view.getContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindImageAnalysis(cameraProvider, barcodeScanner, view);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(view.getContext()));

        return view;
    }

    // barcode processing in this method
    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider, BarcodeScanner scanner, View view) {

        // setting up image analysis mode
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(getContext()), image -> {

            @SuppressLint("UnsafeExperimentalUsageError") Image mediaImage = image.getImage();
            if (mediaImage != null) {
                InputImage imageToAnalyze = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());

                scanner.process(imageToAnalyze)
                        .addOnSuccessListener(
                                barcodes -> {

                                    for (Barcode barcode : barcodes) {

                                        if (!resultsOpen) {

                                            String rawValue = barcode.getRawValue();

                                            GetProduct task = new GetProduct();
                                            task.execute(Long.parseLong(rawValue));

                                            AppCompatActivity activity = (AppCompatActivity) view.getContext();
                                            Bundle bundle = new Bundle();
                                            try {
                                                bundle.putStringArray("scanResult", task.get());
                                            } catch (ExecutionException | InterruptedException e) {
                                                bundle.putStringArray("scanResult", new String[]{"execError", null, null});
                                            }

                                            BarcodeScanResultSheet barcodeScanResultSheet = new BarcodeScanResultSheet();
                                            barcodeScanResultSheet.setArguments(bundle);

                                            barcodeScanResultSheet.show(activity.getSupportFragmentManager(), "sheet");
                                        }
                                    }

                                })
                        .addOnFailureListener(
                                e -> {
                                    Toast.makeText(getContext(), "Error!", Toast.LENGTH_SHORT).show();

                                    // Task failed with an exception
                                    // ...
                                })
                        .addOnCompleteListener(
                                task -> {
                                    image.close();
                                    mediaImage.close();
                                }
                        );
            }
        });

        // showing camera view on screen
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector,
                imageAnalysis, preview);
    }
}

// AsyncTask soon to be @Deprecated
@Deprecated
class GetProduct extends AsyncTask<Long, String[], String[]> {

    @Override
    protected String[] doInBackground(Long... barcodeNum) {

        // [0] is status, i.e. "cannot find barcode" or "success"
        // [1] is name (if successful)
        // [2] is quantity (if successful)
        String[] result = {null, null, null};

        try {
            URL url = new URL("https://world.openfoodfacts.org/api/v0/product/" + Arrays.toString(barcodeNum) + ".json");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.connect();

            if (con.getResponseCode() != 200) {
                result[0] = "badResponse";
            } else {

                Scanner scanner = new Scanner(url.openStream());
                String response = scanner.useDelimiter("\\Z").next();
                JSONObject obj = new JSONObject(response);

                if (obj.getString("status").equals("0")) {
                    result[0] = "prodNotFound";
                } else {
                    JSONObject prodObj = obj.getJSONObject("product");
                    result[0] = "success";

                    if (prodObj.has("product_name_en")) {
                        if (!prodObj.getString("product_name_en").equals("")) {
                            result[1] = prodObj.getString("product_name_en");
                        }
                    } else if (prodObj.has("product_name")) {
                        if (!prodObj.getString("product_name").equals("")) {
                            result[1] = prodObj.getString("product_name");
                        }
                    }

                    if (prodObj.has("generic_name_en")) {
                        if (!prodObj.getString("generic_name_en").equals("")) {
                            result[1] = prodObj.getString("generic_name_en");
                        }
                    } else if (prodObj.has("generic_name")) {
                        if (!prodObj.getString("generic_name").equals("")) {
                            result[1] = prodObj.getString("generic_name");
                        }
                    }

                    result[2] = "quantityNotFound";

                    if (prodObj.has("quantity")) {
                        if (!prodObj.getString("quantity").equals("")) {
                            result[2] = prodObj.getString("quantity");
                        }
                    }
                }

                scanner.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    protected void onPostExecute(String[] name) {
        // done
    }
}