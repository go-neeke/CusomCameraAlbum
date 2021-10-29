package com.android.customcameraalbum.settings;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.customcameraalbum.camera.listener.OnCameraViewListener;
import com.android.customcameraalbum.settings.api.CameraSettingApi;

import com.android.customcameraalbum.common.coordinator.VideoEditCoordinator;
import com.android.customcameraalbum.common.enums.MimeType;

import java.util.Set;

/**
 * 有关拍摄界面的动态设置
 *
 * @author zhongjh
 * @date 2018/12/26
 */
public class CameraSetting implements CameraSettingApi {

    private final CameraSpec mCameraSpec;


    public CameraSetting() {
        mCameraSpec = CameraSpec.getCleanInstance();
    }

    @Override
    public void onDestroy() {
        mCameraSpec.onCameraViewListener = null;
    }

    @Override
    public CameraSetting mimeTypeSet(@NonNull Set<MimeType> mimeTypes) {
        mCameraSpec.mimeTypeSet = mimeTypes;
        return this;
    }

    @Override
    public CameraSetting duration(int duration) {
        mCameraSpec.duration = duration;
        return this;
    }

    @Override
    public CameraSetting minDuration(int minDuration) {
        mCameraSpec.minDuration = minDuration;
        return this;
    }

    @Override
    public CameraSetting videoEdit(VideoEditCoordinator videoEditManager) {
        mCameraSpec.videoEditCoordinator = videoEditManager;
        return this;
    }

    @Override
    public CameraSetting useImgFlash(boolean useImgFlash) {
        mCameraSpec.useImgFlash = useImgFlash;
        return this;
    }

    @Override
    public CameraSetting isSectionRecord(boolean isSectionRecord) {
        mCameraSpec.isSectionRecord = isSectionRecord;
        return this;
    }

    @Override
    public CameraSetting watermarkResource(int watermarkResource) {
        mCameraSpec.watermarkResource = watermarkResource;
        return this;
    }

    @Override
    public CameraSetting imageSwitch(int imageSwitch) {
        mCameraSpec.imageSwitch = imageSwitch;
        return this;
    }

    @Override
    public CameraSetting imageFlashOn(int imageFlashOn) {
        mCameraSpec.imageFlashOn = imageFlashOn;
        return this;
    }

    @Override
    public CameraSetting imageFlashOff(int imageFlashOff) {
        mCameraSpec.imageFlashOff = imageFlashOff;
        return this;
    }

    @Override
    public CameraSetting imageFlashAuto(int imageFlashAuto) {
        mCameraSpec.imageFlashAuto = imageFlashAuto;
        return this;
    }

    @Override
    public CameraSetting setOnCameraViewListener(@Nullable OnCameraViewListener listener) {
        mCameraSpec.onCameraViewListener = listener;
        return this;
    }


}
