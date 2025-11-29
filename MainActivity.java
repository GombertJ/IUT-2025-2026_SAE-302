package com.example.bowsershell;

import android.os.Bundle;
import android.util.Log;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String API_URL = "http://localhost:8000/failles/v1/cves/";
    private static final String TAG = "MainActivity";

    private RequestQueue requestQueue;
    private ArrayAdapter<String> myAdapter;
    private ArrayList<String> dataList = new ArrayList<>();

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

        ListView myList = (ListView) findViewById(R.id.listView);
        myAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
        myList.setOnItemClickListener(this::onItemClick);
        myList.setAdapter(myAdapter);

        requestQueue = Volley.newRequestQueue(this);

        fetchCveList();
    }

    private void fetchCveList() {
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                API_URL,
                null,

                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            dataList.clear();

                            for (int i = 0; i < response.length(); i++) {
                                JSONObject cveObject = response.getJSONObject(i);

                                String name = cveObject.getString("name");
                                String target = cveObject.getString("target");
                                String state = cveObject.getString("state");

                                String displayString = name + " ; " + target + " ; " + state;

                                dataList.add(displayString);
                            }

                            myAdapter.notifyDataSetChanged();

                        } catch (JSONException e) {
                            Log.e(TAG, "Erreur de parsing JSON: " + e.getMessage());
                            Toast.makeText(MainActivity.this, "Erreur de données reçues.", Toast.LENGTH_LONG).show();
                        }
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Erreur Volley: " + error.toString());
                        Toast.makeText(MainActivity.this, "Erreur de connexion à l'API.", Toast.LENGTH_LONG).show();
                    }
                }
        );

        requestQueue.add(jsonArrayRequest);
    }

    public void onItemClick (AdapterView<?>p, View v,int pos, long id) {
        String cveClicked = dataList.get(pos);
        Toast.makeText(getApplicationContext(),"Clic sur : "+ cveClicked,Toast.LENGTH_SHORT).show();
        MediaPlayer.create(getApplicationContext(), R.raw.bowser).start();
    }
}