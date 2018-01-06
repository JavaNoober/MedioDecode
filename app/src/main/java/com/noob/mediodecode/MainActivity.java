package com.noob.mediodecode;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
	VideoPlayView playView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		playView = findViewById(R.id.player);
		//获取所支持的编码信息的方法
		HashMap<String, MediaCodecInfo.CodecCapabilities> mEncoderInfos = new HashMap<>();
		for(int i = MediaCodecList.getCodecCount() - 1; i >= 0; i--){
			MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
			if(codecInfo.isEncoder()){
				for(String t : codecInfo.getSupportedTypes()){
					try{
						mEncoderInfos.put(t, codecInfo.getCapabilitiesForType(t));
					} catch(IllegalArgumentException e){
						e.printStackTrace();
					}
				}
			}
		}
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
