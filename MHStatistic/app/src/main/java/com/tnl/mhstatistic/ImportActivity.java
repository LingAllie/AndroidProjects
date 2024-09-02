package com.tnl.mhstatistic;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class ImportActivity extends Fragment {

    private RecyclerView recyclerViewFolder;
    private FloatingActionButton floatBtnFolder;
    private List<String> folderList;
    private FolderAdapter folderAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import, container, false);

        recyclerViewFolder = view.findViewById(R.id.recyclerViewFolder);
        floatBtnFolder = view.findViewById(R.id.floatBtnFolder);

        folderList = new ArrayList<>();
        folderAdapter = new FolderAdapter(folderList, this::onFolderClick);
        recyclerViewFolder.setAdapter(folderAdapter);
        recyclerViewFolder.setLayoutManager(new GridLayoutManager(getContext(), 2));

        floatBtnFolder.setOnClickListener(v -> showCreateFolderDialog());

        return view;
    }

    private void showCreateFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Create New Folder");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_create_folder, null);
        builder.setView(dialogView);

        EditText edtFolderName = dialogView.findViewById(R.id.edtFolderName);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String folderName = edtFolderName.getText().toString().trim();
            if (!folderName.isEmpty()) {
                createFolder(folderName);
            } else {
                Toast.makeText(getContext(), "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void createFolder(String folderName) {
        folderList.add(folderName);
        folderAdapter.notifyDataSetChanged();
    }

    private void onFolderClick(String folderName) {
        // Navigate to ListExcelFragment with the selected folder name
        ListExcel listExcelFragment = new ListExcel();
        Bundle args = new Bundle();
        args.putString("FOLDER_NAME", folderName);
        listExcelFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, listExcelFragment)
                .addToBackStack(null)
                .commit();
    }
}
