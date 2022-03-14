/*
 * Course: SE4910-011
 * Winter 2021
 * Lab: MSOE GPA Calculator
 * Author: Stuart Harley
 * Created: 12/10/2021
 */

package stuartharley.msoe.lab2;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import stuartharley.msoe.lab2.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private TextView gpaTextView;
    private TableLayout gpaTable;

    private Transcript transcript;
    private ArrayAdapter<String> classNames;

    private static final int MIN_CREDITS = 1;
    private static final int MAX_CREDITS = 4;
    // private static final int MAX_GRADES = 8;
    private static final String CHANNEL_ID = "channel_id";

    private DBHandler dbHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        classNames = new ArrayAdapter<>(getContext(), android.R.layout.select_dialog_item,
                getResources().getStringArray(R.array.known_courses_array));
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        transcript = new Transcript();
        dbHandler = new DBHandler(getContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        gpaTextView = (TextView) getView().findViewById(R.id.gpaTextView);
        gpaTable = (TableLayout) getView().findViewById(R.id.gpaTable);
        populateTranscriptFromDb();
        refreshTable();

        binding.buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getUserGrade(false);
            }
        });

        binding.buttonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getUserGrade(true);
            }
        });
    }

    /** Method is used to save the transcript to a bundle when appropriate. No longer used since we
     * implemented data persistence with SQLite
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("transcript", transcript);
    }
    */

    /** Method is used to load a transcript saved in a bundle when appropriate. No longer used since
     * we implemented data persistence with SQLite
    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if(savedInstanceState != null) {
            transcript = savedInstanceState.getParcelable("transcript");
        } else {
            transcript = new Transcript();
        }
        refreshTable();
    }
    */

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.clear_button:
                clearTranscript();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Queries the DBHandler for a list of all the grades in the database and adds them to the transcript
     */
    private void populateTranscriptFromDb() {
        for(Grade grade : dbHandler.getAllGrades()) {
            transcript.addGrade(grade);
        }
    }

    /**
     * Clears the transcript/database and allows a user to completely restart
     */
    private void clearTranscript() {
        transcript = new Transcript();
        dbHandler.deleteAll();
        refreshTable();
        updateGPATextView();
    }

    /**
     * Gets a course name, credits and letter grade from the user with an AlertDialog popup.
     * @param change False if adding a new grade. True if changing an existing grade.
     */
    private void getUserGrade(boolean change)
    {
        LinearLayout LL = new LinearLayout(getContext());
        LL.setOrientation(LinearLayout.HORIZONTAL);

        AutoCompleteTextView courseAutoCompleteTextView = new AutoCompleteTextView(getContext());
        if(!change) {
            courseAutoCompleteTextView.setAdapter(classNames);
        } else {
            courseAutoCompleteTextView.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.select_dialog_item,
                transcript.getClassNames()));  // If updating a grade, the autocomplete values will only be existing course Names
        }

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        courseAutoCompleteTextView.setInputType(InputType.TYPE_CLASS_TEXT);

        NumberPicker creditsPicker = new NumberPicker(getContext());
        creditsPicker.setMaxValue(MAX_CREDITS);
        creditsPicker.setMinValue(MIN_CREDITS);

        NumberPicker gradePicker = new NumberPicker(getContext());
        String[] grades = new String[] { "A", "AB", "B", "BC", "C", "CD", "D", "F" };
        gradePicker.setMaxValue(grades.length - 1);
        gradePicker.setMinValue(0);
        gradePicker.setDisplayedValues(grades);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(50, 50);
        params.gravity = Gravity.CENTER;

        LinearLayout.LayoutParams courseParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        courseParams.weight = 1;

        LinearLayout.LayoutParams creditParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        creditParams.weight = 1;

        LinearLayout.LayoutParams gradeParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gradeParams.weight = 1;

        LL.setLayoutParams(params);
        LL.addView(courseAutoCompleteTextView, courseParams);
        LL.addView(creditsPicker, creditParams);
        LL.addView(gradePicker, gradeParams);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(LL);
        if(!change) {
            builder.setTitle("Select Course Name, Credits, and Grade");
        } else {
            builder.setTitle("Specify Course Name, New Credits, and New Grade");
        }

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String courseName = courseAutoCompleteTextView.getText().toString();
                int credits = creditsPicker.getValue();
                String letter = grades[gradePicker.getValue()];
                if(courseName.equals("")) {
                    Snackbar.make(getView(), "Grade must have a non-empty courseName", Snackbar.LENGTH_LONG)
                            .setBackgroundTint(getResources().getColor(R.color.red))
                            .setAction("Action", null).show();
                    return;
                }
                if(!change) {
                    boolean added = transcript.addGrade(new Grade(courseName, credits, letter));
                    if(!added) {
                        Snackbar.make(getView(), "Grade must not have the same courseName as an existing Grade", Snackbar.LENGTH_LONG)
                                .setBackgroundTint(getResources().getColor(R.color.red))
                                .setAction("Action", null).show();
                        return;
                    } else {
                        /* Disables Add button if MAX_GRADES is reached in the transcript
                        if(transcript.getNumCourses() >= MAX_GRADES) {
                            binding.buttonAdd.setClickable(false);
                            binding.buttonAdd.setAlpha(.3F);
                        } */
                        dbHandler.addNewCourse(courseName, credits, letter);
                        addGradeToTable(transcript.getGrade(transcript.getNumCourses()-1));  // Adds the last grade in transcript
                    }
                } else {
                    boolean changed = transcript.changeGrade(courseName, credits, letter);
                    if(!changed) {
                        Snackbar.make(getView(), "Grade with specified name not found", Snackbar.LENGTH_LONG)
                                .setBackgroundTint(getResources().getColor(R.color.red))
                                .setAction("Action", null).show();
                        return;
                    }
                    dbHandler.updateGrade(courseName, credits, letter);
                    refreshTable();
                }
                double gpa = transcript.getGpa();
                int totalCredits = transcript.getTotalCredits();
                updateGPATextView();
                showHonorsNotification(gpa, totalCredits);
            }
        });

        builder.show();
    }

    /**
     * Adds a row to the table for a new grade
     * @param grade the grade
     */
    private void addGradeToTable(Grade grade) {
        TableRow row = new TableRow(getContext());
        TextView courseTextView = new TextView(getContext());
        courseTextView.setText(grade.getCourseName());
        courseTextView.setWidth((int) (150 * getResources().getDisplayMetrics().density));
        TextView creditsTextView = new TextView(getContext());
        creditsTextView.setText(String.format("%d", grade.getCredits()));
        creditsTextView.setGravity(Gravity.CENTER);
        creditsTextView.setWidth((int) (101 * getResources().getDisplayMetrics().density));
        TextView gradeTextView = new TextView(getContext());
        gradeTextView.setText(grade.getLetter());
        gradeTextView.setGravity(Gravity.CENTER);
        gradeTextView.setWidth((int) (101 * getResources().getDisplayMetrics().density));
        row.addView(courseTextView);
        row.addView(creditsTextView);
        row.addView(gradeTextView);
        gpaTable.addView(row);
    }

    /**
     * Refreshes the table clearing it and adding all new rows
     */
    private void refreshTable() {
        gpaTable.removeAllViews();
        for(int i=0; i< transcript.getNumCourses(); i++) {
            addGradeToTable(transcript.getGrade(i));
        }
    }

    /**
     * Updates the gpaTextView with the GPA from the transcript
     */
    private void updateGPATextView() {
        gpaTextView.setText(String.format("GPA: %.2f", transcript.getGpa()));
    }

    /**
     * Create the message to be used in any honors/academic probation message
     * @param gpa the gpa to check against honors requirements
     * @param credits the credits to check against dean's list requirements
     * @return a String that is the message to be displayed
     */
    private String createHonorsMessage(double gpa, int credits) {
        String message = "";
        if(credits >= 30 && gpa >= 3.70) {
            message = "Dean's List with High Honors";
        } else if(credits >= 30 && gpa >= 3.20) {
            message = "Dean's List";
        } else if(gpa >= 3.20) {
            message = "Honors List";
        } else if(gpa < 2.0) {
            message = "Academic Probation";
        }
        return message;
    }

    /**
     * Displays any honors/academic probation messages in a snackbar
     * @param gpa the gpa to check against honors requirements
     * @param credits the credits to check against dean's list requirements
     */
    private void showHonorsSnackBar(double gpa, int credits) {
        String message = createHonorsMessage(gpa, credits);
        if(!message.equals("")) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getResources().getColor(R.color.red))
                    .setAction("Action", null).show();
        }
    }

    /**
     * Displays any honors/academic probation messages in a notification
     * @param gpa the gpa to check against honors requirements
     * @param credits the credits to check against dean's list requirements
     */
    private void showHonorsNotification(double gpa, int credits) {
        String message = createHonorsMessage(gpa, credits);
        if(!message.equals("")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://catalog.msoe.edu/content.php?catoid=22&navoid=623"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle(message)
                    .setContentText("See Academic Policy for additional information")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .addAction(R.drawable.empty, "Academic Policy", pendingIntent)
                    .setAutoCancel(true);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());
            notificationManager.notify(1, builder.build());
        }
    }
}