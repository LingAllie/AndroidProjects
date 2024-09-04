package com.tnl.mhstatistic;

import android.Manifest;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.tnl.mhstatistic.adapter.FileAdapter;
import com.tnl.mhstatistic.entity.FileRecord;
import com.tnl.mhstatistic.shared.SharedViewModel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ImportActivity extends Fragment {

    private static final int PICK_FILE_REQUEST_CODE = 1;
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final String TAG = "ListExcel";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private FloatingActionButton floatBtnFile;
    private SharedViewModel viewModel;
    private String folderName;
    private LinearLayout btnBack;
    private Spinner spinnerSortOptions;
    private List<FileRecord> fileList;
    private TextView tvFoldName;

    private FirebaseFirestore firestore;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_file, container, false);

        recyclerView = rootView.findViewById(R.id.recyclerViewFile);
        floatBtnFile = rootView.findViewById(R.id.floatBtnFile);
        btnBack = rootView.findViewById(R.id.btnBack);
        tvFoldName = rootView.findViewById(R.id.tvFoldName);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        firestore = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            folderName = getArguments().getString("FOLDER_NAME");
            viewModel.setSelectedFolder(folderName);
        }

        tvFoldName.setText(folderName);

        setupRecyclerView();
        setupFabButton();
        setupBackButton();

        return rootView;
    }

    private void setupRecyclerView() {
        adapter = new FileAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel.loadFiles(folderName);
        viewModel.getSelectedFolder().observe(getViewLifecycleOwner(), selectedFolder -> {
            Log.d(TAG, "Selected Folder: " + selectedFolder);
            if (selectedFolder != null) {
                viewModel.getFileRecords().observe(getViewLifecycleOwner(), fileRecords -> {
                    if (fileRecords != null) {
                        Log.d(TAG, "File Records Count: " + fileRecords.size());
                        List<FileRecord> filteredFiles = new ArrayList<>();
                        for (FileRecord file : fileRecords) {
                            String folderYear = file.getFolderName();
                            Log.d(TAG, "FileRecord Import Date: " + folderYear);
                            if (folderYear.equals(selectedFolder)) {
                                filteredFiles.add(file);
                            }
                        }
                        Log.d(TAG, "Filtered files: " + filteredFiles.size());
                        adapter.updateFileList(filteredFiles);
                    }
                });
            }
        });


        // Load files if not already loaded
        if (viewModel.getFileRecords().getValue() == null || viewModel.getFileRecords().getValue().isEmpty()) {
            viewModel.loadFiles(folderName);
        }
    }

    private void setupFabButton() {
        floatBtnFile.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                openFilePicker();
            } else {
                requestStoragePermission();
            }
        });
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> navigateToImportFragment());
    }

    private void navigateToImportFragment() {
        // Replace the current fragment with ManageActivity
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
            result -> {
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
                String fileName = getFileName(uri);
                if (fileName.endsWith(".xlsx")) {
                    try {
                        String importDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                        FileRecord fileRecord = new FileRecord(fileName, importDate, folderName, "");

                        // Log for debugging
                        Log.d(TAG, "Adding file: " + fileRecord.getFileName());

                        // Add file to ViewModel
                        Log.d(TAG, "onActive: folder name: " + folderName);
                        viewModel.addFile(requireContext(), folderName, fileRecord);

                        // Update adapter
                        adapter.addFileRecord(fileRecord);

                        // Save file to local storage
                        saveFile(uri, fileName, fileRecord);

                        // Process Excel File and save data to Firestore
                        processExcelFile(uri);

                    } catch (IOException e) {
                        Toast.makeText(getContext(), "Error reading file", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Error: Only .xlsx files are accepted", Toast.LENGTH_SHORT).show();
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

    private void saveFile(Uri uri, String fileName, FileRecord fileRecord) throws IOException {
        InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
        if (inputStream != null) {
            File externalStorageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (externalStorageDir != null) {
                File file = new File(externalStorageDir, fileName);

                // Check if the file already exists and delete it
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        Log.d(TAG, "Existing file deleted: " + file.getName());
                    } else {
                        Log.e(TAG, "Failed to delete existing file: " + file.getName());
                    }
                }

                // Save the new file
                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    inputStream.close();
                    outputStream.flush();
                    Toast.makeText(getContext(), "Please wait..." , Toast.LENGTH_SHORT).show();

                    // Upload to Firebase Storage
                    uploadFileToFirebase(file, fileName, fileRecord);
                }
            }
        } else {
            throw new IOException("Error opening file input stream");
        }
    }

    private void uploadFileToFirebase(File file, String fileName, FileRecord fileRecord) {
        Uri fileUri = Uri.fromFile(file);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child(folderName + "/Files/" + fileName);
        UploadTask uploadTask = storageRef.putFile(fileUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                fileRecord.setUrl(downloadUrl.toString());
                saveFileRecordToFirestore(fileRecord);
            }).addOnFailureListener(e -> Log.e(TAG, "Failed to get download URL", e));
        }).addOnFailureListener(e -> Log.e(TAG, "Upload failed", e));
    }

    private void saveFileRecordToFirestore(FileRecord fileRecord) {
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("fileName", fileRecord.getFileName());
        fileData.put("importDate", fileRecord.getImportDate());
        fileData.put("folderName", fileRecord.getFolderName());
        fileData.put("url", fileRecord.getUrl());

        // Construct the Firestore path for the document
        String path = "Files/" + folderName + "/" + fileRecord.getFileName();

        // Save the file data to the Firestore document at the new path
        firestore.collection("Files")
                .document(folderName)
                .collection("Files")
                .document(fileRecord.getFileName())
                .set(fileData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "FileRecord saved successfully");
                    viewModel.addFile(requireContext(), folderName, fileRecord);
                    adapter.addFileRecord(fileRecord);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error saving FileRecord", e));

            }


    private void processExcelFile(Uri uri) {
        try (InputStream inputStream = getContext().getContentResolver().openInputStream(uri)) {
            Workbook workbook = new XSSFWorkbook(inputStream);

            // Process each sheet
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);

                if (sheet == null) {
                    Log.e(TAG, "Sheet is null");
                    Toast.makeText(getContext(), "Sheet not found", Toast.LENGTH_SHORT).show();
                    continue;
                }

                Log.d(TAG, "Processing sheet: " + sheet.getSheetName());

                for (Row row : sheet) {
                    try {
                        // Skip header row
                        if (row.getRowNum() == 0) continue;

                        // Initialize variables with default values
                        Date excelDate = row.getCell(0) != null ? row.getCell(0).getDateCellValue() : null;
                        int room = (row.getCell(1) != null ? (int) row.getCell(1).getNumericCellValue() : 0) < 0 ? (int) (row.getCell(1).getNumericCellValue() * -1) : (int) row.getCell(1).getNumericCellValue();
                        int event = (row.getCell(2) != null ? (int) row.getCell(2).getNumericCellValue() : 0) < 0 ? (int) (row.getCell(2).getNumericCellValue() * -1) : (int) row.getCell(2).getNumericCellValue();
                        double totalMoney = (row.getCell(3) != null ? row.getCell(3).getNumericCellValue() : 0) < 0 ? (int) (row.getCell(3).getNumericCellValue() * -1) : (int) row.getCell(3).getNumericCellValue();
                        double totalKwh = (row.getCell(4) != null ? row.getCell(4).getNumericCellValue() : 0) < 0 ? (int) (row.getCell(4).getNumericCellValue() * -1) : (int) row.getCell(4).getNumericCellValue();
                        double fbMoney = (row.getCell(5) != null ? row.getCell(5).getNumericCellValue() : 0) < 0 ? (int) (row.getCell(5).getNumericCellValue() * -1) : (int) row.getCell(5).getNumericCellValue();
                        double roomMoney = (row.getCell(6) != null ? row.getCell(6).getNumericCellValue() : 0) < 0 ? (int) (row.getCell(6).getNumericCellValue() * -1) : (int) row.getCell(6).getNumericCellValue();
                        double spaMoney = (row.getCell(7) != null ? row.getCell(7).getNumericCellValue() : 0) < 0 ? (int) (row.getCell(7).getNumericCellValue() * -1) : (int) row.getCell(7).getNumericCellValue();
                        double adminMoney = (row.getCell(8) != null ? row.getCell(8).getNumericCellValue() : 0) < 0 ? (int) (row.getCell(8).getNumericCellValue() * -1) : (int) row.getCell(8).getNumericCellValue();
                        double fbKwh = (row.getCell(9) != null ? row.getCell(9).getNumericCellValue() : 0) < 0 ? (int) (row.getCell(9).getNumericCellValue() * -1) : (int) row.getCell(9).getNumericCellValue();
                        double roomKwh = (row.getCell(10) != null ? row.getCell(10).getNumericCellValue() : 0) < 0 ? (int) (row.getCell(10).getNumericCellValue() * -1) : (int) row.getCell(10).getNumericCellValue();
                        double spaKwh = (row.getCell(11) != null ? row.getCell(11).getNumericCellValue() : 0) < 0 ? (int) (row.getCell(11).getNumericCellValue() * -1) : (int) row.getCell(11).getNumericCellValue();
                        double adminKwh = (row.getCell(12) != null ? row.getCell(12).getNumericCellValue() : 0) < 0 ? (int) (row.getCell(12).getNumericCellValue() * -1) : (int) row.getCell(12).getNumericCellValue();


                        // Format the double values to 2 decimal places
                        totalMoney = Double.parseDouble(DECIMAL_FORMAT.format(totalMoney));
                        totalKwh = Double.parseDouble(DECIMAL_FORMAT.format(totalKwh));
                        fbMoney = Double.parseDouble(DECIMAL_FORMAT.format(fbMoney));
                        roomMoney = Double.parseDouble(DECIMAL_FORMAT.format(roomMoney));
                        spaMoney = Double.parseDouble(DECIMAL_FORMAT.format(spaMoney));
                        adminMoney = Double.parseDouble(DECIMAL_FORMAT.format(adminMoney));
                        fbKwh = Double.parseDouble(DECIMAL_FORMAT.format(fbKwh));
                        roomKwh = Double.parseDouble(DECIMAL_FORMAT.format(roomKwh));
                        spaKwh = Double.parseDouble(DECIMAL_FORMAT.format(spaKwh));
                        adminKwh = Double.parseDouble(DECIMAL_FORMAT.format(adminKwh));

                        // Convert the date to year/month/day
                        Calendar calendar = Calendar.getInstance();
                        if (excelDate != null) {
                            calendar.setTime(excelDate);
                        }
                        String year = folderName; // Use folderName as the year
                        String month = new SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.getTime());
                        String day = excelDate != null ? String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)) : "Unknown";

                        // Log the extracted values for debugging
                        Log.d(TAG, String.format("Row %d: Date: %s, Room: %d, Event: %d, TotalMoney: %.2f, TotalKwh: %.2f, F&B Money: %.2f, Room Money: %.2f, Spa Money: %.2f, Admin Money: %.2f, F&B kWh: %.2f, Room kWh: %.2f, Spa kWh: %.2f, Admin kWh: %.2f",
                                row.getRowNum(), excelDate != null ? new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(excelDate) : "Unknown",
                                room, event, totalMoney, totalKwh, fbMoney, roomMoney, spaMoney, adminMoney, fbKwh, roomKwh, spaKwh, adminKwh));

                        // Check if file already exists
                        List<FileRecord> files = viewModel.getFolderFilesMap().getValue().get(folderName);
                        if (files != null) {
                            for (FileRecord fileRecord : files) {
                                if (fileRecord.getFileName().equals(getFileName(uri))) {
                                    // Update existing file record's date
                                    fileRecord.setImportDate(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
                                    viewModel.updateFile(folderName, fileRecord);
                                    adapter.notifyDataSetChanged();
                                    break;
                                }
                            }
                        }

                        // Save to Firestore
                        saveDataToFirestore(year, month, day, room, event, totalMoney, totalKwh,
                                fbMoney, roomMoney, spaMoney, adminMoney,
                                fbKwh, roomKwh, spaKwh, adminKwh);

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing row " + row.getRowNum() + ": " + e.getMessage(), e);
                    }
                }
            }

            workbook.close();
            Toast.makeText(getContext(), "Excel data processed and saved", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Log.e(TAG, "Error opening input stream: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error processing Excel file", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error processing Excel file", Toast.LENGTH_SHORT).show();
        }
    }



    private void saveDataToFirestore(String year, String month, String day, int room, int event,
                                     double totalMoney, double totalKwh, double fbMoney, double roomMoney,
                                     double spaMoney, double adminMoney, double fbKwh, double roomKwh,
                                     double spaKwh, double adminKwh) {
        // Implement the Firestore save logic here
        // Example Firestore save operation
        Map<String, Object> data = new HashMap<>();
        data.put("Room", room);
        data.put("Event", event);
        data.put("TotalElectricBill", totalMoney);
        data.put("TotalElectricUse", totalKwh);
        data.put("F&BFee", fbMoney);
        data.put("RoomFee", roomMoney);
        data.put("SpaFee", spaMoney);
        data.put("AdminPublicFee", adminMoney);
        data.put("F&Bkwh", fbKwh);
        data.put("Roomkwh", roomKwh);
        data.put("Spakwh", spaKwh);
        data.put("AdminPublickwh", adminKwh);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("MHStatistic").document(year)
                .collection(month).document(day)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Data successfully written!"))
                .addOnFailureListener(e -> Log.w("Firestore", "Error writing document", e));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {
                Toast.makeText(getContext(), "Storage permission is required to import files", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
