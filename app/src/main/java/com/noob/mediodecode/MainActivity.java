package com.noob.mediodecode;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
	VideoPlayView playView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		playView = findViewById(R.id.player);
	}

	@Override
	protected void onResume() {
		super.onResume();
		playView.start();
	}

	@Override
	protected void onPause() {
		super.onPause();
		playView.stop();
	}
}
