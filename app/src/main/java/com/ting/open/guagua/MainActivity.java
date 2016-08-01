package com.ting.open.guagua;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.ting.open.guagua.View.GuaKa;

/**
 * Created by lt on 2016/7/31.
 */
public class MainActivity extends Activity {

    private GuaKa mGuaKa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGuaKa = (GuaKa) findViewById(R.id.gua_ka);
        mGuaKa.setOnGuaKaCompleteListener(new GuaKa.OnGuaKaCompleteListener() {
            @Override
            public void complete() {
                Toast.makeText(MainActivity.this,"guagua",Toast.LENGTH_SHORT).show();
            }
        });

        mGuaKa.setText("haha");
    }
}
