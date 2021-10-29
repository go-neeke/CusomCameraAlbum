package com.android.customcameraalbum.camera;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.gowtham.library.utils.CompressOption;
import com.gowtham.library.utils.TrimVideo;
import com.android.customcameraalbum.BaseFragment;
import com.android.customcameraalbum.MainActivity;
import com.android.customcameraalbum.R;
import com.android.customcameraalbum.album.model.SelectedItemCollection;
import com.android.customcameraalbum.camera.common.Constants;
import com.android.customcameraalbum.camera.entity.BitmapData;
import com.android.customcameraalbum.camera.listener.CaptureListener;
import com.android.customcameraalbum.camera.listener.ClickOrLongListener;
import com.android.customcameraalbum.camera.listener.ErrorListener;
import com.android.customcameraalbum.camera.listener.OperateCameraListener;
import com.android.customcameraalbum.camera.util.FileUtil;
import com.android.customcameraalbum.preview.BasePreviewActivity;
import com.android.customcameraalbum.settings.CameraSpec;
import com.android.customcameraalbum.settings.GlobalSpec;
import com.android.customcameraalbum.utils.BitmapUtils;
import com.android.customcameraalbum.utils.ViewBusinessUtils;


import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

import com.android.customcameraalbum.common.entity.MultiMedia;
import com.android.customcameraalbum.common.enums.MultimediaTypes;
import com.android.customcameraalbum.common.listener.VideoEditListener;
import com.android.customcameraalbum.common.utils.MediaStoreCompat;
import com.android.customcameraalbum.common.utils.ThreadUtils;

import static android.app.Activity.RESULT_OK;
import static com.android.customcameraalbum.camera.common.Constants.TYPE_VIDEO;
import static com.android.customcameraalbum.preview.BasePreviewActivity.REQ_IMAGE_EDIT;
import static com.android.customcameraalbum.constants.Constant.EXTRA_MULTIMEDIA_CHOICE;
import static com.android.customcameraalbum.constants.Constant.EXTRA_MULTIMEDIA_TYPES;
import static com.android.customcameraalbum.constants.Constant.EXTRA_RESULT_SELECTION;
import static com.android.customcameraalbum.constants.Constant.EXTRA_RESULT_SELECTION_PATH;
import static com.android.customcameraalbum.constants.Constant.REQUEST_CODE_PREVIEW_CAMRRA;
import static com.android.customcameraalbum.constants.Constant.REQUEST_CODE_PREVIEW_VIDEO;

/**
 * 拍摄视频
 *
 * @author zhongjh
 * @date 2018/8/22
 */
public class CameraFragment extends BaseFragment implements TrimVideo.CompressBuilderListener {

    private Activity mActivity;

    private CameraLayout mCameraLayout;
    private final static int MILLISECOND = 2000;

    /**
     * 声明一个long类型变量：用于存放上一点击“返回键”的时刻
     */
    private long mExitTime;
    /**
     * 是否提交,如果不是提交则要删除冗余文件
     */
    private boolean mIsCommit = false;

    private Dialog dialog;

    private final ActivityResultLauncher<Intent> startForResult =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK &&
                                result.getData() != null) {
                            File newFile = new File(TrimVideo.getTrimmedVideoPath(result.getData()));
                            Uri uri = Uri.fromFile(newFile);
                            Log.d("A.lee", "Trimmed path:: " + uri);
                            Intent resultIntent = new Intent();
                            ArrayList<String> arrayList = new ArrayList<>();
                            arrayList.add(newFile.getPath());
                            ArrayList<Uri> selectedUris = new ArrayList<>();
                            selectedUris.add(uri);
                            resultIntent.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, arrayList);
                            resultIntent.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris);
                            resultIntent.putExtra(EXTRA_MULTIMEDIA_TYPES, MultimediaTypes.VIDEO);
                            resultIntent.putExtra(EXTRA_MULTIMEDIA_CHOICE, true);
                            mActivity.setResult(RESULT_OK, resultIntent);
                            mIsCommit = true;
                            mActivity.finish();
                        } else {

                        }
