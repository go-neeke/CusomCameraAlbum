package com.android.customcameraalbum.recorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.customcameraalbum.BaseFragment;
import com.android.customcameraalbum.MainActivity;
import com.android.customcameraalbum.R;
import com.android.customcameraalbum.camera.common.Constants;
import com.android.customcameraalbum.camera.listener.ClickOrLongListener;
import com.android.customcameraalbum.camera.util.FileUtil;
import com.android.customcameraalbum.recorder.db.RecordingItem;
import com.android.customcameraalbum.recorder.widget.SoundRecordingLayout;
import com.android.customcameraalbum.settings.GlobalSpec;
import com.android.customcameraalbum.settings.RecordeSpec;
import com.android.customcameraalbum.utils.BitmapUtils;
import com.android.customcameraalbum.utils.ViewBusinessUtils;
import com.android.customcameraalbum.widget.BaseOperationLayout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.android.customcameraalbum.common.enums.MultimediaTypes;
import com.android.customcameraalbum.common.utils.MediaStoreCompat;
import com.android.customcameraalbum.utils.StatusBarUtils;
import com.android.customcameraalbum.common.utils.ThreadUtils;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;
import static com.android.customcameraalbum.camera.common.Constants.BUTTON_STATE_ONLY_LONG_CLICK;
import static com.android.customcameraalbum.camera.common.Constants.TYPE_PICTURE;
import static com.android.customcameraalbum.constants.Constant.EXTRA_MULTIMEDIA_CHOICE;
import static com.android.customcameraalbum.constants.Constant.EXTRA_MULTIMEDIA_TYPES;
import static com.android.customcameraalbum.constants.Constant.EXTRA_RESULT_RECORDING_ITEM;

/**
 * ??????
 *
 * @author zhongjh
 * @date 2018/8/22
 */
public class SoundRecordingFragment extends BaseFragment {

    private static final String TAG = SoundRecordingFragment.class.getSimpleName();
    /**
     * ???????????????2?????????
     */
    private final static int AGAIN_TIME = 2000;
    protected Activity mActivity;
    private Context mContext;

    RecordeSpec mRecordSpec;
    MediaStoreCompat mAudioMediaStoreCompat;

    /**
     * ?????????????????????
     */
    private boolean isPlaying = false;
    private ViewHolder mViewHolder;

    /**
     * ???????????????????????????????????????
     */
    long timeWhenPaused = 0;

    private MediaPlayer mMediaPlayer = null;
    /**
     * ???????????????
     */
    RecordingItem recordingItem;

    /**
     * ????????????long???????????????????????????????????????????????????????????????
     */
    private long mExitTime;

    // region ??????????????????

    private File mFile = null;

    private MediaRecorder mRecorder = null;

    private long mStartingTimeMillis = 0;
    // endregion

    public static SoundRecordingFragment newInstance() {
        return new SoundRecordingFragment();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        this.mActivity = activity;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.mContext = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mViewHolder = new ViewHolder(inflater.inflate(R.layout.fragment_soundrecording_zjh, container, false));

        // ???????????????
        mRecordSpec = RecordeSpec.getInstance();
        // ????????????
        mViewHolder.pvLayout.setTip(getResources().getString(R.string.z_multi_library_long_press_sound_recording));
        // ??????????????????
        mViewHolder.pvLayout.setDuration(mRecordSpec.duration * 1000);
        // ??????????????????
        mViewHolder.pvLayout.setMinDuration(mRecordSpec.minDuration);
        // ??????????????????
        mViewHolder.pvLayout.setButtonFeatures(BUTTON_STATE_ONLY_LONG_CLICK);

        // ?????????????????????
        int statusBarHeight = StatusBarUtils.getStatusBarHeight(mContext);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mViewHolder.chronometer.getLayoutParams();
        layoutParams.setMargins(layoutParams.leftMargin, layoutParams.topMargin + statusBarHeight, layoutParams.rightMargin, layoutParams.bottomMargin);

        initListener();
        return mViewHolder.rootView;
    }

