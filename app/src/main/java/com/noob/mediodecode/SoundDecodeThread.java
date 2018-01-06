package com.noob.mediodecode;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by xiaoqi on 2018/1/5.
 */

public class SoundDecodeThread extends Thread {

	private final static String TAG = "SoundDecodeThread";

	private MediaCodec mediaCodec;

	private AudioPlayer mPlayer;
	private String path;

	public SoundDecodeThread(String path) {
		this.path = path;
	}

	@Override
	public void run() {
		MediaExtractor mediaExtractor = new MediaExtractor();
		try {
			mediaExtractor.setDataSource(path); // 设置数据源
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		String mimeType;
		for (int i = 0; i < mediaExtractor.getTrackCount(); i++) { // 信道总数
			MediaFormat format = mediaExtractor.getTrackFormat(i); // 音频文件信息
			mimeType = format.getString(MediaFormat.KEY_MIME);
			if (mimeType.startsWith("audio/")) { // 音频信道
				mediaExtractor.selectTrack(i); // 切换到 音频信道
				try {
					mediaCodec = MediaCodec.createDecoderByType(mimeType); // 创建解码器,提供数据输出
				} catch (IOException e) {
					e.printStackTrace();
				}
				mediaCodec.configure(format, null, null, 0);
				mPlayer = new AudioPlayer(format.getInteger(MediaFormat.KEY_SAMPLE_RATE), AudioFormat
						.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
				mPlayer.init();
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
				int inIndex = mediaCodec.dequeueInputBuffer(0);
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

			int outIndex = mediaCodec.dequeueOutputBuffer(info, 0);
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

					while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
						try {
							sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						}
					}
					//用来保存解码后的数据
					byte[] outData = new byte[info.size];
					buffer.get(outData);
					//清空缓存
					buffer.clear();
					//播放解码后的数据
					mPlayer.play(outData, 0, info.size);
					mediaCodec.releaseOutputBuffer(outIndex, true);
					break;
			}

			// All decoded frames have been rendered, we can stop playing
			// now
			if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
				break;
			}
		}

		mediaCodec.stop();
		mediaCodec.release();
		mediaExtractor.release();
	}
}
