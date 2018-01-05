package com.noob.mediodecode;

import android.content.Context;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by xiaoqi on 2018/1/5.
 */

public class VideoPlayView extends SurfaceView implements SurfaceHolder.Callback {

	private static final String strVideo = Environment.getExternalStorageDirectory().getPath() + "/qwwz.mp4";

	private DecodeThread thread;

	public VideoPlayView(Context context) {
		super(context);
	}

	public VideoPlayView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public VideoPlayView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.e("VideoPlayView", "surfaceCreated");
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.e("VideoPlayView", "surfaceChanged");
		if (thread == null) {
			thread = new DecodeThread(holder.getSurface(), strVideo);
			thread.start();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.e("VideoPlayView", "surfaceDestroyed");
		if (thread != null) {
			thread.interrupt();
		}
	}

	public void start(){
		thread = new DecodeThread(getHolder().getSurface(), strVideo);
		thread.start();
	}
}
