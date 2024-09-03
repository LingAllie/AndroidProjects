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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tnl.adapter.FolderAdapter;
import com.tnl.shared.SharedViewModel;

import java.util.ArrayList;
import java.util.List;

public class ManageActivity extends Fragment {

    private RecyclerView recyclerViewFolder;
    private FloatingActionButton floatBtnFolder;
    private List<String> folderList;
    private FolderAdapter folderAdapter;
    private SharedViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_import, container, false);

        recyclerViewFolder = view.findViewById(R.id.recyclerViewFolder);
        floatBtnFolder = view.findViewById(R.id.floatBtnFolder);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        viewModel.loadFoldersAndFiles(requireActivity());

        folderList = new ArrayList<>();
        folderAdapter = new FolderAdapter(folderList, this::onFolderClick, this::onFolderLongClick);
        recyclerViewFolder.setAdapter(folderAdapter);
        recyclerViewFolder.setLayoutManager(new GridLayoutManager(getContext(), 4));

        viewModel.getFolderList().observe(getViewLifecycleOwner(), folderList -> {
            folderAdapter.updateFolders(folderList); // Update the adapter with the new folder list
        });

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
        viewModel.addFolder(this.requireContext(),folderName); // Add the folder to the ViewModel
        folderAdapter.notifyDataSetChanged(); // Notify the adapter that the data has changed
    }


    private void onFolderClick(String folderName) {
        // Navigate to ListExcelFragment with the selected folder name
        ImportActivity listExcelFragment = new ImportActivity();
        Bundle args = new Bundle();
        args.putString("FOLDER_NAME", folderName);
        listExcelFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, listExcelFragment)
                .addToBackStack(null)
                .commit();
    }

    private void onFolderLongClick(String folderName, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Options");

        builder.setItems(new String[]{"Rename", "Delete"}, (dialog, which) -> {
            switch (which) {
                case 0: // Rename
                    showRenameFolderDialog(folderName, position);
                    break;
                case 1: // Delete
                    showDeleteConfirmationDialog(folderName, position);
                    break;
            }
        });

        builder.create().show();
    }

    private void showRenameFolderDialog(String oldFolderName, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Rename Folder");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_rename_folder, null);
        builder.setView(dialogView);

        EditText edtFolderName = dialogView.findViewById(R.id.edtFolderName);
        edtFolderName.setText(oldFolderName);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newFolderName = edtFolderName.getText().toString().trim();
            if (!newFolderName.isEmpty()) {
                viewModel.renameFolder(this.requireContext(), oldFolderName, newFolderName);
                folderAdapter.notifyItemChanged(position); // Notify the adapter that the item has changed
            } else {
                Toast.makeText(getContext(), "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }


    private void showDeleteConfirmationDialog(String folderName, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Confirm Deletion");
        builder.setMessage("Are you sure you want to delete this folder?");

        builder.setPositiveButton("Yes", (dialog, which) -> deleteFolder(folderName, position));
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void renameFolder(String oldFolderName, String newFolderName, int position) {
        viewModel.removeFolder(this.requireContext(), oldFolderName);
        viewModel.addFolder(this.requireContext(), newFolderName);
        folderAdapter.notifyItemChanged(position); // Notify the adapter that the item has changed
    }

    private void deleteFolder(String folderName, int position) {
        viewModel.removeFolder(this.requireContext(), folderName);
        folderAdapter.notifyItemRemoved(position);
    }
}