    @Override
    public boolean onBackPressed() {
        // ??????????????????????????????
        if (mViewHolder.pvLayout.mState == Constants.STATE_PREVIEW) {
            return false;
        } else {
            // ????????????????????????????????????
            if ((System.currentTimeMillis() - mExitTime) > AGAIN_TIME) {
                // ??????2000ms??????????????????????????????Toast????????????
                Toast.makeText(mActivity.getApplicationContext(), getResources().getString(R.string.z_multi_library_press_confirm_again_to_close), Toast.LENGTH_SHORT).show();
                // ???????????????????????????????????????????????????????????????????????????
                mExitTime = System.currentTimeMillis();
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * ??????
     */
    private void initListener() {
        // ???????????????
        initPvLayoutPhotoVideoListener();
        // ????????????
        initRlSoundRecordingClickListener();

        // ???????????????
        initPvLayoutOperateListener();
    }

    /**
     * ???????????????
     */
    private void initPvLayoutPhotoVideoListener() {
        mViewHolder.pvLayout.setPhotoVideoListener(new ClickOrLongListener() {
            @Override
            public void actionDown() {
                // ?????????????????????
                ViewBusinessUtils.setTabLayoutScroll(false, ((MainActivity) mActivity), mViewHolder.pvLayout);
            }

            @Override
            public void onClick() {

            }

            @Override
            public void onLongClickShort(long time) {
                Log.d(TAG, "onLongClickShort" + time);
                mViewHolder.pvLayout.setTipAlphaAnimation(getResources().getString(R.string.z_multi_library_the_recording_time_is_too_short));  // ????????????
                // ????????????
                new Handler(Looper.getMainLooper()).postDelayed(() -> onRecord(false, true), mRecordSpec.minDuration - time);
                mViewHolder.chronometer.setBase(SystemClock.elapsedRealtime());
                // ?????????????????????
                ViewBusinessUtils.setTabLayoutScroll(true, ((MainActivity) mActivity), mViewHolder.pvLayout);
            }

            @Override
            public void onLongClick() {
                Log.d(TAG, "onLongClick");
                // ????????????
                onRecord(true, false);
            }

            @Override
            public void onLongClickEnd(long time) {
                mViewHolder.pvLayout.hideBtnClickOrLong();
                mViewHolder.pvLayout.startShowLeftRightButtonsAnimator();
                Log.d(TAG, "onLongClickEnd");
                // ????????????
                onRecord(false, false);
                showRecordEndView();
            }

            @Override
            public void onLongClickError() {

            }
        });
    }

    /**
     * ????????????
     */
    private void initRlSoundRecordingClickListener() {
        ((SoundRecordingLayout.ViewHolder) mViewHolder.pvLayout.viewHolder).rlSoundRecording.setOnClickListener(view -> {
            initAudio();
            // ??????
            onPlay(isPlaying);
            isPlaying = !isPlaying;
        });
    }

    /**
     * ???????????????
     */
    private void initPvLayoutOperateListener() {
        mViewHolder.pvLayout.setOperateListener(new BaseOperationLayout.OperateListener() {
            @Override
            public void cancel() {
                // ?????????????????????
                ViewBusinessUtils.setTabLayoutScroll(true, ((MainActivity) mActivity), mViewHolder.pvLayout);
                // ????????????????????????
                mViewHolder.pvLayout.reset();
                // ????????????
                mViewHolder.chronometer.setBase(SystemClock.elapsedRealtime());
            }

            @Override
            public void confirm() {
            }

            @Override
            public void preview() {

            }

        });
    }

    /**
     * ????????????????????????
     */
    private void initAudio() {
        // ??????service???????????????
        recordingItem = new RecordingItem();
        SharedPreferences sharePreferences = mActivity.getSharedPreferences("sp_name_audio", MODE_PRIVATE);
        final String filePath = sharePreferences.getString("audio_path", "");
        long elapsed = sharePreferences.getLong("elapsed", 0);
        recordingItem.setFilePath(filePath);
        recordingItem.setLength((int) elapsed);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mMediaPlayer != null) {
            stopPlaying();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            stopPlaying();
        }
    }


    /**
     * ????????????????????????
     * // recording pause
     *
     * @param start   ????????????????????????
     * @param isShort ??????????????????
     */
    private void onRecord(boolean start, boolean isShort) {
        if (start) {
            // ????????????
            File folder = new File(mActivity.getExternalFilesDir(null) + "/SoundRecorder");
            if (!folder.exists()) {
                // folder /SoundRecorder doesn't exist, create the folder
                boolean wasSuccessful = folder.mkdir();
                if (!wasSuccessful) {
                    System.out.println("was not successful.");
                }
            }
            // ????????????,???1???????????????
            mViewHolder.chronometer.setBase(SystemClock.elapsedRealtime() - 1000);
            mViewHolder.chronometer.start();

            // start RecordingService
            startRecording();
            // keep screen on while recording
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            mViewHolder.chronometer.stop();
            timeWhenPaused = 0;

            stopRecording(isShort);
            // allow the screen to turn off again once recording is finished
            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /**
     * ????????????????????????
     * // Play start/stop
     *
     * @param isPlaying ??????????????????
     */
    private void onPlay(boolean isPlaying) {
        if (!isPlaying) {
            // currently MediaPlayer is not playing audio
            if (mMediaPlayer == null) {
                startPlaying(); // ???????????????
            } else {
                resumePlaying(); // ????????????????????????????????????
            }

        } else {
            // ????????????
            pausePlaying();
        }
    }

    /**
     * ??????MediaPlayer
     */
    private void startPlaying() {
        // ?????????????????????
        ((SoundRecordingLayout.ViewHolder) mViewHolder.pvLayout.viewHolder).ivRecord.setImageResource(R.drawable.ic_pause_white_24dp);
        mMediaPlayer = new MediaPlayer();

        try {
            // ????????????
            mMediaPlayer.setDataSource(recordingItem.getFilePath());
            mMediaPlayer.prepare();

            mMediaPlayer.setOnPreparedListener(mp -> mMediaPlayer.start());
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        mMediaPlayer.setOnCompletionListener(mp -> stopPlaying());

        //keep screen on while playing audio
        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * ????????????
     */
    private void resumePlaying() {
        // ?????????
        ((SoundRecordingLayout.ViewHolder) mViewHolder.pvLayout.viewHolder).ivRecord.setImageResource(R.drawable.ic_pause_white_24dp);
        mMediaPlayer.start();
    }

    /**
     * ????????????
     */
    private void pausePlaying() {
        // ????????????????????????
        ((SoundRecordingLayout.ViewHolder) mViewHolder.pvLayout.viewHolder).ivRecord.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        mMediaPlayer.pause();
    }

    /**
     * ????????????
     */
    private void stopPlaying() {
        // ????????????????????????
        ((SoundRecordingLayout.ViewHolder) mViewHolder.pvLayout.viewHolder).ivRecord.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        // ??????mediaPlayer
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        mMediaPlayer.release();
        mMediaPlayer = null;

        isPlaying = !isPlaying;

        // ????????????????????????????????????????????? ??????????????????
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * ????????????????????????
     */
    private void showRecordEndView() {
        // ????????????????????????????????????????????????
        ((SoundRecordingLayout.ViewHolder) mViewHolder.pvLayout.viewHolder).ivRecord.setImageResource(R.drawable.ic_play_arrow_white_24dp);
    }

    /**
     * ??????????????????
     */
    private void moveRecordFile() {
        // ??????????????????
        ThreadUtils.executeByIo(new ThreadUtils.BaseSimpleBaseTask<Void>() {
            @Override
            public Void doInBackground() {
                // ?????????????????????????????????
                initAudio();
                // ??????????????????
                String newFileName = recordingItem.getFilePath().substring(recordingItem.getFilePath().lastIndexOf(File.separator));
                File newFile = mAudioMediaStoreCompat.createFile(newFileName, 2, false);
                Log.d(TAG, "newFile" + newFile.getAbsolutePath());
                FileUtil.copy(new File(recordingItem.getFilePath()), newFile, null, (ioProgress, file) -> {
                    int progress = (int) (ioProgress * 100);
                    ThreadUtils.runOnUiThread(() -> {
                        recordingItem.setFilePath(newFile.getPath());
                        if (progress >= 100) {
                            // ?????? ??????????????????
                            Intent result = new Intent();
                            result.putExtra(EXTRA_RESULT_RECORDING_ITEM, recordingItem);
                            result.putExtra(EXTRA_MULTIMEDIA_TYPES, MultimediaTypes.AUDIO);
                            result.putExtra(EXTRA_MULTIMEDIA_CHOICE, false);
                            mActivity.setResult(RESULT_OK, result);
                            mActivity.finish();
                        }
                    });
                });
                return null;
            }

            @Override
            public void onSuccess(Void result) {

            }
        });
    }

    // region ????????????????????????

    /**
     * ????????????
     */
    private void startRecording() {

        // ??????????????????????????????
        GlobalSpec globalSpec = GlobalSpec.getInstance();
        // ????????????????????????
        mAudioMediaStoreCompat = new MediaStoreCompat(getContext());
        mAudioMediaStoreCompat.setSaveStrategy(globalSpec.audioStrategy == null ? globalSpec.saveStrategy : globalSpec.audioStrategy);
        mFile = mAudioMediaStoreCompat.createFile(2, true);

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFile.getPath());
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setAudioChannels(1);

        try {
            mRecorder.prepare();
            mRecorder.start();
            mStartingTimeMillis = System.currentTimeMillis();

            //startTimer();
            //startForeground(1, createNotification());

        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }
    }

    /**
     * ????????????
     *
     * @param isShort ??????????????????
     */
    private void stopRecording(boolean isShort) {
        mViewHolder.pvLayout.setEnabled(false);

        ThreadUtils.executeByIo(new ThreadUtils.BaseTask<Boolean>() {
            @Override
            public Boolean doInBackground() {
                if (isShort) {
                    // ???????????????????????????????????????
                    if (mFile.exists()) {
                        boolean delete = mFile.delete();
                        if (!delete) {
                            System.out.println("file not delete.");
                        }
                    }
                } else {
                    long mElapsedMillis = (System.currentTimeMillis() - mStartingTimeMillis);
                    // ??????????????????????????????
                    mActivity.getSharedPreferences("sp_name_audio", MODE_PRIVATE)
                            .edit()
                            .putString("audio_path", mFile.getPath())
                            .putLong("elapsed", mElapsedMillis)
                            .apply();
                }
                if (mRecorder != null) {
                    try {
                        mRecorder.stop();
                    } catch (RuntimeException ignored) {
                        // ????????????????????????
                    }
                    mRecorder.release();
                    mRecorder = null;
                }
                return true;
            }

            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    mViewHolder.pvLayout.setEnabled(true);
                }
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onFail(Throwable t) {

            }
        });
    }

    // endregion

    public static class ViewHolder {
        View rootView;
        public Chronometer chronometer;
        public SoundRecordingLayout pvLayout;

        public ViewHolder(View rootView) {
            this.rootView = rootView;
            this.chronometer = rootView.findViewById(R.id.chronometer);
            this.pvLayout = rootView.findViewById(R.id.pvLayout);
        }

    }
}
