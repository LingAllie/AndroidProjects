package com.tnl.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
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
import com.github.mikephil.charting.utils.ColorTemplate;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private CombinedChart combinedChart;
    private TextView txtSelectDate;
    private TextView txtRoomData;
    private TextView txtEventData;
    private TextView txtTotalMoney;
    private TextView txtTotalKWh;
    private String selectedDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        combinedChart = findViewById(R.id.combinedChart);
        txtSelectDate = findViewById(R.id.txtSelectDate);
        txtRoomData = findViewById(R.id.txtRoomData);
        txtEventData = findViewById(R.id.txtEventData);
        txtTotalMoney = findViewById(R.id.txtTotalMoney);
        txtTotalKWh = findViewById(R.id.txtTotalKWh);

        // Set up Combined Chart
        setupCombinedChart();

        // Set up Date Picker TextView
        txtSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the current date
                final Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                // Create DatePickerDialog
                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                                txtSelectDate.setText(selectedDate);
                                updateAdditionalData(); // Update data based on the selected date
                            }
                        }, year, month, day);
                datePickerDialog.show();
            }
        });
    }

    private void setupCombinedChart() {
        CombinedData data = new CombinedData();

        // Bar Data
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        barEntries.add(new BarEntry(1, 200));
        barEntries.add(new BarEntry(2, 400));
        barEntries.add(new BarEntry(3, 600));
        barEntries.add(new BarEntry(4, 800));

        BarDataSet barDataSet = new BarDataSet(barEntries, "Money Spent");
        barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.3f); // Adjust the bar width to make bars thinner or thicker

        // Line Data
        ArrayList<Entry> lineEntries = new ArrayList<>();
        lineEntries.add(new Entry(1, 50));
        lineEntries.add(new Entry(2, 60));
        lineEntries.add(new Entry(3, 70));
        lineEntries.add(new Entry(4, 80));

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
        data.setData(barData);
        data.setData(lineData);

        // Set data to CombinedChart
        combinedChart.setData(data);

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
        xAxis.setAxisMaximum(5); // Ensure X-axis ends at the number of categories + 1
        xAxis.setCenterAxisLabels(true); // Center the labels

        // Rotate the X-axis labels diagonally
        xAxis.setLabelRotationAngle(-30f); // Rotate the labels 30 degrees

        // Set the description text to an empty string
        combinedChart.getDescription().setText(""); // Set the description text to empty

        // Adjust the space between bars
        combinedChart.getBarData().setBarWidth(0.7f); // Adjust this value to change the bar width

        // Refresh the chart
        combinedChart.invalidate(); // Refresh the chart
    }

    private void updateAdditionalData() {
        // Example values
        int rooms = 10; // Replace with your actual value
        int events = 5; // Replace with your actual value
        float totalMoney = 12345.67f; // Replace with your actual value
        float totalKWh = 678.90f; // Replace with your actual value

        txtRoomData.setText("Rooms: " + rooms);
        txtEventData.setText("Events: " + events);
        txtTotalMoney.setText("Total Money: " + totalMoney + " VND");
        txtTotalKWh.setText("Total kWh: " + totalKWh + " kWh");
    }
}
