package com.wfy.learn;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.wfy.learn.utils.DecoderUtils;

import java.io.IOException;
import java.nio.ByteBuffer;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "tag";
    private SurfaceView sfv;
    String rootDirectory = Environment.getExternalStorageDirectory() + "/test.mp4";
    String url = "http://img-test.youbesun.com/1,037febcefd68";
    MediaExtractor extractor;
    MediaCodec codec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sfv = findViewById(R.id.sfv);
        sfv.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    extractor = new MediaExtractor();
                    extractor.setDataSource(MainActivity.this, Uri.parse(url), null);
                    simplePlayer();
                    playerAudio();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    /**
     * 采样率
     */
    private int mSampleRate = -1;

    /**
     * 声音通道数量
     */
    private int mChannels = 1;

    /**
     * PCM采样位数
     */
    private int mPCMEncodeBit = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * 音频播放器
     */
    private AudioTrack mAudioTrack;

    /**
     * 音频数据缓存
     */
    private short[] mAudioOutTempBuf;


    MediaExtractor audioExtractor;

    private void playerAudio() {
        try {
            audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(MainActivity.this, Uri.parse(url), null);
            MediaFormat selTrackFmt = DecoderUtils.chooseVideoTrack(extractor, true);
            MediaCodecList codecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
            MediaCodec codec = MediaCodec.createByCodecName(codecList.findDecoderForFormat(selTrackFmt));
            codec.configure(selTrackFmt, null, null, 0);

            initAudioTrace(selTrackFmt, codec);


            codec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    ByteBuffer buffer = codec.getInputBuffer(index);
                    //媒体抽流器，抽取视频流，将读取到视频流填入buffer中
                    int sampleSize = audioExtractor.readSampleData(buffer, 0);
                    // 2、填入数据、并将其交给 MediaCodec
                    if (sampleSize < 0) {
                        //BUFFER_FLAG_END_OF_STREAM -> 读取视频流结束
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        long sampleTime = audioExtractor.getSampleTime();
                        codec.queueInputBuffer(index, 0, sampleSize, sampleTime, 0);
                        audioExtractor.advance();
                    }
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo bufferInfo) {
                    // 3、MediaCodec 处理数据后，将处理后的数据放在一个空的 output buffer
                    ByteBuffer outputBuffer = codec.getOutputBuffer(index);

                    mAudioOutTempBuf = new short[bufferInfo.size];
                    outputBuffer.position(0);
                    outputBuffer.asShortBuffer().get(mAudioOutTempBuf, 0, bufferInfo.size);
                    mAudioTrack.write(mAudioOutTempBuf, 0, bufferInfo.size);

                    //释放缓冲区
                    codec.releaseOutputBuffer(index, false);
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

                }
            });
            codec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initAudioTrace(MediaFormat format, MediaCodec codec) {
        mChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            mPCMEncodeBit = format.getInteger(MediaFormat.KEY_PCM_ENCODING);
        } else {
            mPCMEncodeBit = AudioFormat.ENCODING_PCM_16BIT;
        }


        int channel;
        if (mChannels == 1) {
            //单声道
            channel = AudioFormat.CHANNEL_OUT_MONO;
        } else {
            //双声道
            channel = AudioFormat.CHANNEL_OUT_STEREO;
        }

        //获取最小缓冲区
        int minBufferSize = AudioTrack.getMinBufferSize(mSampleRate, channel, mPCMEncodeBit);

        mAudioOutTempBuf = new short[minBufferSize / 2];


        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,//播放类型：音乐
                mSampleRate, //采样率
                channel, //通道
                mPCMEncodeBit, //采样位数
                minBufferSize, //缓冲区大小
                AudioTrack.MODE_STREAM); //播放模式：数据流动态写入，另一种是一次性写入
        mAudioTrack.play();
    }

    private void simplePlayer() {
        try {

            DecoderUtils.dumpFormat(extractor);
            DecoderUtils.displayDecoders();

            MediaFormat selTrackFmt = DecoderUtils.chooseVideoTrack(extractor, false);
            codec = DecoderUtils.createCodec(selTrackFmt, sfv.getHolder().getSurface());
            codec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    //1、请求一个空的输入 input buffer    拿到一个被清空的buffer
                    ByteBuffer buffer = codec.getInputBuffer(index);
                    //媒体抽流器，抽取视频流，将读取到视频流填入buffer中
                    int sampleSize = extractor.readSampleData(buffer, 0);

                    // 2、填入数据、并将其交给 MediaCodec
                    if (sampleSize < 0) {
                        //BUFFER_FLAG_END_OF_STREAM -> 读取视频流结束
                        codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        long sampleTime = extractor.getSampleTime();
                        codec.queueInputBuffer(index, 0, sampleSize, sampleTime, 0);
                        extractor.advance();
                    }
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                    // 3、MediaCodec 处理数据后，将处理后的数据放在一个空的 output buffer
                    ByteBuffer outputBuffer = codec.getOutputBuffer(index);

                    //4、获取填充数据了的 output buffer，得到其中的数据，然后将其返还给 MediaCodec
                    MediaFormat bufferFormat = codec.getOutputFormat(index);
                    //释放缓冲区
                    codec.releaseOutputBuffer(index, true);
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                    Log.e(TAG, "" + e.getMessage());
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    Log.i(TAG, "onOutputFormatChanged: " + format);
                }
            });
//            codec.configure();
            codec.start();
//            codec.stop();
////            codec.release();
////            codec.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
