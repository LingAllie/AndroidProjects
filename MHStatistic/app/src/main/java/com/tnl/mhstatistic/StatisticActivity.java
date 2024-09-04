package com.tnl.mhstatistic;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tnl.mhstatistic.shared.SharedViewModel;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticActivity extends Fragment {

    private SharedViewModel viewModel;

    private TextView txtSelectDate;
    private Button btnPrevDate, btnNextDate;
    private CombinedChart combinedChart;
    private TextView txtRoomData, txtEventData, txtTotalKWh, txtTotalMoney;
    private FirebaseFirestore db;

    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private Map<String, Object> data;

    private static final String TAG = "StatisticActivity";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistic, container, false);

        // Initialize Views and ViewModel
        txtSelectDate = view.findViewById(R.id.txtSelectDate);
        combinedChart = view.findViewById(R.id.combinedChart);
        txtRoomData = view.findViewById(R.id.txtRoomData);
        txtEventData = view.findViewById(R.id.txtEventData);
        txtTotalKWh = view.findViewById(R.id.txtTotalKWh);
        txtTotalMoney = view.findViewById(R.id.txtTotalMoney);

        Button btnPrevDate = view.findViewById(R.id.btnPrevDate);
        Button btnNextDate = view.findViewById(R.id.btnNextDate);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        db = FirebaseFirestore.getInstance();

        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("d-MM-yyyy", Locale.getDefault());

        // Initialize with current date if no date is selected
        Date initialDate = viewModel.getSelectedDate().getValue();
        if (initialDate == null) {
            initialDate = calendar.getTime();
            viewModel.setSelectedDate(initialDate);
        }
        String currentDate = dateFormat.format(initialDate);
        txtSelectDate.setText(currentDate);

        viewModel.getSelectedDate().observe(getViewLifecycleOwner(), date -> {
            if (date != null) {
                // Log year, month, and date values
                int year = date.getYear() + 1900; // Adjusting for the offset in Date class
                int month = date.getMonth() + 1; // Adjusting for the offset in Date class
                int day = date.getDate();

                Log.d(TAG, "Observed Date: Year=" + year + ", Month=" + month + ", Day=" + day);

                // Format the date and update UI
                String formattedDate = dateFormat.format(date);
                txtSelectDate.setText(formattedDate);

                // Load data for the selected date
                loadDataForDate(year, month, day);
            }
        });


        txtSelectDate.setOnClickListener(v -> showDatePicker());
        btnPrevDate.setOnClickListener(v -> changeDate(-1));
        btnNextDate.setOnClickListener(v -> changeDate(1));

        if (savedInstanceState != null) {
            String savedDateStr = savedInstanceState.getString("selectedDate");
            if (savedDateStr != null) {
                try {
                    Date savedDate = dateFormat.parse(savedDateStr);
                    viewModel.setSelectedDate(savedDate);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing saved date", e);
                }
            }
        }

        combinedChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e instanceof BarEntry) {
                    BarEntry barEntry = (BarEntry) e;
                    int index = (int) barEntry.getX();
                    if (data != null && data.containsKey("F&BFee") && data.containsKey("RoomFee") &&
                            data.containsKey("SpaFee") && data.containsKey("AdminPublicFee")) {
                        showInfoDialog(index);
                    }
                }
            }

            @Override
            public void onNothingSelected() {
                // Handle if needed
            }
        });

        return view;
    }



    private void showDatePicker() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(selectedYear, selectedMonth, selectedDay);
                    Date selectedDate = calendar.getTime();
                    String formattedDate = dateFormat.format(selectedDate);
                    txtSelectDate.setText(formattedDate);
                    viewModel.setSelectedDate(selectedDate); // Set Date in ViewModel
                    loadDataForDate(selectedYear, selectedMonth + 1, selectedDay);
                }, year, month, day);

        datePickerDialog.show();
    }


    private void changeDate(int dayOffset) {
        // Get the currently selected date from the ViewModel
        Date selectedDate = viewModel.getSelectedDate().getValue();

        if (selectedDate != null) {
            // Update calendar with the selected date
            calendar.setTime(selectedDate);
            calendar.add(Calendar.DAY_OF_MONTH, dayOffset);

            // Update the ViewModel with the new date
            Date newDate = calendar.getTime();
            viewModel.setSelectedDate(newDate);

            // Update the UI with the new date
            String selectedDateStr = dateFormat.format(newDate);
            txtSelectDate.setText(selectedDateStr);

            // Load data for the new date
            loadDataForDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
        } else {
            Log.e(TAG, "Selected date is null. Cannot change date.");
        }
    }


    private void loadDataForDate(int year, int month, int day) {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.ENGLISH); // Ensure English locale
        String monthName = monthFormat.format(new Date(year - 1900, month - 1, 1));

        db.collection("MHStatistic")
                .document(String.valueOf(year))
                .collection(monthName) // Use English month name
                .document(String.valueOf(day))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        data = documentSnapshot.getData();
                        if (data != null) {
                            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
                            // For integer values
                            int room = ((Long) data.get("Room")).intValue();
                            int event = ((Long) data.get("Event")).intValue();

                            // For floating-point values
                            float totalKWh = ((Double) data.get("TotalElectricUse")).floatValue();
                            float totalMoney = ((Double) data.get("TotalElectricBill")).floatValue();

                            txtRoomData.setText(String.valueOf(room));
                            txtEventData.setText(String.valueOf(event));
                            txtTotalKWh.setText(decimalFormat.format(totalKWh));
                            txtTotalMoney.setText(decimalFormat.format(totalMoney));
                            updateCharts(data);
                        }
                    } else {
                        Log.d(TAG, "No data found for the selected date.");
                        showDefaultCharts();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading data from Firestore", e);
                    Toast.makeText(requireContext(), "Error loading data from Firestore", Toast.LENGTH_SHORT).show();
                    showDefaultCharts();
                });
    }



    private void updateCharts(Map<String, Object> data) {
        try {
            // Prepare Bar Entries
            List<BarEntry> barEntries = new ArrayList<>();
            barEntries.add(new BarEntry(0, ((Number) data.get("F&BFee")).floatValue()));
            barEntries.add(new BarEntry(1, ((Number) data.get("RoomFee")).floatValue()));
            barEntries.add(new BarEntry(2, ((Number) data.get("SpaFee")).floatValue()));
            barEntries.add(new BarEntry(3, ((Number) data.get("AdminPublicFee")).floatValue()));

            // Prepare Line Entries
            List<Entry> lineEntries = new ArrayList<>();
            lineEntries.add(new Entry(0, ((Number) data.get("F&Bkwh")).floatValue()));
            lineEntries.add(new Entry(1, ((Number) data.get("Roomkwh")).floatValue()));
            lineEntries.add(new Entry(2, ((Number) data.get("Spakwh")).floatValue()));
            lineEntries.add(new Entry(3, ((Number) data.get("AdminPublickwh")).floatValue()));

            // Calculate Min and Max for Bar Chart (Money)
            float barMin = Float.MAX_VALUE;
            float barMax = Float.MIN_VALUE;
            for (BarEntry entry : barEntries) {
                barMin = Math.min(barMin, entry.getY());
                barMax = Math.max(barMax, entry.getY());
            }

            // Calculate Min and Max for Line Chart (kWh)
            float lineMin = Float.MAX_VALUE;
            float lineMax = Float.MIN_VALUE;
            for (Entry entry : lineEntries) {
                lineMin = Math.min(lineMin, entry.getY());
                lineMax = Math.max(lineMax, entry.getY());
            }

            // Get the SpaFee value for Y-axis adjustment
            float spaFee = ((Number) data.get("SpaFee")).floatValue();

            // Set Bar Chart Colors
            int[] colors = new int[] {
                    getResources().getColor(R.color.colorFandB),
                    getResources().getColor(R.color.colorRoom),
                    getResources().getColor(R.color.colorSpa),
                    getResources().getColor(R.color.colorAdminPublic)
            };

            BarDataSet barDataSet = new BarDataSet(barEntries, "Fees");
            barDataSet.setColors(colors);
            barDataSet.setValueTextSize(12f);
            barDataSet.setValueTextColor(getResources().getColor(R.color.colorTextBar));
            BarData barData = new BarData(barDataSet);

            // Adjust Bar Width and Spacing
            float barWidth = 0.6f;
            barData.setBarWidth(barWidth);
            combinedChart.getXAxis().setAxisMinimum(-0.5f); // Add space to the left
            combinedChart.getXAxis().setAxisMaximum(barEntries.size() - 0.5f); // Add space to the right

            // Create Line Data
            LineDataSet lineDataSet = new LineDataSet(lineEntries, "kWh");
            lineDataSet.setColor(getResources().getColor(R.color.colorLine)); // Line color
            lineDataSet.setDrawCircles(true); // Draw circles at data points
            lineDataSet.setDrawValues(false); // Show values on the line
            lineDataSet.setLineWidth(2f); // Set line width for visibility
            lineDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT); // Follow the right Y-Axis
            LineData lineData = new LineData(lineDataSet);

            // Combine Data
            CombinedData combinedData = new CombinedData();
            combinedData.setData(barData);
            combinedData.setData(lineData);
            combinedChart.setData(combinedData);

            // Configure Axes with Money and kWh adjustments
            configureYAxis(barMin, barMax, lineMin, lineMax, spaFee);
            configureXAxis();

            // Set description
            combinedChart.getDescription().setText("");

            // Hide the legend
            combinedChart.getLegend().setEnabled(false);

            combinedChart.setTouchEnabled(true); // Enable touch interactions
            combinedChart.setClickable(true);
            combinedChart.setDragEnabled(true);
            combinedChart.setScaleEnabled(true);
            combinedChart.setPinchZoom(true);

            // Refresh chart
            combinedChart.invalidate();
        } catch (Exception e) {
            Log.e(TAG, "Error updating charts: ", e);
        }
    }


    private void showDefaultCharts() {
        // Show charts with zero values
        List<BarEntry> barEntries = new ArrayList<>();
        barEntries.add(new BarEntry(0, 0f));
        barEntries.add(new BarEntry(1, 0f));
        barEntries.add(new BarEntry(2, 0f));
        barEntries.add(new BarEntry(3, 0f));

        List<Entry> lineEntries = new ArrayList<>();
        lineEntries.add(new Entry(0, 0f));
        lineEntries.add(new Entry(1, 0f));
        lineEntries.add(new Entry(2, 0f));
        lineEntries.add(new Entry(3, 0f));

        BarDataSet barDataSet = new BarDataSet(barEntries, "Fees");
        barDataSet.setColors(new int[]{
                getResources().getColor(R.color.colorFandB),
                getResources().getColor(R.color.colorRoom),
                getResources().getColor(R.color.colorSpa),
                getResources().getColor(R.color.colorAdminPublic)
        });
        BarData barData = new BarData(barDataSet);
        barDataSet.setValueTextSize(12f);
        barDataSet.setValueTextColor(getResources().getColor(R.color.colorTextBar));

        LineDataSet lineDataSet = new LineDataSet(lineEntries, "kWh");
        lineDataSet.setColor(getResources().getColor(R.color.colorLine)); // Line color
        lineDataSet.setValueTextColor(getResources().getColor(R.color.colorTextLine)); // Text color
        lineDataSet.setDrawCircles(true); // Draw circles at data points
        lineDataSet.setDrawValues(true); // Show values on the line
        lineDataSet.setLineWidth(2f); // Set line width for visibility
        lineDataSet.setValueTextSize(12f);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT); // Follow the right Y-Axis
        LineData lineData = new LineData(lineDataSet);

        CombinedData combinedData = new CombinedData();
        combinedData.setData(barData);
        combinedData.setData(lineData);
        combinedChart.setData(combinedData);

        // Configure Axes with default values
        configureYAxis(0f, 0f, 0f, 0f, 0f);
        configureXAxis();

        combinedChart.invalidate(); // Refresh chart
    }

    private void configureYAxis(float barMin, float barMax, float lineMin, float lineMax, float spaFee) {
        // Configure the left Y-Axis (for Money)
        YAxis leftAxis = combinedChart.getAxisLeft();
        float moneyMin = Math.min(barMin, spaFee / 2);
        float moneyMax = Math.max(barMax, spaFee * 2);
        leftAxis.setAxisMinimum(moneyMin);
        leftAxis.setAxisMaximum(moneyMax);
        leftAxis.setDrawGridLines(false);
        leftAxis.setLabelCount(4, true);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%,.0f", value);
            }
        });

        // Configure the right Y-Axis (for kWh)
        YAxis rightAxis = combinedChart.getAxisRight();
        rightAxis.setAxisMinimum(lineMin - (lineMin * 0.1f)); // Adjust minimum to ensure visibility
        rightAxis.setAxisMaximum(lineMax + (lineMax * 0.1f)); // Adjust maximum to ensure visibility
        rightAxis.setDrawGridLines(false);
        rightAxis.setLabelCount(4, true);
        rightAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%,.0f", value);
            }
        });
    }

    private void configureXAxis() {
        XAxis xAxis = combinedChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // Only show integer values
        xAxis.setGranularityEnabled(true);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);

        // Set the labels
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"F&B", "Room", "Spa", "Admin-Public"}));
    }

    private void showInfoDialog(int index) {
        // Map to hold data for each column
        Map<Integer, String[]> columnData = new HashMap<>();
        columnData.put(0, new String[]{"F&B", "F&BFee", "F&Bkwh"});
        columnData.put(1, new String[]{"Room", "RoomFee", "Roomkwh"});
        columnData.put(2, new String[]{"Spa", "SpaFee", "Spakwh"});
        columnData.put(3, new String[]{"Admin-Public", "AdminPublicFee", "AdminPublickwh"});

        // Retrieve column information
        String[] info = columnData.get(index);
        String category = info[0];
        String feeKey = info[1];
        String kwhKey = info[2];

        // Get values from the data map
        float fee = ((Number) data.get(feeKey)).floatValue();
        float kwh = ((Number) data.get(kwhKey)).floatValue();

        // Create and show the dialog
        new AlertDialog.Builder(getContext())
                .setTitle(category)
                .setMessage(String.format("Electric Bill: %,.2f VND\n\nElectric Use: %,.2f kWh", fee, kwh))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
