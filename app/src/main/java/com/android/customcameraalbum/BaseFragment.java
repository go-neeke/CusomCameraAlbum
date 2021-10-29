package com.android.customcameraalbum;

import androidx.fragment.app.Fragment;

import com.android.customcameraalbum.listener.HandleBackInterface;
import com.android.customcameraalbum.utils.HandleBackUtil;

/**
 * 录音、视频、音频的fragment继承于他
 * @author zhongjh
 */
public abstract class BaseFragment extends Fragment implements HandleBackInterface {

    @Override
    public boolean onBackPressed() {
        return HandleBackUtil.handleBackPress(this);
    }

}
