package com.osmino.sova.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.osmino.sova.R;

import java.util.ArrayDeque;

import static com.osmino.sova.model.AssistantController.ASSISTANT_EMOTION;
import static com.osmino.sova.model.AssistantController.ASSISTANT_EMOTION_PARAM;


public class AvatarFragment extends Fragment {
    private static final String TAG = "AvatarFragment";
    private TextureVideoView videoView;
    private ArrayDeque<Integer> emotionsQueue = new ArrayDeque<>();
    private int lastEmotion = 0;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ASSISTANT_EMOTION.equals(action)) {
                int emotion = intent.getIntExtra(ASSISTANT_EMOTION_PARAM, 0);
                Log.d(TAG, "Receive emotion: " + emotion);
                int rawEmotionId = 0;
                switch (emotion) {
                    case 0: rawEmotionId = R.raw.idle_out; break;
                    case 1: rawEmotionId = R.raw.hi; break;
                    case 2: rawEmotionId = R.raw.no; break;
                    case 3: rawEmotionId = R.raw.yes; break;
                    case 4: rawEmotionId = R.raw.idle; break;
                    case 5: rawEmotionId = R.raw.idk; break;
                }

                if (rawEmotionId > 0) {
                    if (rawEmotionId == R.raw.idle) {
                        if (lastEmotion != R.raw.idle && lastEmotion != R.raw.idle_in) {
                            addEmotionToPlay(R.raw.idle_in);
                        }
                    } else {
                        if ((lastEmotion == R.raw.idle || lastEmotion == R.raw.idle_in) && rawEmotionId != R.raw.idle_out) {
                            addEmotionToPlay(R.raw.idle_out);
                        }
                    }
                    if (lastEmotion == R.raw.idle_out && rawEmotionId == R.raw.idle_out) {
                        return;
                    }
                    addEmotionToPlay(rawEmotionId);
                }
            }
        }
    };

    public AvatarFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_avatar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ASSISTANT_EMOTION);
        requireContext().registerReceiver(mReceiver, filter);

        videoView = view.findViewById(R.id.videoView);
        videoView.setOnCompletionListener(mp -> {
            Log.d(TAG, "Video completed");
            emotionsQueue.pollFirst();
            pullNextEmotion();
        });
    }

    private void addEmotionToPlay(int emotionId) {
        if (isDetached() || !isResumed()) {
            Log.v(TAG, "View detached, skip emotion play");
            return;
        }
        Log.d(TAG, "emotionsQueue size: " + emotionsQueue.size());

        if (emotionsQueue.isEmpty()) {
            emotionsQueue.addLast(emotionId);
            pullNextEmotion();
        } else {
            emotionsQueue.addLast(emotionId);
        }
    }

    private void pullNextEmotion(){
        if (videoView.isPlaying()) {
            Log.d(TAG, "return");
            return;
        }

        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                Integer nextEmotion = emotionsQueue.peekFirst();
                if (nextEmotion != null) {
                    Log.d(TAG, "Next emotion: " + nextEmotion);
                    if (!playEmotion(nextEmotion)) {
                        emotionsQueue.pollFirst();
                    } else {
                        lastEmotion = nextEmotion;
                    }
                }
            });
        } else {
            emotionsQueue.pollFirst();
        }
    }

    private boolean playEmotion(int emotionId) {
        if (isDetached() || !isResumed()) {
            return false;
        }
        if (videoView.isPlaying()) {
            Log.w(TAG, "Warning! video is playing");
        }

        String path = "android.resource://" + requireContext().getPackageName() + "/" + emotionId;
        Log.d(TAG, "Play video: " + path);
        videoView.setVideoURI(Uri.parse(path));
        videoView.start();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        videoView.seekTo(1);
        if (!emotionsQueue.isEmpty()) {
            pullNextEmotion();
        } else {
            addEmotionToPlay(R.raw.idle_in);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }
}