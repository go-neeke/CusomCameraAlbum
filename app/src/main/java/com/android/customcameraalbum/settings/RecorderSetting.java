package com.android.customcameraalbum.settings;

import com.android.customcameraalbum.settings.api.RecorderSettingApi;

/**
 * 录音机
 * @author zhongjh
 */
public final class RecorderSetting implements RecorderSettingApi {

    private final RecordeSpec mRecordeSpec;

    public RecorderSetting() {
        mRecordeSpec = RecordeSpec.getCleanInstance();
    }

    @Override
    public RecorderSetting duration(int duration) {
        mRecordeSpec.duration = duration;
        return this;
    }

    @Override
    public RecorderSetting minDuration(int minDuration) {
        mRecordeSpec.minDuration = minDuration;
        return this;
    }

}
