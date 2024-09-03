package com.tnl.shared;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;
import com.tnl.entity.FileRecord;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedViewModel extends ViewModel {

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final MutableLiveData<List<String>> folderList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, List<FileRecord>>> folderFilesMap = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<String> selectedFolder = new MutableLiveData<>();
    private final MutableLiveData<Date> selectedDate = new MutableLiveData<>();
    private final String TAG = "SVM";


    public LiveData<Date> getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(Date date) {
        selectedDate.setValue(date);
    }

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

        // Save to SharedPreferences
        SharedPreferencesHelper.saveFoldersToPreferences(context, folders);
        SharedPreferencesHelper.saveFolderFilesToPreferences(context, filesMap);

        // Save to Firestore
        firestore.collection("MHElectric").document(folderName)
                .set(new HashMap<>())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Folder added to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding folder to Firestore", e));
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

            // Save to SharedPreferences
            SharedPreferencesHelper.saveFoldersToPreferences(context, folders);
            SharedPreferencesHelper.saveFolderFilesToPreferences(context, filesMap);

            // Remove from Firestore
            firestore.collection("MHElectric").document(folderName)
                    .delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Folder removed from Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error removing folder from Firestore", e));
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

    public void updateFile(String folderName, FileRecord updatedFile) {
        Map<String, List<FileRecord>> folderFiles = folderFilesMap.getValue();
        if (folderFiles != null) {
            List<FileRecord> files = folderFiles.get(folderName);
            if (files != null) {
                for (int i = 0; i < files.size(); i++) {
                    FileRecord file = files.get(i);
                    if (file.getFileName().equals(updatedFile.getFileName())) {
                        files.set(i, updatedFile);
                        break;
                    }
                }
                folderFiles.put(folderName, files);
                folderFilesMap.setValue(folderFiles);
            }
        }
    }

}