//                            LogMessage.v("videoTrimResultLauncher data is null");
                    });


    public static CameraFragment newInstance() {
        CameraFragment cameraFragment = new CameraFragment();
        Bundle args = new Bundle();
        cameraFragment.setArguments(args);
        return cameraFragment;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(@NotNull Activity activity) {
        super.onAttach(activity);
        this.mActivity = activity;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera_zjh, container, false);

        view.setOnKeyListener((v, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK);

        mCameraLayout = view.findViewById(R.id.cameraLayout);
        mCameraLayout.setFragment(this);
        mCameraLayout.setErrorListener(new ErrorListener() {
            @Override
            public void onError() {
                //错误监听
                Log.i("CameraActivity", "camera error");
            }

            @Override
            public void onAudioPermissionError() {
            }
        });

        initCameraLayoutCloseListener();
        initCameraLayoutPhotoVideoListener();
        initCameraLayoutOperateCameraListener();
        initCameraLayoutCaptureListener();
        initCameraLayoutEditListener();

        return view;
    }

    public void goVideoTrim(Uri path) {
        TrimVideo.activity(String.valueOf(path))
                .setEnableEdit(true)
                .setCompressOption(new CompressOption()) //empty constructor for default compress option
//                .setCompressOption(new CompressOption(30,"1M",460,320))
                .start(mActivity, startForResult);
    }

    public void confirmVideo(Uri path) {
        TrimVideo.activity(String.valueOf(path))
                .setEnableEdit(false)
                .setExecute(true)
                .setCompressOption(new CompressOption()) //empty constructor for default compress option
//                .setCompressOption(new CompressOption(30,"1M",460,320))
                .start(mActivity, startForResult);
//        showProcessingDialog();


//        TrimVideo.CompressBuilder compressBuilder = TrimVideo.compress(mActivity, String.valueOf(path), this).setCompressOption(new CompressOption());
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                compressBuilder.trimVideo();
//            }
//        }, 1000);


//        ArrayList<String> arrayList = new ArrayList<>();
//        arrayList.add(newFile.getPath());
//        ArrayList<Uri> arrayListUri = new ArrayList<>();
//        arrayListUri.add(Uri.fromFile(newFile));
//        // 获取视频路径
//        Intent result = new Intent();
//        result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, arrayList);
//        result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, arrayListUri);
//        result.putExtra(EXTRA_MULTIMEDIA_TYPES, MultimediaTypes.VIDEO);
//        result.putExtra(EXTRA_MULTIMEDIA_CHOICE, false);
//        mActivity.setResult(RESULT_OK, result);
//        mActivity.finish();
    }

    public void confirmPicture(ArrayList<BitmapData> bitmapDatas) {
        ArrayList<String> arrayList = new ArrayList<>();
        ArrayList<Uri> arrayListUri = new ArrayList<>();

        for (BitmapData data : bitmapDatas) {
            arrayList.add(data.getPath());
            arrayListUri.add(data.getUri());
        }

        Intent result = new Intent();
        result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, arrayList);
        result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, arrayListUri);
        result.putExtra(EXTRA_MULTIMEDIA_TYPES, MultimediaTypes.PICTURE);
        result.putExtra(EXTRA_MULTIMEDIA_CHOICE, false);
        mActivity.setResult(RESULT_OK, result);
        mIsCommit = true;
        mActivity.finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_PREVIEW_CAMRRA:
                // 如果在预览界面点击了确定
                if (data.getBooleanExtra(BasePreviewActivity.EXTRA_RESULT_APPLY, false)) {
                    // 请求的预览界面
                    Bundle resultBundle = data.getBundleExtra(BasePreviewActivity.EXTRA_RESULT_BUNDLE);
                    // 获取选择的数据
                    ArrayList<MultiMedia> selected = resultBundle.getParcelableArrayList(SelectedItemCollection.STATE_SELECTION);
                    if (selected == null) {
                        return;
                    }
                    // 循环判断，如果不存在，则删除
                    ListIterator<Map.Entry<Integer, BitmapData>> i = new ArrayList<>(mCameraLayout.mCaptureBitmaps.entrySet()).listIterator(mCameraLayout.mCaptureBitmaps.size());
                    while (i.hasPrevious()) {
                        Map.Entry<Integer, BitmapData> entry = i.previous();
                        int k = 0;
                        for (MultiMedia multiMedia : selected) {
                            // 根据索引判断是否相同
                            if (!entry.getKey().equals(multiMedia.getPosition())) {
                                k++;
                            }
                        }
                        if (k == selected.size()) {
                            // 所有都不符合，则删除
                            mCameraLayout.removePosition(entry.getKey());
                        }
                    }

                    // 刷新多个图片
                    mCameraLayout.refreshMultiPhoto(selected);
                }
                break;
            case REQUEST_CODE_PREVIEW_VIDEO:
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.add(data.getStringExtra("path"));
                ArrayList<Uri> arrayListUri = new ArrayList<>();
                arrayListUri.add(data.getParcelableExtra("uri"));
                // 获取视频路径
                Intent result = new Intent();
                result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, arrayList);
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, arrayListUri);
                result.putExtra(EXTRA_MULTIMEDIA_TYPES, MultimediaTypes.VIDEO);
                result.putExtra(EXTRA_MULTIMEDIA_CHOICE, false);
                mActivity.setResult(RESULT_OK, result);
                mIsCommit = true;
                mActivity.finish();
                break;
            case REQ_IMAGE_EDIT:
                mCameraLayout.refreshEditPhoto();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onBackPressed() {
        // 判断当前状态是否休闲
        if (mCameraLayout.mState == Constants.STATE_PREVIEW) {
            return false;
        } else {
            // 与上次点击返回键时刻作差
            if ((System.currentTimeMillis() - mExitTime) > MILLISECOND) {
                // 大于2000ms则认为是误操作，使用Toast进行提示
                Toast.makeText(mActivity.getApplicationContext(), getResources().getString(R.string.z_multi_library_press_confirm_again_to_close), Toast.LENGTH_SHORT).show();
                // 并记录下本次点击“返回键”的时刻，以便下次进行判断
                mExitTime = System.currentTimeMillis();
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCameraLayout != null) {
            mCameraLayout.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCameraLayout != null) {
            mCameraLayout.onPause();
        }
    }

    @Override
    public void onDestroy() {
        if (mCameraLayout != null) {
            mCameraLayout.onDestroy(mIsCommit);
        }
        super.onDestroy();
    }

    /**
     * 关闭事件
     */
    private void initCameraLayoutCloseListener() {
        mCameraLayout.setCloseListener(() -> mActivity.finish());
    }

    /**
     * 拍摄按钮事件
     */
    private void initCameraLayoutPhotoVideoListener() {
        mCameraLayout.setPhotoVideoListener(new ClickOrLongListener() {
            @Override
            public void actionDown() {
                // 母窗体禁止滑动
                ViewBusinessUtils.setTabLayoutScroll(false, ((MainActivity) mActivity), mCameraLayout.mViewHolder.pvLayout);
            }

            @Override
            public void onClick() {

            }

            @Override
            public void onLongClickShort(long time) {
                // 母窗体启动滑动
                ViewBusinessUtils.setTabLayoutScroll(true, ((MainActivity) mActivity), mCameraLayout.mViewHolder.pvLayout);
            }

            @Override
            public void onLongClick() {
            }

            @Override
            public void onLongClickEnd(long time) {

            }

            @Override
            public void onLongClickError() {

            }
        });
    }

    /**
     * 确认取消事件
     */
    private void initCameraLayoutOperateCameraListener() {
        mCameraLayout.setOperateCameraListener(new OperateCameraListener() {
            @Override
            public void cancel() {
                // 母窗体启动滑动
                ViewBusinessUtils.setTabLayoutScroll(true, ((MainActivity) mActivity), mCameraLayout.mViewHolder.pvLayout);
            }

            @Override
            public void captureSuccess(ArrayList<String> paths, ArrayList<Uri> uris) {
                Intent result = new Intent();
                result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, paths);
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, uris);
                result.putExtra(EXTRA_MULTIMEDIA_TYPES, MultimediaTypes.PICTURE);
                result.putExtra(EXTRA_MULTIMEDIA_CHOICE, false);
                mActivity.setResult(RESULT_OK, result);
                mIsCommit = true;
                mActivity.finish();

            }

            @Override
            public void recordSuccess(String path, Uri uri) {
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.add(path);
                ArrayList<Uri> arrayListUri = new ArrayList<>();
                arrayListUri.add(uri);
                // 获取视频路径
                Intent result = new Intent();
                result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, arrayList);
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, arrayListUri);
                result.putExtra(EXTRA_MULTIMEDIA_TYPES, MultimediaTypes.VIDEO);
                result.putExtra(EXTRA_MULTIMEDIA_CHOICE, false);
                mActivity.setResult(RESULT_OK, result);
                mIsCommit = true;
                mActivity.finish();
            }

        });
    }

    /**
     * 拍摄后操作图片的事件
     */
    private void initCameraLayoutCaptureListener() {
        mCameraLayout.setCaptureListener(new CaptureListener() {
            @Override
            public void remove(HashMap<Integer, BitmapData> captureBitmaps) {
                // 判断如果删除光图片的时候，母窗体启动滑动
                if (captureBitmaps.size() <= 0) {
                    ((MainActivity) mActivity).setTabLayoutScroll(true);
                }
            }

            @Override
            public void add(HashMap<Integer, BitmapData> captureBitmaps) {
                if (captureBitmaps.size() > 0) {
                    // 母窗体禁止滑动
                    ((MainActivity) mActivity).setTabLayoutScroll(false);
                }
            }
        });
    }

    /**
     * 编辑图片事件
     */
    private void initCameraLayoutEditListener() {
//        mCameraLayout.setEditListener((uri, newPath) -> {
//            Intent intent = new Intent();
//            intent.setClass(getContext(), ImageEditActivity.class);
//            intent.putExtra(ImageEditActivity.EXTRA_IMAGE_URI, uri);
//            intent.putExtra(ImageEditActivity.EXTRA_IMAGE_SAVE_PATH, newPath);
//            this.startActivityForResult(intent, REQ_IMAGE_EDIT);
//        });
    }

    @Override
    public void onSuccess(String outputPath) {
        Log.d("A.lee", "compress success" + outputPath);
//        dialog.dismiss();

        File newFile = new File(outputPath);
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(outputPath);
        ArrayList<Uri> arrayListUri = new ArrayList<>();
        arrayListUri.add(Uri.fromFile(newFile));
        // 获取视频路径
        Intent result = new Intent();
        result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, arrayList);
        result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, arrayListUri);
        result.putExtra(EXTRA_MULTIMEDIA_TYPES, MultimediaTypes.VIDEO);
        result.putExtra(EXTRA_MULTIMEDIA_CHOICE, false);
        mActivity.setResult(RESULT_OK, result);
        mIsCommit = true;
        mActivity.finish();


//        if (showFileLocationAlert)
//            showLocationAlert();
//        else {
//            Intent intent = new Intent();
//            intent.putExtra(TrimVideo.TRIMMED_VIDEO_PATH, outputPath);
//            setResult(RESULT_OK, intent);
//            finish();
//        }
    }

    @Override
    public void onFailed() {
//        if (dialog.isShowing())
//            dialog.dismiss();
    }

    @Override
    public void onProcessing() {
    }

    private void showProcessingDialog() {
        try {
            dialog = new Dialog(mActivity);
            dialog.setCancelable(false);
            dialog.setContentView(com.gowtham.library.R.layout.alert_convert);
            dialog.setCancelable(false);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
