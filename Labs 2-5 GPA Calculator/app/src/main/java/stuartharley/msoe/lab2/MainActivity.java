/*
 * Course: SE4910-011
 * Winter 2021
 * Lab: MSOE GPA Calculator
 * Author: Stuart Harley
 * Created: 12/10/2021
 */

package stuartharley.msoe.lab2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.telephony.SmsManager;
import android.text.InputType;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import stuartharley.msoe.lab2.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private static final int SEND_SMS_PERMISSIONS_REQUEST = 0;
    private static final String SMS_TEXT_INTRO = "Your GPA is a ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.smsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSMS(SMS_TEXT_INTRO + ((TextView)findViewById(R.id.gpaTextView)).getText().toString().substring(5));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.clear_button:
                return false;
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case SEND_SMS_PERMISSIONS_REQUEST:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow in your app.
                    sendSMS(SMS_TEXT_INTRO + ((TextView)findViewById(R.id.gpaTextView)).getText().toString().substring(5));
                }  else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    Snackbar.make(findViewById(R.id.main_activity), R.string.sms_send_permission_denied,
                            Snackbar.LENGTH_SHORT)
                            .show();
                }
        }
    }

    private void requestSMSSendPermission() {
        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Snackbar.make(findViewById(R.id.main_activity), R.string.sms_send_required,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.SEND_SMS}, SEND_SMS_PERMISSIONS_REQUEST);
                }
            }).show();
        } else {
            Snackbar.make(findViewById(R.id.main_activity), R.string.sms_send_required, Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SEND_SMS_PERMISSIONS_REQUEST);
        }
    }

    private void sendSMS(String message) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already available, send SMS
            EditText phoneNumberTextView = new EditText(this);
            phoneNumberTextView.setInputType(InputType.TYPE_CLASS_PHONE);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(phoneNumberTextView);
            builder.setTitle("Select Phone Number");
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    String phoneNo = phoneNumberTextView.getText().toString();
                    if(phoneNo.length() > 0) {
                        sendSMS(message, phoneNo);
                    }
                }
            });
            builder.show();
        } else {
            // Permission is missing and must be requested.
            requestSMSSendPermission();
        }
    }

    private void sendSMS(String message, String phoneNo) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNo, null, message, null, null);
    }
}