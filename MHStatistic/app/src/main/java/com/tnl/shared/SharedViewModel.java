package com.tnl.shared;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tnl.entity.FileRecord;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedViewModel extends ViewModel {

    private static final String TAG = "SharedViewModel";
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final MutableLiveData<List<String>> folderList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<FileRecord>> fileRecords = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<String, List<FileRecord>>> folderFilesMap = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<String> selectedFolder = new MutableLiveData<>();
    private final MutableLiveData<Date> selectedDate = new MutableLiveData<>();

    public SharedViewModel() {
        // Initialize selectedDate with the current date if not set
        if (selectedDate.getValue() == null) {
            selectedDate.setValue(new Date());
        }
    }

    public LiveData<List<FileRecord>> getFileRecords() {
        return fileRecords;
    }

    public LiveData<Date> getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(Date date) {
        Log.d(TAG, "Setting selected date: " + date);
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
        if (!folders.contains(folderName)) {
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
            fileRecord.setFolderName(folderName);  // Ensure folder name is set
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

    public void loadFolders() {
        // Load folders from Firestore
        firestore.collection("MHElectric")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> folders = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            String folderName = document.getId();
                            folders.add(folderName);

                            // Assuming file records are stored as a subcollection in each folder
                            firestore.collection("MHElectric").document(folderName)
                                    .get()
                                    .addOnCompleteListener(fileTask -> {
                                        if (fileTask.isSuccessful()) {
                                            // Set the data to LiveData after processing all folders
                                            folderList.setValue(folders);
                                        } else {
                                            Log.e(TAG, "Error getting files", fileTask.getException());
                                        }
                                    });
                        }

                    } else {
                        Log.e(TAG, "Error getting folders", task.getException());
                        // Set empty values in case of an error
                        folderList.setValue(new ArrayList<>());
                    }
                });
    }

    public void loadFiles() {
        firestore.collection("Files")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<FileRecord> fileRecordsList = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            String fileName = document.getString("fileName");
                            String folderName = document.getString("folderName");
                            String importDate = document.getString("importDate");
                            String url = document.getString("url");

                            // Log the details of each fileRecord
                            Log.d(TAG, "FileRecord - Name: " + fileName + ", Folder: " + folderName + ", Date: " + importDate + ", URL: " + url);

                            FileRecord fileRecord = new FileRecord(fileName, folderName, importDate, url);
                            fileRecordsList.add(fileRecord);
                        }

                        fileRecords.setValue(fileRecordsList);
                    } else {
                        Log.e(TAG, "Error getting files", task.getException());
                        fileRecords.setValue(new ArrayList<>()); // Set empty list on error
                    }
                });
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
