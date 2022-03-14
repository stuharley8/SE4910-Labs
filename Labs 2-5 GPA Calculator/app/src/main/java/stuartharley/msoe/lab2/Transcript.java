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

import java.util.ArrayList;

/**
 * Represents a MSOE transcript containing grades
 */

public class Transcript implements Parcelable {

    private final ArrayList<Grade> grades;
    private double gpa;

    // Constructor
    public Transcript() {
        this.grades = new ArrayList<>();
        this.gpa = 0.0;
    }

    // Parcelable Constructor
    public Transcript(Parcel in) {
        grades = in.createTypedArrayList(Grade.CREATOR);
        gpa = in.readDouble();
    }

    public static final Creator<Transcript> CREATOR = new Creator<Transcript>() {
        @Override
        public Transcript createFromParcel(Parcel in) {
            return new Transcript(in);
        }

        @Override
        public Transcript[] newArray(int size) {
            return new Transcript[size];
        }
    };

    public double getGpa() {
        return gpa;
    }

    public int getTotalCredits() {
        int totalCredits = 0;
        for(Grade grade : grades) {
            totalCredits += grade.getCredits();
        }
        return totalCredits;
    }

    /**
     * Returns the number of courses in the transcript
     * @return the size of the underlying arraylist
     */
    public int getNumCourses() {
        return grades.size();
    }

    /**
     * Returns Grade at the specified index
     * @param index the index
     * @return the Grade at the index
     */
    public Grade getGrade(int index) {
        return grades.get(index);
    }

    /**
     * Adds a grade to the transcript. Transcript must not contain a grade with the same courseName
     * @param grade the grade to be added
     * @return whether or not the grade was added to the transcript successfully
     */
    public boolean addGrade(Grade grade) {
        for(Grade g : grades) {
            if(g.getCourseName().equals(grade.getCourseName())) {
                return false;
            }
        }
        grades.add(grade);
        calcGPA();
        return true;
    }

    /**
     * Calculates the GPA based on the grades contained in the list of grades
     */
    public void calcGPA() {
        gpa = 0.0;
        int totalCredits = 0;
        for(Grade grade : grades) {
            gpa += grade.getCredits() * grade.getGradePoints();
            totalCredits += grade.getCredits();
        }
        gpa /= totalCredits;
    }

    /**
     * Allows for a grade in the transcript to be specified and changed
     * @param courseName the courseName of the grade to change
     * @param credits the new credits of the grade
     * @param letter the new letter grade of the grade
     * @return whether or not a grade with the specified courseName was changed
     */
    public boolean changeGrade(String courseName, int credits, String letter) {
        for(Grade grade : grades) {
            if(grade.getCourseName().equals(courseName)) {
                grade.changeGrade(credits, letter);
                calcGPA();
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a String array of all the class names of the grades in the transcript
     * @return the string array of class names
     */
    public String[] getClassNames() {
        String[] ret = new String[grades.size()];
        for(int i = 0; i < grades.size(); i++) {
            ret[i] = grades.get(i).getCourseName();
        }
        return ret;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        for(Grade grade : grades) {
            dest.writeParcelable(grade, flags);
        }
    }
}
