package com.fr.facedetection;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SettingActivity extends Activity {

    public Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        Button runBtn = findViewById(R.id.buttonRun);

        runBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = getSharedPreferences("MyPref", MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();

                TextView companyId = findViewById(R.id.comanyID);
                editor.putInt("company_id", Integer.parseInt(companyId.getText().toString()) );

                editor.commit();

                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);
            }
        });
    }

}
