package com.example.floorplan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PlanAdapter adapter;
    private List<File> fileList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Button btnCreateNew = findViewById(R.id.btnCreateNew);
        btnCreateNew.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ImageSourceActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedPlans();
    }

    private void loadSavedPlans() {
        fileList.clear();
        File dir = getFilesDir();
        File[] files = dir.listFiles((d, name) -> name.startsWith("plan_") && name.endsWith(".png"));

        if (files != null) {
            fileList.addAll(Arrays.asList(files));
        }

        adapter = new PlanAdapter(this, fileList);
        recyclerView.setAdapter(adapter);
    }
}