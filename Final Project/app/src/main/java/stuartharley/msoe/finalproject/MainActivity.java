/*
 * Course: SE4910-011
 * Winter 2021
 * Lab: Final Project
 * Author: Stuart Harley
 * Created: 2/5/2022
 */

package stuartharley.msoe.finalproject;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Surface;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseStorage mFirebaseStorage;

    private ImageView imageView;
    private TextView resultsTextView;
    private Uri imagePath;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION = 0;
    public final static int ACTION_REQUEST_GALLERY = 1046;
    private static boolean imagePresent = false;

    private Classifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFirebaseStorage = FirebaseStorage.getInstance();
        imageView = findViewById(R.id.imageView);
        resultsTextView = findViewById(R.id.resultsTextView);
        try {
            classifier = new Classifier(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        findViewById(R.id.uploadButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPhoto();
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "uploadButton");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            }
        });
        findViewById(R.id.saveButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "saveButton");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                handleSavePhoto();
            }
        });
    }

    @Override
    protected void onDestroy() {
        classifier.close();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_PERMISSION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow in your app.
                    getPhoto();
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    Snackbar.make(findViewById(R.id.main_activity), R.string.read_external_storage_permission_denied,
                            Snackbar.LENGTH_SHORT)
                            .show();
                }
        }
    }

    private void requestReadExternalStoragePermission() {
        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Snackbar.make(findViewById(R.id.main_activity), R.string.read_external_storage_required,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION);
                }
            }).show();
        } else {
            Snackbar.make(findViewById(R.id.main_activity), R.string.read_external_storage_required, Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION);
        }
    }

    /**
     * Gets a photo from the local files app
     */
    private void getPhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already available, select photo
            // Create intent for picking a photo from the gallery
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");

            Intent chooser = Intent.createChooser(intent, "Choose a Picture");
            startActivityForResult(chooser, ACTION_REQUEST_GALLERY);
        } else {
            // Permission is missing and must be requested.
            requestReadExternalStoragePermission();
        }
    }

    /**
     * Handles the result of the activity called form startActivityForResult
     * The only result in this application is the selected photo being returned in the data param
     *
     * @param requestCode the requestCode
     * @param resultCode  the resultCode
     * @param data        the data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == ACTION_REQUEST_GALLERY) {
                try {
                    imagePath = data.getData();
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imagePath);
                    imageView.setImageBitmap(bitmap);
                    classify(bitmap);
                    if (!imagePresent) {
                        findViewById(R.id.saveButton).setEnabled(true);
                        imagePresent = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Uses the previously defined classifier to classify the chest xray as being either Covid positive or Normal.
     * Sets the results textView accordingly
     */
    public void classify(Bitmap bitmap) {
        Classifier.Recognition result = classifier.recognizeImage(bitmap.copy(Bitmap.Config.ARGB_8888, true), Surface.ROTATION_0);
        if (result.getConfidence() > .700 && result.getTitle().contains("covid")) {
            resultsTextView.setText(R.string.covid_positive);
        } else {
            resultsTextView.setText(R.string.normal);
        }
    }

    /**
     * Handles what happens when the save button is clicked. Gets the patient name from the user.
     * Saves the photo to firebase cloud storage in a folder specific to the patient.
     */
    private void handleSavePhoto() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Enter name of patient");
        final EditText input = new EditText(this);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String name = input.getText().toString().toLowerCase();
                String photoName = getTimestamp() +  ": " + resultsTextView.getText().toString().substring(9);
                StorageReference storageRef = mFirebaseStorage.getReference();
                StorageReference picRef = storageRef.child(name + "/" + photoName + ".png");
                picRef.putFile(imagePath)
                        .addOnSuccessListener(taskSnapshot -> {
                            Snackbar.make(findViewById(R.id.main_activity), "Image Uploaded Successfully", Snackbar.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(exception -> {
                            Snackbar.make(findViewById(R.id.main_activity), exception.getMessage(), Snackbar.LENGTH_LONG).show();
                        });
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.METHOD, "Firebase Storage");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    private String getTimestamp() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM_dd_yyyy h:mm:ss a");
        return " " + sdf.format(date);
    }
}