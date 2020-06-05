package com.wfy.learn.utils;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

public class DecoderUtils {
    private static final String TAG = "tag";

    public static void dumpFormat(MediaExtractor extractor) {
        int count = extractor.getTrackCount();
        Log.i(TAG, "playVideo: track count: " + count);
        for (int i = 0; i < count; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            Log.i(TAG, "playVideo: track " + i + ":" + getTrackInfo(format));
        }
    }

    public static String getTrackInfo(MediaFormat format) {
        String info = format.getString(MediaFormat.KEY_MIME);
        if (info.startsWith("audio/")) {
            info += " samplerate: " + format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    + ", channel count:" + format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        } else if (info.startsWith("video/")) {
            info += " size:" + format.getInteger(MediaFormat.KEY_WIDTH) + "x" + format.getInteger(MediaFormat.KEY_HEIGHT);
        }
        return info;
    }

    public static void displayDecoders() {
        MediaCodecList list = new MediaCodecList(MediaCodecList.REGULAR_CODECS);//REGULAR_CODECS参考api说明
        MediaCodecInfo[] codecs = list.getCodecInfos();
        for (MediaCodecInfo codec : codecs) {
            if (codec.isEncoder())
                continue;
            Log.i(TAG, "displayDecoders: " + codec.getName());
        }
    }

    public static MediaFormat chooseVideoTrack(MediaExtractor extractor, boolean isAudio) {
        int count = extractor.getTrackCount();
        for (int i = 0; i < count; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            if (!isAudio && format.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
                extractor.selectTrack(i);//选择轨道
                return format;
            } else if (isAudio && format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                extractor.selectTrack(i);//选择轨道
                return format;
            }
        }
        return null;
    }

    public static MediaFormat chooseAudioTrack(MediaExtractor extractor) {
        int count = extractor.getTrackCount();
        for (int i = 0; i < count; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                extractor.selectTrack(i);//选择轨道
                return format;
            }
        }
        return null;
    }


    public static MediaCodec createCodec(MediaFormat format, Surface surface) throws IOException {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        MediaCodec codec = MediaCodec.createByCodecName(codecList.findDecoderForFormat(format));
        codec.configure(format, surface, null, 0);
        codec.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        return codec;
    }
}
