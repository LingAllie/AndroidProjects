package com.tnl.mhstatistic;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tnl.entity.FileRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedViewModel extends ViewModel {

    private final MutableLiveData<List<String>> folderList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, List<FileRecord>>> folderFilesMap = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<String> selectedFolder = new MutableLiveData<>();
    private final String TAG = "SVM";

    public LiveData<List<String>> getFolderList() {
        return folderList;
    }

    public LiveData<Map<String, List<FileRecord>>> getFolderFilesMap() {
        return folderFilesMap;
    }

    public LiveData<String> getSelectedFolder() {
        return selectedFolder;
    }

    public void addFolder(Context context, String folderName) {
        List<String> folders = folderList.getValue();
        if (folders == null) {
            folders = new ArrayList<>();
        }
        folders.add(folderName);
        folderList.setValue(folders);

        // Initialize folder files map
        Map<String, List<FileRecord>> filesMap = folderFilesMap.getValue();
        if (filesMap == null) {
            filesMap = new HashMap<>();
        }
        filesMap.put(folderName, new ArrayList<>());
        folderFilesMap.setValue(filesMap);

        SharedPreferencesHelper.saveFoldersToPreferences(context, folders);
        SharedPreferencesHelper.saveFolderFilesToPreferences(context, filesMap);
    }

    public void removeFolder(Context context, String folderName) {
        List<String> folders = folderList.getValue();
        if (folders != null) {
            folders.remove(folderName);
            folderList.setValue(folders);

            // Remove folder files map entry
            Map<String, List<FileRecord>> filesMap = folderFilesMap.getValue();
            if (filesMap != null) {
                filesMap.remove(folderName);
                folderFilesMap.setValue(filesMap);
            }

            SharedPreferencesHelper.saveFoldersToPreferences(context, folders);
            SharedPreferencesHelper.saveFolderFilesToPreferences(context, filesMap);
        }
    }

    public void addFile(Context context, String folderName, FileRecord fileRecord) {
        Map<String, List<FileRecord>> filesMap = folderFilesMap.getValue();
        if (filesMap == null) {
            filesMap = new HashMap<>();
        }

        List<FileRecord> files = filesMap.get(folderName);
        if (files == null) {
            files = new ArrayList<>();
            filesMap.put(folderName, files);
        }

        // Check for duplicate file
        boolean exists = files.stream()
                .anyMatch(existingFile -> existingFile.getFileName().equals(fileRecord.getFileName()));

        if (!exists) {
            files.add(fileRecord);
            folderFilesMap.setValue(filesMap);
            SharedPreferencesHelper.saveFolderFilesToPreferences(context, filesMap);
        }
    }




    public void removeFile(Context context, String folderName, String fileName) {
        Map<String, List<FileRecord>> filesMap = folderFilesMap.getValue();
        if (filesMap != null) {
            List<FileRecord> files = filesMap.get(folderName);
            if (files != null) {
                files.removeIf(fileRecord -> fileRecord.getFileName().equals(fileName));
                folderFilesMap.setValue(filesMap);
                SharedPreferencesHelper.saveFolderFilesToPreferences(context, filesMap);
            }
        }
    }

    public void setSelectedFolder(String folderName) {
        selectedFolder.setValue(folderName);
    }

    public void loadFoldersAndFiles(Context context) {
        List<String> folders = SharedPreferencesHelper.loadFoldersFromPreferences(context);
        Map<String, List<FileRecord>> filesMap = SharedPreferencesHelper.loadFolderFilesFromPreferences(context);

        Log.d(TAG, "Loaded folders: " + folders);
        Log.d(TAG, "Loaded file records: " + filesMap);

        if (folders != null) {
            folderList.setValue(folders);
        }

        if (filesMap != null) {
            folderFilesMap.setValue(filesMap);
        } else {
            folderFilesMap.setValue(new HashMap<>());
        }
    }

}
