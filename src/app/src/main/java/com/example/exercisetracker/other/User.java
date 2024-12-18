package com.example.exercisetracker.other;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.exercisetracker.login.LogInScreen;

import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

public class User {
    private static Integer UserID;
    private static String forename;
    private static String surname;
    private static Float weight;
    private static java.sql.Date dateOfBirth;
    private static Integer height;
    private static String username;
    private static String password;
    private static Set<Integer> friendsList = new HashSet<Integer>();

    public static Set<Integer> getFriendsList() {
        return friendsList;
    }

    public static void addFriendsList(Integer friend) {
        //adds to set of friends
        //as set, no duplicate ids will exist
        User.friendsList.add(friend);
    }

    public static void removeFriendsList(Integer friend) {
        //adds to set of friends
        //as set, no duplicate ids will exist
        User.friendsList.remove(friend);
    }

    public static void saveUser(String username, String password,String forename, String surname,String DOB, Float weight, Integer height ){
        //saving sanitised details to user class
        setUsername(username);
        setPassword(password);
        setWeight(weight);
        setHeight(height);
        setForename(forename);
        setSurname(surname);
        setDateOfBirth(Date.valueOf(DOB));
    }

    public static void clearFriendsList() {
        friendsList.clear();
    }


    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        User.username = username;
    }

    public static String getForename() {
        return forename;
    }

    public static void setForename(String forename) {
        User.forename = forename;
    }

    public static String getSurname() {
        return surname;
    }

    public static void setSurname(String surname) {
        User.surname = surname;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        User.password = password;
    }

    public static Integer getUserID() {
        return UserID;
    }

    public static void setUserID(Integer userID) {
        UserID = userID;
    }

    public static String getName() {
        return getForename() + " " + getSurname();
    }

    public static Integer getHeight() {
        return height;
    }

    public static void setHeight(Integer height) {
        User.height = height;
    }

    public static Float getWeight() {
        return weight;
    }

    public static void setWeight(Float weight) {
        User.weight = weight;
    }

    public static java.sql.Date getDateOfBirth() {
        return dateOfBirth;
    }

    public static void setDateOfBirth(java.sql.Date dateOfBirth) {
        User.dateOfBirth = dateOfBirth;
    }

    public static void logout(Context context) {
        //clearing User class data
        setUsername(null);
        setUserID(null);
        setHeight(null);
        setWeight(null);
        setDateOfBirth(null);
        setPassword(null);
        setForename(null);
        setSurname(null);
        //clearing shared preferences
        SharedPreferences prefs = context.getSharedPreferences(LogInScreen.getShared_prefs(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

}
