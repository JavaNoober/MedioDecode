package com.noob.mediodecode;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
	VideoPlayView playView;
	private static final String strVideo = Environment.getExternalStorageDirectory().getPath() + "/qwwz.mp4";
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
		new SoundDecodeThread(strVideo).start();
	}
}
