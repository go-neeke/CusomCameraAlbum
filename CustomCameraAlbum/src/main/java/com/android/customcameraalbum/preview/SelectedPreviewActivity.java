package com.android.customcameraalbum.preview;

import android.os.Bundle;
import androidx.annotation.Nullable;

import com.android.customcameraalbum.settings.GlobalSpec;
import com.android.customcameraalbum.album.model.SelectedItemCollection;

import java.util.List;

import com.android.customcameraalbum.common.entity.MultiMedia;

/**
 * 预览界面
 * @author zhongjh
 */
public class SelectedPreviewActivity extends BasePreviewActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!GlobalSpec.getInstance().hasInited) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        Bundle bundle = getIntent().getBundleExtra(EXTRA_DEFAULT_BUNDLE);
        List<MultiMedia> selected = bundle.getParcelableArrayList(SelectedItemCollection.STATE_SELECTION);
        mAdapter.addAll(selected);
        mAdapter.notifyDataSetChanged();
        if (mAlbumSpec.countable) {
            mViewHolder.checkView.setCheckedNum(1);
        } else {
            mViewHolder.checkView.setChecked(true);
        }
        mPreviousPos = 0;
        updateSize(selected.get(0));
    }

}