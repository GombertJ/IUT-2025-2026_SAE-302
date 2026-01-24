package com.example.bowsershell;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String API_URL = "http://10.0.2.2:8000/v1/cves/";
    private static final String TAG = "MainActivity";

    private RequestQueue requestQueue;
    private ArrayAdapter<String> myAdapter;
    private final ArrayList<String> dataList = new ArrayList<>();
    private final List<String> IdList = new ArrayList<>();

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


        ListView myList = findViewById(R.id.listView);
        myAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);
        myList.setOnItemClickListener(this::onItemClick);
        myList.setAdapter(myAdapter);
        SearchView searchView = findViewById(R.id.searchView);

        requestQueue = Volley.newRequestQueue(this);

        fetchCveList(null);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String newString) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                fetchCveList(query);
                return false;
            }
        });
    }

    private void fetchCveList(String query) {
        String url = API_URL;

        if (query != null && !query.isEmpty()) {
            url += "?q=" + query;
        }

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        dataList.clear();
                        IdList.clear();

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject cve = response.getJSONObject(i);

                            String name = cve.getString("name");
                            String target = cve.getString("target");
                            String state = cve.getString("state");

                            String display = name + " | " + target + " | " + state;

                            dataList.add(display);
                            IdList.add(cve.getString("id"));
                        }

                        myAdapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        Toast.makeText(this, "Erreur de parsing JSON", Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(this, "Erreur API", Toast.LENGTH_LONG).show()
        );

        requestQueue.add(request);
    }


    public void onItemClick (AdapterView<?>p, View v,int pos, long id) {

        Intent act_2 = new Intent(this, MainActivity2.class);

        String CVEId = IdList.get(pos);
        String API_URL_CVE = "http://10.0.2.2:8000/v1/cves/"+CVEId;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                API_URL_CVE,
                null,

                response -> {
                    try {

                        act_2.putExtra("CVEName", response.getString("name"));
                        act_2.putExtra("CVEId", CVEId);
                        act_2.putExtra("CVETarget", response.getString("target"));
                        act_2.putExtra("CVEState", response.getString("state"));
                        act_2.putExtra("CVEInfos", response.getString("infos"));

                        startActivity(act_2);

                    } catch (JSONException e) {
                        Log.e(TAG, "Erreur de parsing JSON: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "Erreur de données reçues.", Toast.LENGTH_LONG).show();
                    }
                },

                error -> {
                    Log.e(TAG, "Erreur Volley: " + error.toString());
                    Toast.makeText(MainActivity.this, "Erreur de connexion à l'API.", Toast.LENGTH_LONG).show();
                }
        );

        requestQueue.add(jsonObjectRequest);
    }
}