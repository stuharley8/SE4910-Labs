/*
 * Course: SE4910-011
 * Winter 2021
 * Lab: MSOE GPA Calculator
 * Author: Stuart Harley
 * Created: 1/16/2021
 */

package stuartharley.msoe.lab2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DBHandler extends SQLiteOpenHelper {

    public static final int DB_VERSION = 1;
    public static final String DB_NAME = "gradeDB";

    private static final String TABLE_NAME = "transcript";
    private static final String NAME_COL = "courseName";
    private static final String CREDITS_COL = "credits";
    private static final String LETTER_COL = "letter";

    public DBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_NAME + " (" + NAME_COL + " TEXT PRIMARY KEY,"
                + CREDITS_COL + " INTEGER," + LETTER_COL + " TEXT)";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /**
     * Adds a new course grade to the transcript table
     * @param courseName the courseName of the new grade
     * @param credits the credits of the new grade
     * @param letter the letter grade of the new grade
     */
    public void addNewCourse(String courseName, int credits, String letter) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NAME_COL, courseName);
        values.put(CREDITS_COL, credits);
        values.put(LETTER_COL, letter);
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    /**
     * Deletes all entries from the transcript table
     */
    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
        db.close();
    }

    /**
     * Returns an arraylist of grades created from every row of the transcript table
     * @return the list of grades
     */
    public ArrayList<Grade> getAllGrades() {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<Grade> grades = new ArrayList<>();
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null)) {
            while (cursor.moveToNext()) {
                String courseName = cursor.getString(cursor.getColumnIndexOrThrow(NAME_COL));
                int credits = cursor.getInt(cursor.getColumnIndexOrThrow(CREDITS_COL));
                String letter = cursor.getString(cursor.getColumnIndexOrThrow(LETTER_COL));
                grades.add(new Grade(courseName, credits, letter));
            }
        }
        db.close();
        return grades;
    }

    /**
     * Updates a grade in the transcript table with the new credits and letter values
     * @param courseName the courseName of the grade to update
     * @param credits the new credit value of the course
     * @param letter the new letter grade of the course
     */
    public void updateGrade(String courseName, int credits, String letter) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CREDITS_COL, credits);
        values.put(LETTER_COL, letter);
        db.update(TABLE_NAME, values, "courseName=?", new String[]{courseName});
        db.close();
    }
}