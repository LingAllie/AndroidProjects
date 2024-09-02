package com.tnl.statistic.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tnl.statistic.ui.dashboard.ImportFragment;

import java.util.Map;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<Map<String, ImportFragment.Data>> dataMap = new MutableLiveData<>();

    public LiveData<Map<String, ImportFragment.Data>> getDataMap() {
        return dataMap;
    }

    public void setDataMap(Map<String, ImportFragment.Data> dataMap) {
        this.dataMap.setValue(dataMap);
    }
}