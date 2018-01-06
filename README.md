# 使用MediaCodec硬解码h.265视频及音频进行播放

h.265这个视频是很多播放器不支持的，就算是bilibili开源的ijkplayer也不能直接播放，需要自己去重新编译
才可以支持。
这里通过这个demo来演示一下如何硬解码视频，播放h.265视频，其实编码的视频同样道理。

视频的播放主要在surfaceView中显示，而解码过程则在**音频解码线程**和**视频解码线程**两个线程中分别执行。

## 视频解码

主要是用到了一个MediaCodec这个类来进行解码。

### 设置数据源

    MediaExtractor mediaExtractor = new MediaExtractor();
    try {
        mediaExtractor.setDataSource(path); // 设置数据源
    } catch (IOException e1) {
        e1.printStackTrace();
    }
### 根据视频的编码信息来初始化MediaCodec:

视频的mineType是video类型。

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
    mediaCodec.start(); // 启动MediaCodec ，等待传入数据

### 获取缓存器

    // 输入
    ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers(); // 用来存放目标文件的数据
    // 输出
    ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers(); // 解码后的数据
    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo(); // 用于描述解码得到的byte[]数据的相关信息

### 开始解码

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

                mediaCodec.releaseOutputBuffer(outIndex, true);
                break;
        }

        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
            break;
        }
    }
    
### 解码完成后释放资源

    mediaCodec.stop();
    mediaCodec.release();
    mediaExtractor.release();
    
  这样视频的解码就已经完成了，此时surfaceView已经可以播放视频了，接下来是音频解码。
   
   
## 音频解码
  
  音频解码的过程和上面大同小异，主要区别在于，视频是用surfaceView播放显示的，而音频我们需要使用AudioTrack来播放。
  
### 创建一个AudioPlayer类用于播放音频


    public class AudioPlayer {
        private int mFrequency;// 采样率
        private int mChannel;// 声道
        private int mSampBit;// 采样精度
        private AudioTrack mAudioTrack;
    
        public AudioPlayer(int frequency, int channel, int sampbit) {
            this.mFrequency = frequency;
            this.mChannel = channel;
            this.mSampBit = sampbit;
        }
    
        /**
         * 初始化
         */
        public void init() {
            if (mAudioTrack != null) {
                release();
            }
            // 获得构建对象的最小缓冲区大小
            int minBufSize = AudioTrack.getMinBufferSize(mFrequency, mChannel, mSampBit);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    mFrequency, mChannel, mSampBit, minBufSize, AudioTrack.MODE_STREAM);
            mAudioTrack.play();
        }
    
        /**
         * 释放资源
         */
        private void release() {
            if (mAudioTrack != null) {
                mAudioTrack.stop();
                mAudioTrack.release();
            }
        }
    
        /**
         * 将解码后的pcm数据写入audioTrack播放
         *
         * @param data   数据
         * @param offset 偏移
         * @param length 需要播放的长度
         */
        public void play(byte[] data, int offset, int length) {
            if (data == null || data.length == 0) {
                return;
            }
            try {
                mAudioTrack.write(data, offset, length);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
### 初始化音频解码器：

音频的mineType是audio类型，我们根据这个来去音频信息即可。
    
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
    
### 音频解码：
音频解码过程与视频解码大同小异，只需要额外调用一下我们创建的AudioPlayer来播放音频即可。

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
            Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
            break;
        }
    }
    
## 效果展示

视频编码信息,为h.265:

![视频编码信息](https://raw.githubusercontent.com/JavaNoober/MedioDecode/master/%E7%BC%96%E7%A0%81%E4%BF%A1%E6%81%AF.jpg)

播放效果（带声音）:

![视频播放](https://raw.githubusercontent.com/JavaNoober/MedioDecode/master/%E8%A7%A3%E7%A0%81%E6%92%AD%E6%94%BE.gif)

## 获取MediaCodec支持解码的编码格式:

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
 
![](https://raw.githubusercontent.com/JavaNoober/MedioDecode/master/%E6%94%AF%E6%8C%81%E7%9A%84%E7%BC%96%E7%A0%81%E4%BF%A1%E6%81%AF.png)

## 完整demo地址

使用的时候将assets下的h265.mp4复制到sd卡即可

gitHub地址，欢迎star:[https://github.com/JavaNoober/MedioDecode](https://github.com/JavaNoober/MedioDecode)


    