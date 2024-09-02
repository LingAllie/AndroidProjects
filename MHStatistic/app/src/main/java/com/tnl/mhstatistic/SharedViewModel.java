package com.tnl.mhstatistic;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tnl.entity.FileRecord;

import java.util.ArrayList;
import java.util.List;

public class SharedViewModel extends ViewModel {

    private final MutableLiveData<List<String>> folderList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<FileRecord>> fileList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> selectedFolder = new MutableLiveData<>();

    public LiveData<List<String>> getFolderList() {
        return folderList;
    }

    public LiveData<List<FileRecord>> getFileList() {
        return fileList;
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
        SharedPreferencesHelper.saveFoldersToPreferences(context, folders);
    }

    public void removeFolder(Context context, String folderName) {
        List<String> folders = folderList.getValue();
        if (folders != null) {
            folders.remove(folderName);
            folderList.setValue(folders);
            SharedPreferencesHelper.saveFoldersToPreferences(context, folders);
        }
    }


    public void addFile(FileRecord fileRecord) {
        List<FileRecord> currentList = fileList.getValue();
        if (currentList != null) {
            currentList.add(fileRecord);
            fileList.setValue(currentList);
        }
    }

    public void removeFile(String fileName) {
        List<FileRecord> currentList = fileList.getValue();
        if (currentList != null) {
            currentList.remove(fileName);
            fileList.setValue(currentList);
        }
    }

    public void setSelectedFolder(String folderName) {
        selectedFolder.setValue(folderName);
    }

    public void loadFolders(Context context) {
        List<String> folders = SharedPreferencesHelper.loadFoldersFromPreferences(context);
        if (folders != null) {
            folderList.setValue(folders);
        }
    }


}
