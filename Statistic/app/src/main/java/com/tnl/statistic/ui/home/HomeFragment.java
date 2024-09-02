package com.tnl.statistic.ui.home;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tnl.statistic.ui.dashboard.ImportFragment;
import com.tnl.statistic.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "FirestoreData";

    private CombinedChart combinedChart;
    private TextView txtSelectDate, txtRoomData, txtEventData, txtTotalMoney, txtTotalKWh;
    private Calendar calendar;
    private String selectedDate = "";
    private Map<String, ImportFragment.Data> dataMap = new HashMap<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize UI components
        combinedChart = view.findViewById(R.id.combinedChart);
        txtSelectDate = view.findViewById(R.id.txtSelectDate);
        txtRoomData = view.findViewById(R.id.txtRoomData);
        txtEventData = view.findViewById(R.id.txtEventData);
        txtTotalMoney = view.findViewById(R.id.txtTotalMoney);
        txtTotalKWh = view.findViewById(R.id.txtTotalKWh);

        // Initialize Calendar
        calendar = Calendar.getInstance();
        updateSelectedDate();

        // Set up Firestore
        db = FirebaseFirestore.getInstance();

        // Fetch data from Firestore using the selected year, month abbreviation, and day
        fetchDataFromFirestore(getCurrentYear(), getCurrentMonthAbbreviation(), getCurrentDay());

        // Set up Date Picker TextView
        txtSelectDate.setOnClickListener(v -> showDatePickerDialog());

        // Set up Previous Button
        view.findViewById(R.id.btnPrevDate).setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            updateSelectedDate();
        });

        // Set up Next Button
        view.findViewById(R.id.btnNextDate).setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            updateSelectedDate();
        });

        return view;
    }

    private void fetchDataFromFirestore(String year, String month, String day) {
        if (db != null) {
            String path = "MHElectric/" + year + "/" + month + "/" + day;
            Log.d(TAG, "Firestore path: " + path); // Log the path

            db.collection("MHElectric")
                    .document(year)
                    .collection(month)
                    .document(day)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                ImportFragment.Data data = document.toObject(ImportFragment.Data.class);

                                // Log the data retrieved
                                Log.d(TAG, "Room: " + data.getRoom() +
                                        ", Event: " + data.getEvent() +
                                        ", TotalElectricBill: " + data.getTotalMoney());

                                dataMap.put(selectedDate, data);
                                setupCombinedChart();
                                updateAdditionalData();
                            } else {
                                showToast("No data found for the selected date.");
                            }
                        } else {
                            showToast("Error fetching data. Please try again.");
                        }
                    });
        } else {
            Log.d(TAG, "Firestore DB is null!");
        }
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void setupCombinedChart() {
        CombinedData combinedData = new CombinedData();

        if (dataMap != null && dataMap.containsKey(selectedDate)) {
            ImportFragment.Data dataEntry = dataMap.get(selectedDate);

            // Bar Data
            ArrayList<BarEntry> barEntries = new ArrayList<>();
            barEntries.add(new BarEntry(0, (float) dataEntry.getFnbMoney()));
            barEntries.add(new BarEntry(1, (float) dataEntry.getRoomMoney()));
            barEntries.add(new BarEntry(2, (float) dataEntry.getSpaMoney()));
            barEntries.add(new BarEntry(3, (float) dataEntry.getAdminPublicMoney()));

            BarDataSet barDataSet = new BarDataSet(barEntries, "Money Spent");
            barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
            BarData barData = new BarData(barDataSet);
            barData.setBarWidth(0.3f); // Adjust the bar width

            // Line Data
            ArrayList<Entry> lineEntries = new ArrayList<>();
            lineEntries.add(new Entry(0, (float) dataEntry.getFnbKWh()));
            lineEntries.add(new Entry(1, (float) dataEntry.getRoomKWh()));
            lineEntries.add(new Entry(2, (float) dataEntry.getSpaKWh()));
            lineEntries.add(new Entry(3, (float) dataEntry.getAdminPublicKWh()));

            LineDataSet lineDataSet = new LineDataSet(lineEntries, "kWh Consumed");
            lineDataSet.setColor(ColorTemplate.getHoloBlue());
            lineDataSet.setLineWidth(2.5f);
            lineDataSet.setCircleColor(ColorTemplate.getHoloBlue());
            lineDataSet.setCircleRadius(5f);
            lineDataSet.setFillColor(ColorTemplate.getHoloBlue());
            lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            lineDataSet.setDrawValues(true);
            LineData lineData = new LineData(lineDataSet);

            // Combine both Bar and Line data into CombinedData
            combinedData.setData(barData);
            combinedData.setData(lineData);

            // Set data to CombinedChart
            combinedChart.setData(combinedData);

            // Configure the left Y-Axis (for Bar Chart)
            YAxis leftAxis = combinedChart.getAxisLeft();
            leftAxis.setAxisMinimum(0f); // Set min value
            leftAxis.setAxisMaximum(1000f); // Set max value, adjust according to your data range
            leftAxis.setDrawGridLines(false);
            leftAxis.setLabelCount(4, true); // Number of labels
            leftAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.format("%,.0f VND", value); // Format the label as currency
                }
            });

            // Configure the right Y-Axis (for Line Chart)
            YAxis rightAxis = combinedChart.getAxisRight();
            rightAxis.setAxisMinimum(0f); // Set min value
            rightAxis.setAxisMaximum(100f); // Set max value, adjust according to your data range
            rightAxis.setDrawGridLines(false);
            rightAxis.setLabelCount(4, true); // Number of labels
            rightAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.format("%,.0f kWh", value); // Format the label with kWh
                }
            });

            // Set the X-Axis
            XAxis xAxis = combinedChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"F&B", "Room", "Spa", "Admin-Public"}));
            xAxis.setGranularity(1f); // Only intervals of 1f (1 unit) on the X-axis
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // Set labels to be at the bottom
            xAxis.setDrawGridLines(false); // Disable grid lines for clarity
            xAxis.setAxisMinimum(0); // Ensure X-axis starts from 0
            xAxis.setAxisMaximum(4); // Ensure X-axis ends at the number of categories
            xAxis.setCenterAxisLabels(true); // Center the labels
            xAxis.setLabelRotationAngle(-30f); // Rotate the labels 30 degrees

            // Set the description text to an empty string
            combinedChart.getDescription().setText(""); // Set the description text to empty

            // Refresh the chart
            combinedChart.invalidate(); // Refresh the chart
        }
    }

    private void showDatePickerDialog() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year1, monthOfYear, dayOfMonth) -> {
                    calendar.set(year1, monthOfYear, dayOfMonth);
                    updateSelectedDate();
                }, year, month, day);
        datePickerDialog.show();
    }

    private void updateSelectedDate() {
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1; // Months are indexed from 0
        int year = calendar.get(Calendar.YEAR);

        selectedDate = String.format("%d/%d/%d", day, month, year);
        txtSelectDate.setText(selectedDate);

        // Convert month to the abbreviated format (e.g., 1 -> Jan, 9 -> Sep)
        String monthAbbreviation = getMonthAbbreviation(month);

        // Fetch data from Firestore using the selected year, month abbreviation, and day
        fetchDataFromFirestore(String.valueOf(year), monthAbbreviation, String.valueOf(day));
    }

    private String getMonthAbbreviation(int month) {
        // Array of month abbreviations
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return months[month - 1]; // Subtract 1 since months are indexed from 0
    }

    private void updateAdditionalData() {
        if (dataMap.containsKey(selectedDate)) {
            ImportFragment.Data dataEntry = dataMap.get(selectedDate);
            txtRoomData.setText(String.valueOf(dataEntry.getRoom()));
            txtEventData.setText(String.valueOf(dataEntry.getEvent()));
            txtTotalMoney.setText(String.format("%,.0f VND", dataEntry.getTotalMoney()));
            txtTotalKWh.setText(String.format("%,.0f kWh", dataEntry.getTotalKWh()));

            // Log the additional data
            Log.d(TAG, "Room: " + dataEntry.getRoom() +
                    ", Event: " + dataEntry.getEvent() +
                    ", TotalMoney: " + dataEntry.getTotalMoney() +
                    ", TotalKWh: " + dataEntry.getTotalKWh());
        } else {
            txtRoomData.setText("N/A");
            txtEventData.setText("N/A");
            txtTotalMoney.setText("N/A");
            txtTotalKWh.setText("N/A");
        }
    }

    public void setDataMap(Map<String, ImportFragment.Data> dataMap) {
        this.dataMap = dataMap;
        // Optionally update the chart and other UI elements if necessary
        updateSelectedDate();
    }

    private String getCurrentYear() {
        return String.valueOf(calendar.get(Calendar.YEAR));
    }

    private String getCurrentMonthAbbreviation() {
        return getMonthAbbreviation(calendar.get(Calendar.MONTH) );
    }

    private String getCurrentDay() {
        return String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
    }
}
