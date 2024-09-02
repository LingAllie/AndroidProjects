package com.tnl.mhstatistic;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tnl.entity.FileRecord;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class SharedPreferencesHelper {

    private static final String PREFS_NAME = "app_prefs";
    private static final String FOLDER_LIST_KEY = "folder_list";
    private static final String FOLDER_FILES_KEY = "folder_files";

    public static void saveFoldersToPreferences(Context context, List<String> folderList) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(folderList);
        editor.putString(FOLDER_LIST_KEY, json);
        editor.apply();
    }

    public static List<String> loadFoldersFromPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(FOLDER_LIST_KEY, null);
        Gson gson = new Gson();
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    // Save folder files
    public static void saveFolderFilesToPreferences(Context context, Map<String, List<FileRecord>> folderFilesMap) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(folderFilesMap);
        editor.putString(FOLDER_FILES_KEY, json);
        editor.apply();
    }

    // Load folder files
    public static Map<String, List<FileRecord>> loadFolderFilesFromPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(FOLDER_FILES_KEY, null);
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, List<FileRecord>>>() {}.getType();
        return gson.fromJson(json, type);
    }
}
