package com.example.bowsershell;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.media.MediaPlayer;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MediaPlayer.create(getApplicationContext(), R.raw.bowser).start();
        String[] myStringArray = {"PHP 8.1.0","Port 22 open","Not secured equipement on ...","SQL Injections available through ...","XSS exploit detected on ...","Vulnerability n°6","Vulnerability n°7","Vulnerability n°8","Vulnerability n°9","Vulnerability n°10","Vulnerability n°11","Vulnerability n°12","Vulnerability n°13","Vulnerability n°14","Vulnerability n°15","Vulnerability n°16","Vulnerability n°17","Vulnerability n°18","Vulnerability blablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablabla"};

        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, myStringArray);
        ListView myList = (ListView) findViewById(R.id.listView);
        myList.setOnItemClickListener(this::onItemClick);
        myList.setAdapter(myAdapter);
    }

    public void onItemClick (AdapterView<?>p, View v,int pos, long id) {
        Toast.makeText(getApplicationContext(),"Clic à la position"+(pos+1),Toast.LENGTH_SHORT).show();
        MediaPlayer.create(getApplicationContext(), R.raw.bowser).start();
    }
}