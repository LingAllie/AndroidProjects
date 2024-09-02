package com.tnl.mhstatistic;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tnl.adapter.FileAdapter;
import com.tnl.entity.FileRecord;
import com.tnl.shared.SharedViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImportActivity extends Fragment implements FileAdapter.OnFileLongClickListener{

    private static final int PICK_FILE_REQUEST_CODE = 1;
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final String TAG = "ListExcel";

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private FloatingActionButton floatBtnFile;
    private SharedViewModel viewModel;
    private String folderName;
    private LinearLayout btnBack;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list_excel, container, false);

        recyclerView = rootView.findViewById(R.id.recyclerViewFile);
        floatBtnFile = rootView.findViewById(R.id.floatBtnFile);
        btnBack = rootView.findViewById(R.id.btnBack);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        if (getArguments() != null) {
            folderName = getArguments().getString("FOLDER_NAME");
            viewModel.setSelectedFolder(folderName);
        }

        adapter = new FileAdapter(this::onFileLongClick);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);

        viewModel.getFolderFilesMap().observe(getViewLifecycleOwner(), folderFilesMap -> {
            List<FileRecord> files = folderFilesMap.get(folderName);
            if (files != null) {
                adapter.updateFileList(files);
            }
        });

        floatBtnFile.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openFilePicker();
            } else {
                requestStoragePermission();
            }
        });

        btnBack.setOnClickListener(v -> navigateToImportFragment());

        return rootView;
    }

    private void navigateToImportFragment() {
        // Replace the current fragment with ImportFragment
        if (getActivity() != null) {
            Fragment importFragment = new ManageActivity();
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.frame_layout, importFragment); // Update R.id.frame_layout with your actual container ID
            transaction.addToBackStack(null); // Add to back stack to enable back navigation
            transaction.commit();
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int write = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
            return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                intent.setData(uri);
                storageActivityResultLauncher.launch(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storageActivityResultLauncher.launch(intent);
            }
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    private final ActivityResultLauncher<Intent> storageActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            // Permission granted
                            Log.d(TAG, "Permission is granted");
                        } else {
                            // Permission denied
                            Log.d(TAG, "Permission is denied");
                        }
                    }
                }
            }
    );

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Excel File"), PICK_FILE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == getActivity().RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    String fileName = getFileName(uri);
                    String importDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                    FileRecord fileRecord = new FileRecord(fileName, importDate, folderName);

                    // Log for debugging
                    Log.d(TAG, "Adding file: " + fileRecord.getFileName());

                    // Add file to ViewModel
                    viewModel.addFile(requireContext(), folderName, fileRecord);

                    // Update adapter
                    adapter.addFileRecord(fileRecord);

                    // Save file to local storage
                    saveFile(uri, fileName);
                } catch (IOException e) {
                    Toast.makeText(getContext(), "Error reading file", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String getFileName(Uri uri) {
        String[] projection = {DocumentsContract.Document.COLUMN_DISPLAY_NAME};
        Cursor cursor = getContext().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String fileName = cursor.getString(0);
                cursor.close();
                return fileName;
            }
            cursor.close();
        }
        return "Unknown";
    }

    private void saveFile(Uri uri, String fileName) throws IOException {
        InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
        if (inputStream != null) {
            File externalStorageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (externalStorageDir != null) {
                File file = new File(externalStorageDir, fileName);
                if (file.exists()) {
                    file = renameFile(file);
                }
                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    inputStream.close();
                    outputStream.flush();
                    Toast.makeText(getContext(), "File saved as: " + file.getName(), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            throw new IOException("Error opening file input stream");
        }
    }

    private File renameFile(File file) {
        String fileName = file.getName();
        String newFileName = fileName;
        int i = 1;
        while (file.exists()) {
            newFileName = fileName + " (" + i + ")";
            file = new File(file.getParent(), newFileName);
            i++;
        }
        return file;
    }

    @Override
    public void onFileLongClick(FileRecord fileRecord) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete File")
                .setMessage("Are you sure you want to delete " + fileRecord.getFileName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Handle file deletion
                    viewModel.removeFile(requireContext(), folderName, fileRecord.getFileName());
                    adapter.updateFileList(viewModel.getFolderFilesMap().getValue().get(folderName));
                    Toast.makeText(getContext(), "File deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
