/*
 * Course: SE4910-011
 * Winter 2021
 * Lab: MSOE GPA Calculator
 * Author: Stuart Harley
 * Created: 12/10/2021
 */

package stuartharley.msoe.lab2;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a grade received from 1 completed MSOE course
 */
public class Grade implements Parcelable {

    private String courseName;
    private int credits;
    private String letter;
    private double gradePoints;

    // Constructor
    public Grade(String courseName, int credits, String letter) {
        this.courseName = courseName;
        this.credits = credits;
        this.letter = letter.toUpperCase();
        gradePoints = calcGradePoints();
    }

    // Parcelable Constructor
    public Grade(Parcel in) {
        this.courseName = in.readString();
        this.credits = in.readInt();
        this.letter = in.readString();
        this.gradePoints = in.readDouble();
    }

    public static final Creator<Grade> CREATOR = new Creator<Grade>() {
        @Override
        public Grade createFromParcel(Parcel in) {
            return new Grade(in);
        }

        @Override
        public Grade[] newArray(int size) {
            return new Grade[size];
        }
    };

    private double calcGradePoints() {
        double gp = 0.0;
        switch (letter) {
            case "A":
                gp = 4.0;
                break;
            case "AB":
                gp = 3.5;
                break;
            case "B":
                gp = 3.0;
                break;
            case "BC":
                gp = 2.5;
                break;
            case "C":
                gp = 2.0;
                break;
            case "CD":
                gp = 1.5;
                break;
            case "D":
                gp = 1.0;
                break;
            case "F":
                gp = 0.0;
                break;
        }
        return gp;
    }

    public String getCourseName() {
        return courseName;
    }

    public double getGradePoints() {
        return gradePoints;
    }

    public String getLetter() {
        return letter;
    }

    public int getCredits() {
        return credits;
    }

    /**
     * Allows the letter grade to be changed
     */
    public void changeGrade(int credits, String letter) {
        this.credits = credits;
        this.letter = letter;
        gradePoints = calcGradePoints();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(courseName);
        dest.writeInt(credits);
        dest.writeString(letter);
        dest.writeDouble(gradePoints);
    }
}
