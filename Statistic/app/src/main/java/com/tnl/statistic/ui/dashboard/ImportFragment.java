package com.tnl.statistic.ui.dashboard;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tnl.statistic.R;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportFragment extends Fragment {

    private static final int PICK_FILE_REQUEST_CODE = 1;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 2;
    private FloatingActionButton fabImport;
    private ListView listViewImportedFiles;
    private Map<String, Data> dataMap = new HashMap<>();
    private List<String> fileList = new ArrayList<>();
    private ArrayAdapter<String> fileListAdapter;
    private Spinner spinnerYear;
    private String selectedYear = "2024"; // Default year, you can set it dynamically based on current year


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import, container, false);

        fabImport = view.findViewById(R.id.fabImport);
        listViewImportedFiles = view.findViewById(R.id.listViewImportedFiles);
        spinnerYear = view.findViewById(R.id.spinnerYear);

        fileListAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, fileList);
        listViewImportedFiles.setAdapter(fileListAdapter);

        // Set up the spinner with years
        List<String> years = new ArrayList<>();
        for (int i = 2019; i <= 2100; i++) {
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, years);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(spinnerAdapter);

        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedYear = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Keep default year
            }
        });

        fabImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ImportFragment", "Import button clicked");
                requestStoragePermission();
            }
        });

        listViewImportedFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("ImportFragment", "List item clicked at position: " + position);
                // Handle list item click if needed
            }
        });

        return view;
    }

    private void requestStoragePermission() {
        Log.d("ImportFragment", "Checking storage permission");
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("ImportFragment", "Requesting storage permission");
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_STORAGE_PERMISSION);
        } else {
            Log.d("ImportFragment", "Storage permission already granted");
            startFilePickerIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("ImportFragment", "onRequestPermissionsResult called with requestCode: " + requestCode);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("ImportFragment", "Storage permission granted");
                startFilePickerIntent();
            } else {
                Log.d("ImportFragment", "Storage permission denied");
            }
        } else {
            Log.d("ImportFragment", "Request code not matched: " + requestCode);
        }
    }

    private void startFilePickerIntent() {
        Log.d("ImportFragment", "Starting file picker intent");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Choose a file"), PICK_FILE_REQUEST_CODE);
        } catch (Exception e) {
            Log.e("ImportFragment", "Failed to start file picker intent", e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("ImportFragment", "onActivityResult called with requestCode: " + requestCode + " and resultCode: " + resultCode);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            Log.d("ImportFragment", "File selected: " + (fileUri != null ? fileUri.toString() : "null"));
            if (fileUri != null) {
                processExcelFile(fileUri);
            } else {
                Log.d("ImportFragment", "File URI is null");
            }
        } else {
            Log.d("ImportFragment", "Request code or result code not matched");
        }
    }

    private void processExcelFile(Uri fileUri) {
        Log.d("ImportFragment", "Processing file: " + fileUri.toString());
        try (InputStream inputStream = getContext().getContentResolver().openInputStream(fileUri)) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            for (Row row : sheet) {
                // Split the date from Excel
                String excelDate = row.getCell(0).getStringCellValue(); // e.g., "1-Aug"
                String[] dateParts = excelDate.split("-");
                String day = dateParts[0];  // "1"
                String month = dateParts[1]; // "Aug"

                // Create the data object
                Data data = new Data();
                data.setRoom((int) row.getCell(1).getNumericCellValue());
                data.setEvent((int) row.getCell(2).getNumericCellValue());
                data.setTotalMoney(row.getCell(3).getNumericCellValue());
                data.setTotalKWh(row.getCell(4).getNumericCellValue());
                data.setFnbMoney(row.getCell(5).getNumericCellValue());
                data.setRoomMoney(row.getCell(6).getNumericCellValue());
                data.setSpaMoney(row.getCell(7).getNumericCellValue());
                data.setAdminPublicMoney(row.getCell(8).getNumericCellValue());
                data.setFnbKWh(row.getCell(9).getNumericCellValue());
                data.setRoomKWh(row.getCell(10).getNumericCellValue());
                data.setSpaKWh(row.getCell(11).getNumericCellValue());
                data.setAdminPublicKWh(row.getCell(12).getNumericCellValue());

                // Prepare Firestore data map
                Map<String, Object> firestoreData = new HashMap<>();
                firestoreData.put("Room", data.getRoom());
                firestoreData.put("Event", data.getEvent());
                firestoreData.put("TotalElectricBill", data.getTotalMoney());
                firestoreData.put("TotalElectricUse", data.getTotalKWh());
                firestoreData.put("F&BFee", data.getFnbMoney());
                firestoreData.put("RoomFee", data.getRoomMoney());
                firestoreData.put("SpaFee", data.getSpaMoney());
                firestoreData.put("AdminPublicFee", data.getAdminPublicMoney());
                firestoreData.put("F&Bkwh", data.getFnbKWh());
                firestoreData.put("Roomkwh", data.getRoomKWh());
                firestoreData.put("Spakwh", data.getSpaKWh());
                firestoreData.put("AdminPublickwh", data.getAdminPublicKWh());

                // Save to Firestore
                db.collection("MHElectric")
                        .document(selectedYear) // Assume year is 2024, change as needed
                        .collection(month) // Use month as collection
                        .document(day) // Use day as document key
                        .set(firestoreData)
                        .addOnSuccessListener(documentReference -> Log.d("ImportFragment", "Data successfully added for date: " + excelDate))
                        .addOnFailureListener(e -> Log.e("ImportFragment", "Error adding document", e));
            }

            // Update the file list and refresh the ListView
            String fileName = fileUri.getLastPathSegment();
            if (fileName != null) {
                fileList.add(fileName);
                fileListAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            Log.e("ImportFragment", "Error processing Excel file", e);
        }
    }


    private void fetchDataFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("MHElectric")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            String date = document.getString("Date");
                            int room = document.getLong("Room").intValue();
                            int event = document.getLong("Event").intValue();
                            double totalElectricBill = document.getDouble("TotalElectricBill");
                            double totalElectricUse = document.getDouble("TotalElectricUse");
                            double fnbFee = document.getDouble("F&BFee");
                            double roomFee = document.getDouble("RoomFee");
                            double spaFee = document.getDouble("SpaFee");
                            double adminPublicFee = document.getDouble("AdminPublicFee");
                            double fnbKwh = document.getDouble("F&Bkwh");
                            double roomKwh = document.getDouble("Roomkwh");
                            double spaKwh = document.getDouble("Spakwh");
                            double adminPublicKwh = document.getDouble("AdminPublickwh");

                            // Use this data to populate your chart
                        }
                    } else {
                        Log.e("ImportFragment", "Error getting documents: ", task.getException());
                    }
                });
    }

    public static class Data {
        private int room;
        private int event;
        private double totalMoney;
        private double totalKWh;
        private double fnbMoney;
        private double roomMoney;
        private double spaMoney;
        private double adminPublicMoney;
        private double fnbKWh;
        private double roomKWh;
        private double spaKWh;
        private double adminPublicKWh;

        // Getters and setters
        public int getRoom() { return room; }
        public void setRoom(int room) { this.room = room; }
        public int getEvent() { return event; }
        public void setEvent(int event) { this.event = event; }
        public double getTotalMoney() { return totalMoney; }
        public void setTotalMoney(double totalMoney) { this.totalMoney = totalMoney; }
        public double getTotalKWh() { return totalKWh; }
        public void setTotalKWh(double totalKWh) { this.totalKWh = totalKWh; }
        public double getFnbMoney() { return fnbMoney; }
        public void setFnbMoney(double fnbMoney) { this.fnbMoney = fnbMoney; }
        public double getRoomMoney() { return roomMoney; }
        public void setRoomMoney(double roomMoney) { this.roomMoney = roomMoney; }
        public double getSpaMoney() { return spaMoney; }
        public void setSpaMoney(double spaMoney) { this.spaMoney = spaMoney; }
        public double getAdminPublicMoney() { return adminPublicMoney; }
        public void setAdminPublicMoney(double adminPublicMoney) { this.adminPublicMoney = adminPublicMoney; }
        public double getFnbKWh() { return fnbKWh; }
        public void setFnbKWh(double fnbKWh) { this.fnbKWh = fnbKWh; }
        public double getRoomKWh() { return roomKWh; }
        public void setRoomKWh(double roomKWh) { this.roomKWh = roomKWh; }
        public double getSpaKWh() { return spaKWh; }
        public void setSpaKWh(double spaKWh) { this.spaKWh = spaKWh; }
        public double getAdminPublicKWh() { return adminPublicKWh; }
        public void setAdminPublicKWh(double adminPublicKWh) { this.adminPublicKWh = adminPublicKWh; }
    }
}
