package com.android.customcameraalbum.preview;

import static android.app.Activity.RESULT_OK;
import static com.android.customcameraalbum.constants.Constant.EXTRA_MULTIMEDIA_CHOICE;
import static com.android.customcameraalbum.constants.Constant.EXTRA_MULTIMEDIA_TYPES;
import static com.android.customcameraalbum.constants.Constant.EXTRA_RESULT_SELECTION;
import static com.android.customcameraalbum.constants.Constant.EXTRA_RESULT_SELECTION_PATH;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.gowtham.library.utils.CompressOption;
import com.gowtham.library.utils.TrimVideo;

import java.io.File;
import java.util.ArrayList;

import com.android.customcameraalbum.common.enums.MultimediaTypes;

public class VideoPreview {
    public static void previewVideo(AppCompatActivity context, String url) {
        ActivityResultLauncher<Intent> startActivityResult = context.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                        }
                    }
                });

        TrimVideo.activity(url)
                .setCompressOption(new CompressOption()) //empty constructor for default compress option
                .setHideSeekBar(true)
                .setEnableEdit(false)
                .start(context, startActivityResult);
    }
}
