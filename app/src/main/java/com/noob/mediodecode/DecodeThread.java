package com.noob.mediodecode;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by xiaoqi on 2018/1/5.
 */

public class DecodeThread extends Thread {

	private final static String TAG = "DecodeThread";

	private MediaExtractor mediaExtractor;
	/** 用来读取音視频文件 提取器 */
	private MediaCodec mediaCodec;
	/** 用来解码 解碼器 */
	private Surface surface;

	private String path;

	public DecodeThread(Surface surface, String path) {
		this.surface = surface;
		this.path = path;
	}

	@Override
	public void run() {
		mediaExtractor = new MediaExtractor();
		try {
			mediaExtractor.setDataSource(path); // 设置数据源
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		String mimeType = null;
		for (int i = 0; i < mediaExtractor.getTrackCount(); i++) { // 信道总数
			MediaFormat format = mediaExtractor.getTrackFormat(i); // 音频文件信息
			mimeType = format.getString(MediaFormat.KEY_MIME);
			if (mimeType.startsWith("video/")) { // 视频信道
				mediaExtractor.selectTrack(i); // 切换到视频信道
				try {
					mediaCodec = MediaCodec.createDecoderByType(mimeType); // 创建解码器,提供数据输出
				} catch (IOException e) {
					e.printStackTrace();
				}
				mediaCodec.configure(format, surface, null, 0);
				break;
			}

		}
		if (mediaCodec == null) {
			Log.e(TAG, "Can't find video info!");
			return;
		}

		mediaCodec.start(); // 启动MediaCodec ，等待传入数据
		ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers(); // 用来存放目标文件的数据
		// 输入
		ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers(); // 解码后的数据
		// 输出
		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo(); // 用于描述解码得到的byte[]数据的相关信息
		boolean bIsEos = false;
		long startMs = System.currentTimeMillis();

		// ==========开始解码=============
		while (!Thread.interrupted()) {

			if (!bIsEos) {
				int inIndex = mediaCodec.dequeueInputBuffer(10000);
				if (inIndex >= 0) {
					ByteBuffer buffer = inputBuffers[inIndex];
					int nSampleSize = mediaExtractor.readSampleData(buffer, 0); // 读取一帧数据至buffer中
					if (nSampleSize < 0) {
						Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
						mediaCodec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
						bIsEos = true;
					} else {
						// 填数据
						mediaCodec.queueInputBuffer(inIndex, 0, nSampleSize, mediaExtractor.getSampleTime(), 0); // 通知MediaDecode解码刚刚传入的数据
						mediaExtractor.advance(); // 继续下一取样
					}
				}
			}

			int outIndex = mediaCodec.dequeueOutputBuffer(info, 10000);
			switch (outIndex) {
				case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
					Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
					outputBuffers = mediaCodec.getOutputBuffers();
					break;
				case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
					Log.d(TAG, "New format " + mediaCodec.getOutputFormat());
					break;
				case MediaCodec.INFO_TRY_AGAIN_LATER:
					Log.d(TAG, "dequeueOutputBuffer timed out!");
					break;
				default:
					ByteBuffer buffer = outputBuffers[outIndex];
					Log.v(TAG, "We can't use this buffer but render it due to the API limit, " + buffer);

					// We use a very simple clock to keep the video FPS, or the
					// video
					// playback will be too fast
					while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
						try {
							sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						}
					}

//					if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
//						 这里可以从ByteBuffer中获取csd参数
//						mediaCodec.releaseOutputBuffer(outIndex, false);
//						return;
//					}
					mediaCodec.releaseOutputBuffer(outIndex, true);
					break;
			}

			// All decoded frames have been rendered, we can stop playing
			// now
			if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
				Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
				break;
			}
		} // end while

		mediaCodec.stop();
		mediaCodec.release();
		mediaExtractor.release();
	}
}
