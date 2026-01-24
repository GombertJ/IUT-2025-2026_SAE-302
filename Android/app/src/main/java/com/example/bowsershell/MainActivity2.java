package com.example.bowsershell;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.RequestQueue;

public class MainActivity2 extends AppCompatActivity {

    private String stt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView CVETitle = findViewById(R.id.CVETitle);
        TextView Infos = findViewById(R.id.infos);

        Bundle extras = getIntent().getExtras();
        assert extras != null;
        String CVEName = extras.getString("CVEName");
        String CVEId = extras.getString("CVEId");
        String CVETarget = extras.getString("CVETarget");
        String CVEState = extras.getString("CVEState");
        String CVEInfos = extras.getString("CVEInfos");

        String CVEDetails = "Informations de la CVE\n\n\t●  Id: " + CVEId + "\n\n\t●  Cible: " + CVETarget + "\n\n\t●  Etat: " + stt + "\n\n\t●  Informations supplementaires: " + "\n"+CVEInfos;

        if (CVEState != "open"){
            stt = "<font color='#cc0029'>"+CVEDetails+"</font>";
        }


        CVETitle.setText(CVEName);
        Infos.setText(CVEDetails);
    }

    public void returnToMain(View v) {
        Intent act_1 = new Intent(this, MainActivity.class);
        startActivity(act_1);
    }
}