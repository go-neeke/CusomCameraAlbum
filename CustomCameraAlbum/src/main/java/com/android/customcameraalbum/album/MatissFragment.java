package com.android.customcameraalbum.album;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.customcameraalbum.utils.StatusBarUtils;
import com.gowtham.library.utils.CompressOption;
import com.gowtham.library.utils.TrimVideo;
import com.android.customcameraalbum.MainActivity;
import com.android.customcameraalbum.R;
import com.android.customcameraalbum.album.entity.Album;
import com.android.customcameraalbum.album.model.AlbumCollection;
import com.android.customcameraalbum.album.model.SelectedItemCollection;
import com.android.customcameraalbum.album.ui.mediaselection.MediaSelectionFragment;
import com.android.customcameraalbum.album.ui.mediaselection.adapter.AlbumMediaAdapter;
import com.android.customcameraalbum.album.utils.PhotoMetadataUtils;
import com.android.customcameraalbum.album.widget.AlbumsSpinner;
import com.android.customcameraalbum.album.widget.CheckRadioView;
import com.android.customcameraalbum.camera.CameraLayout;
import com.android.customcameraalbum.camera.common.Constants;
import com.android.customcameraalbum.camera.entity.BitmapData;
import com.android.customcameraalbum.camera.util.FileUtil;
import com.android.customcameraalbum.preview.AlbumPreviewActivity;
import com.android.customcameraalbum.preview.BasePreviewActivity;
import com.android.customcameraalbum.preview.SelectedPreviewActivity;
import com.android.customcameraalbum.settings.AlbumSpec;
import com.android.customcameraalbum.settings.GlobalSpec;
import com.android.customcameraalbum.utils.PathUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.android.customcameraalbum.common.entity.MultiMedia;
import com.android.customcameraalbum.common.enums.MimeType;
import com.android.customcameraalbum.common.enums.MultimediaTypes;
import com.android.customcameraalbum.common.widget.IncapableDialog;

import static android.app.Activity.RESULT_OK;
import static com.android.customcameraalbum.album.model.SelectedItemCollection.COLLECTION_IMAGE;
import static com.android.customcameraalbum.album.model.SelectedItemCollection.COLLECTION_VIDEO;
import static com.android.customcameraalbum.album.model.SelectedItemCollection.STATE_COLLECTION_TYPE;
import static com.android.customcameraalbum.album.model.SelectedItemCollection.STATE_SELECTION;
import static com.android.customcameraalbum.camera.common.Constants.BUTTON_STATE_BOTH;
import static com.android.customcameraalbum.constants.Constant.EXTRA_MULTIMEDIA_CHOICE;
import static com.android.customcameraalbum.constants.Constant.EXTRA_MULTIMEDIA_TYPES;
import static com.android.customcameraalbum.constants.Constant.EXTRA_RESULT_SELECTION;
import static com.android.customcameraalbum.constants.Constant.EXTRA_RESULT_SELECTION_PATH;
import static com.android.customcameraalbum.constants.Constant.REQUEST_CODE_PREVIEW_CAMRRA;


/**
 * ??????
 *
 * @author zhongjh
 * @date 2018/8/22
 */
public class MatissFragment extends Fragment implements AlbumCollection.AlbumCallbacks,
        MediaSelectionFragment.SelectionProvider,
        AlbumMediaAdapter.CheckStateListener, AlbumMediaAdapter.OnMediaClickListener, TrimVideo.CompressBuilderListener {

    private static final String EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable";
    public static final String ARGUMENTS_MARGIN_BOTTOM = "arguments_margin_bottom";

    private static final String CHECK_STATE = "checkState";

    private AppCompatActivity mActivity;
    private Context mContext;
    /**
     * ?????????Fragment,???????????????????????????????????????????????????????????????
     */
    MediaSelectionFragment mFragmentLast;

    /**
     * ????????????
     */
    private GlobalSpec mGlobalSpec;

    /**
     * ?????????????????????
     */
    private final AlbumCollection mAlbumCollection = new AlbumCollection();
    private SelectedItemCollection mSelectedCollection;
    private AlbumSpec mAlbumSpec;

    /**
     * ?????????????????????
     */
    private AlbumsSpinner mAlbumsSpinner;
    /**
     * ??????????????????????????????
     */
    private AlbumsSpinnerAdapter mAlbumsSpinnerAdapter;

    /**
     * ????????????
     */
    private boolean mOriginalEnable;
    /**
     * ????????????
     */
    private boolean mIsRefresh;

    private ViewHolder mViewHolder;

    private Dialog dialog;

    public LinkedHashMap<Integer, BitmapData> mCaptureBitmaps = new LinkedHashMap<>();

    private final ActivityResultLauncher<Intent> startForResult =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK &&
                                result.getData() != null) {
                            File newFile = new File(TrimVideo.getTrimmedVideoPath(result.getData()));
                            Uri uri = Uri.fromFile(newFile);
                            Log.d("A.lee", "Trimmed path:: " + result.getData());
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
                            mActivity.finish();
                        } else {

                        }
//                            LogMessage.v("videoTrimResultLauncher data is null");
                    });


    /**
     * @param marginBottom ????????????
     */
    public static MatissFragment newInstance(int marginBottom) {
        MatissFragment matissFragment = new MatissFragment();
        Bundle args = new Bundle();
        matissFragment.setArguments(args);
        args.putInt(ARGUMENTS_MARGIN_BOTTOM, marginBottom);
        return matissFragment;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        this.mActivity = (AppCompatActivity) activity;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.mContext = context;
        mSelectedCollection = new SelectedItemCollection(getContext());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mAlbumSpec = AlbumSpec.getInstance();
        mGlobalSpec = GlobalSpec.getInstance();
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_matiss_zjh, container, false);

        mViewHolder = new ViewHolder(view);
        initView(savedInstanceState);
        initListener();
        return view;
    }

    /**
     * ?????????view
     */
    private void initView(Bundle savedInstanceState) {
        // ?????????????????????
        ViewGroup.LayoutParams layoutParams = mViewHolder.toolbar.getLayoutParams();
        int statusBarHeight = StatusBarUtils.getStatusBarHeight(mContext);
        layoutParams.height = layoutParams.height + statusBarHeight;
        mViewHolder.toolbar.setLayoutParams(layoutParams);
        mViewHolder.toolbar.setPadding(mViewHolder.toolbar.getPaddingLeft(), statusBarHeight,
                mViewHolder.toolbar.getPaddingRight(), mViewHolder.toolbar.getPaddingBottom());
        TypedArray ta = mContext.getTheme().obtainStyledAttributes(new int[]{R.attr.album_element_color});
        int color = ta.getColor(0, 0);
        ta.recycle();

        mSelectedCollection.onCreate(savedInstanceState, false);
        if (savedInstanceState != null) {
            mOriginalEnable = savedInstanceState.getBoolean(CHECK_STATE);
        }
        updateBottomToolbar();

        mAlbumsSpinnerAdapter = new AlbumsSpinnerAdapter(mContext, null, false);
        mAlbumsSpinner = new AlbumsSpinner(mContext);
        mAlbumsSpinner.setSelectedTextView(mViewHolder.selectedAlbum);
        mAlbumsSpinner.setPopupAnchorView(mViewHolder.bottomToolbar);
        mAlbumsSpinner.setAdapter(mAlbumsSpinnerAdapter);

        mViewHolder.container.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                // Ensure you call it only once :
                mViewHolder.container.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                mAlbumsSpinner.setHeight(mViewHolder.container.getMeasuredHeight());
                // Here you can get the size :)
            }
        });

        mAlbumCollection.onCreate(this, this);
        mAlbumCollection.onRestoreInstanceState(savedInstanceState);
        mAlbumCollection.loadAlbums();
    }

    private void initListener() {
        // ????????????
        mViewHolder.imgClose.setOnClickListener(v -> mActivity.finish());

        // ????????????????????????
        mAlbumsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                // ???????????????
                mAlbumCollection.setStateCurrentSelection(position);
                // ?????????????????????????????????
                mAlbumsSpinnerAdapter.getCursor().moveToPosition(position);
                // ????????????????????????
                Album album = Album.valueOf(mAlbumsSpinnerAdapter.getCursor());
                onAlbumSelected(album);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // ????????????
        mViewHolder.buttonPreview.setOnClickListener(view -> {
            if (mSelectedCollection.getCollectionType() == COLLECTION_VIDEO) {
                MultiMedia item = mSelectedCollection.asList().get(0);
                TrimVideo.activity(String.valueOf(item.getMediaUri()))
                        .setCompressOption(new CompressOption()) //empty constructor for default compress option
                        .setEnableEdit(!mSelectedCollection.typeConflict(item))
                        .start(this, startForResult);
            } else {
                Intent intent = new Intent(mActivity, SelectedPreviewActivity.class);
                intent.putExtra(BasePreviewActivity.IS_ALBUM_URI, true);
                intent.putExtra(BasePreviewActivity.EXTRA_DEFAULT_BUNDLE, mSelectedCollection.getDataWithBundle());
                intent.putExtra(BasePreviewActivity.EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
                startActivityForResult(intent, mGlobalSpec.requestCode);
                if (mGlobalSpec.isCutscenes) {
                    mActivity.overridePendingTransition(R.anim.activity_open, 0);
                }
            }
        });

        // ???????????????????????????
        mViewHolder.buttonApply.setOnClickListener(view -> {
//            // ????????????????????????url??????
            ArrayList<Uri> selectedUris = (ArrayList<Uri>) mSelectedCollection.asListOfUri();
            ArrayList<String> selectedPaths = (ArrayList<String>) mSelectedCollection.asListOfString();

            if (getMultimediaType(selectedUris) == MultimediaTypes.VIDEO) {
                TrimVideo.activity(String.valueOf(selectedUris.get(0)))
                        .setEnableEdit(false)
                        .setExecute(true)
                        .setCompressOption(new CompressOption()) //empty constructor for default compress option
//                .setCompressOption(new CompressOption(30,"1M",460,320))
                        .start(mActivity, startForResult);
            } else {
                Intent result = new Intent();
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris);
                result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths);
                result.putExtra(EXTRA_MULTIMEDIA_TYPES, getMultimediaType(selectedUris));
                result.putExtra(EXTRA_MULTIMEDIA_CHOICE, true);
                // ??????????????????
                result.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
                mActivity.setResult(RESULT_OK, result);
                mActivity.finish();
            }
        });

        // ????????????
        mViewHolder.originalLayout.setOnClickListener(view -> {
            if (getFragmentManager() != null) {
                // ??????????????????????????????????????????
                int count = countOverMaxSize();
                if (count > 0) {
                    IncapableDialog incapableDialog = IncapableDialog.newInstance("",
                            getString(R.string.z_multi_library_error_over_original_count, count, mAlbumSpec.originalMaxSize));
                    incapableDialog.show(getFragmentManager(),
                            IncapableDialog.class.getName());
                    return;
                }

                // ????????????
                mOriginalEnable = !mOriginalEnable;
                mViewHolder.original.setChecked(mOriginalEnable);

                // ????????????????????????
                if (mAlbumSpec.onCheckedListener != null) {
                    mAlbumSpec.onCheckedListener.onCheck(mOriginalEnable);
                }

            }
        });

    }

    /**
     * ??????uri?????????????????????????????????
     *
     * @param selectedUris uri??????
     * @return ???????????????????????????
     */
    private int getMultimediaType(ArrayList<Uri> selectedUris) {
        // ?????????????????????
        int isImageSize = 0;
        // ???????????????
        int isVideoSize = 0;
        ContentResolver resolver = mContext.getContentResolver();
        // ??????????????????
        for (Uri uri : selectedUris) {
            for (MimeType type : MimeType.ofImage()) {
                if (type.checkType(resolver, uri)) {
                    isImageSize++;
                    break;
                }
            }
            for (MimeType type : MimeType.ofVideo()) {
                if (type.checkType(resolver, uri)) {
                    isVideoSize++;
                    break;
                }
            }
        }
        // ?????????????????????????????????
        if (selectedUris.size() == isImageSize) {
            return MultimediaTypes.PICTURE;
        }
        if (selectedUris.size() == isVideoSize) {
            return MultimediaTypes.VIDEO;
        }
        return MultimediaTypes.BLEND;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mSelectedCollection.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // ????????????model
        mAlbumCollection.onDestroy();
        mAlbumSpec.onCheckedListener = null;
        mAlbumSpec.onSelectedListener = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //            onBackPressed(); // TODO
        return item.getItemId() == android.R.id.home || super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        // ?????????????????????
        if (requestCode == mGlobalSpec.requestCode) {
            Bundle resultBundle = data.getBundleExtra(BasePreviewActivity.EXTRA_RESULT_BUNDLE);
            // ?????????????????????
            ArrayList<MultiMedia> selected = resultBundle.getParcelableArrayList(SelectedItemCollection.STATE_SELECTION);
            // ??????????????????
            mOriginalEnable = data.getBooleanExtra(BasePreviewActivity.EXTRA_RESULT_ORIGINAL_ENABLE, false);
            int collectionType = resultBundle.getInt(SelectedItemCollection.STATE_COLLECTION_TYPE,
                    SelectedItemCollection.COLLECTION_UNDEFINED);
            // ????????????????????????????????????
            if (data.getBooleanExtra(BasePreviewActivity.EXTRA_RESULT_APPLY, false)) {
                Intent result = new Intent();
                ArrayList<Uri> selectedUris = new ArrayList<>();
                ArrayList<String> selectedPaths = new ArrayList<>();
                if (selected != null) {
                    for (MultiMedia item : selected) {
                        // ??????uri???path
                        selectedUris.add(item.getMediaUri());
                        selectedPaths.add(PathUtils.getPath(getContext(), item.getMediaUri()));
                    }
                }
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris);
                result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths);
                result.putExtra(EXTRA_MULTIMEDIA_TYPES, getMultimediaType(selectedUris));
                result.putExtra(EXTRA_MULTIMEDIA_CHOICE, true);
                // ??????????????????
                result.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
                mActivity.setResult(RESULT_OK, result);
                mActivity.finish();
            } else {
                // ???????????????
                mSelectedCollection.overwrite(selected, collectionType);
                if (getFragmentManager() != null) {
                    Fragment mediaSelectionFragment = getFragmentManager().findFragmentByTag(
                            MediaSelectionFragment.class.getSimpleName());
                    if (mediaSelectionFragment instanceof MediaSelectionFragment) {
                        if (data.getBooleanExtra(BasePreviewActivity.EXTRA_RESULT_IS_EDIT, false)) {
                            mIsRefresh = true;
                            albumsSpinnerNotifyData();
                            // ?????????????????????
                            ((MediaSelectionFragment) mediaSelectionFragment).restartLoaderMediaGrid();
                        } else {
                            // ???????????????
                            ((MediaSelectionFragment) mediaSelectionFragment).refreshMediaGrid();
                        }
                    }
                    // ????????????
                    updateBottomToolbar();
                }

            }
        }
    }

    /**
     * ??????????????????
     */
    private void updateBottomToolbar() {
        int selectedCount = mSelectedCollection.count();

        if (selectedCount == 0) {
            // ??????????????????????????????????????????
            mViewHolder.buttonPreview.setEnabled(false);
            mViewHolder.buttonPreview.setText(getString(R.string.z_multi_library_button_preview_default));
            mViewHolder.buttonApply.setEnabled(false);
            mViewHolder.buttonApply.setText(getString(R.string.z_multi_library_button_sure_default));
        } else if (selectedCount == 1 && mAlbumSpec.singleSelectionModeEnabled()) {
            // ????????????????????????
            mViewHolder.buttonPreview.setEnabled(true);
            mViewHolder.buttonPreview.setText(R.string.z_multi_library_button_preview_default);
            mViewHolder.buttonApply.setText(R.string.z_multi_library_button_sure_default);
            mViewHolder.buttonApply.setEnabled(true);
        } else {
            // ?????????????????????
            mViewHolder.buttonPreview.setEnabled(true);
            mViewHolder.buttonPreview.setText(getString(R.string.z_multi_library_button_preview, selectedCount));
            mViewHolder.buttonApply.setEnabled(true);
            mViewHolder.buttonApply.setText(getString(R.string.z_multi_library_button_sure, selectedCount));
        }

        // ????????????????????????
        if (mAlbumSpec.originalable) {
            mViewHolder.originalLayout.setVisibility(View.VISIBLE);
            updateOriginalState();
        } else {
            mViewHolder.originalLayout.setVisibility(View.INVISIBLE);
        }

        showBottomView(selectedCount);
    }

    /**
     * ????????????????????????
     */
    private void updateOriginalState() {
        // ??????????????????
        mViewHolder.original.setChecked(mOriginalEnable);
        if (countOverMaxSize() > 0) {
            // ??????????????????
            if (mOriginalEnable) {
                // ???????????????????????? xx mb
                IncapableDialog incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.z_multi_library_error_over_original_size, mAlbumSpec.originalMaxSize));
                if (this.getFragmentManager() == null) {
                    return;
                }
                incapableDialog.show(this.getFragmentManager(),
                        IncapableDialog.class.getName());

                // ????????????????????????
                mViewHolder.original.setChecked(false);
                mOriginalEnable = false;
            }
        }
    }

    /**
     * ??????????????????mb???????????????
     *
     * @return ??????
     */
    private int countOverMaxSize() {
        int count = 0;
        int selectedCount = mSelectedCollection.count();
        for (int i = 0; i < selectedCount; i++) {
            MultiMedia item = mSelectedCollection.asList().get(i);

            if (item.isImage()) {
                float size = PhotoMetadataUtils.getSizeInMb(item.size);

                if (size > mAlbumSpec.originalMaxSize) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public void onAlbumLoadFinished(final Cursor cursor) {
        // ??????????????????
        mAlbumsSpinnerAdapter.swapCursor(cursor);
        // ??????????????????
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            cursor.moveToPosition(mAlbumCollection.getCurrentSelection());
            mAlbumsSpinner.setSelection(getContext(),
                    mAlbumCollection.getCurrentSelection());
            Album album = Album.valueOf(cursor);
            onAlbumSelected(album);
        });
    }

    @Override
    public void onAlbumReset() {
        // ??????????????????
        mAlbumsSpinnerAdapter.swapCursor(null);
    }

    public void albumsSpinnerNotifyData() {
        mAlbumCollection.mLoadFinished = false;
        mAlbumCollection.restartLoadAlbums();
    }


    /**
     * ???????????????????????????
     *
     * @param album ??????
     */
    private void onAlbumSelected(Album album) {

        String displayName = album.getDisplayName(this.getContext());
        mViewHolder.selectedAlbum_title.setText(displayName);

        if (album.isAll() && album.isEmpty()) {
            // ????????????????????????????????????????????????????????????view
            mViewHolder.container.setVisibility(View.GONE);
            mViewHolder.emptyView.setVisibility(View.VISIBLE);
        } else {
            // ?????????????????????????????????fragment???????????????????????????
            mViewHolder.container.setVisibility(View.VISIBLE);
            mViewHolder.emptyView.setVisibility(View.GONE);
            if (!mIsRefresh) {
                assert getArguments() != null;
                if (mFragmentLast != null) {
                    // ???????????????????????????????????????????????????????????????
                    mFragmentLast.onDestroyData();
                }
                mFragmentLast = MediaSelectionFragment.newInstance(album, getArguments().getInt(ARGUMENTS_MARGIN_BOTTOM));
                mActivity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, mFragmentLast, MediaSelectionFragment.class.getSimpleName())
                        .commitAllowingStateLoss();
            }
        }
    }

    @Override
    public void onUpdate() {
        // notify bottom toolbar that check state changed.
        updateBottomToolbar();
        // ???????????????????????????
        if (mAlbumSpec.onSelectedListener != null) {
            mAlbumSpec.onSelectedListener.onSelected(
                    mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString());
        }
    }

    @Override
    public void onMediaClick(Album album, MultiMedia item, int adapterPosition) {
        if (item.isVideo()) {
            Log.d("A.lee", "mSelectedCollection.getCollectionType()" + mSelectedCollection.getCollectionType());
            TrimVideo.activity(String.valueOf(item.getMediaUri()))
                    .setCompressOption(new CompressOption()) //empty constructor for default compress option
                    .setEnableEdit(!mSelectedCollection.typeConflict(item))
                    .start(this, startForResult);

        } else {
            Intent intent = new Intent(mActivity, AlbumPreviewActivity.class);
            intent.putExtra(AlbumPreviewActivity.EXTRA_ALBUM, album);
            intent.putExtra(AlbumPreviewActivity.EXTRA_ITEM, item);
            intent.putExtra(BasePreviewActivity.EXTRA_DEFAULT_BUNDLE, mSelectedCollection.getDataWithBundle());
            intent.putExtra(BasePreviewActivity.EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable);
            intent.putExtra(BasePreviewActivity.IS_ALBUM_URI, true);
            startActivityForResult(intent, mGlobalSpec.requestCode);
            if (mGlobalSpec.isCutscenes) {
                mActivity.overridePendingTransition(R.anim.activity_open, 0);
            }
        }
    }

    @Override
    public SelectedItemCollection provideSelectedItemCollection() {
        return mSelectedCollection;
    }


    /**
     * ?????????????????????
     * ??????????????????table
     * ??????????????????????????????????????????????????????????????????
     *
     * @param count ?????????????????????
     */
    private void showBottomView(int count) {
//        if (count > 0) {
        // ????????????
        mViewHolder.bottomToolbar.setVisibility(View.VISIBLE);
        // ??????????????????table
        ((MainActivity) mActivity).showHideTableLayout(false);
//        } else {
//            // ????????????
//            mViewHolder.bottomToolbar.setVisibility(View.GONE);
//            // ??????????????????table
//            ((MainActivity) mActivity).showHideTableLayout(true);
//        }

        mViewHolder.llPhoto.removeAllViews();

        if (count > 0) {
            mViewHolder.hsvPhoto.setVisibility(View.VISIBLE);
            mViewHolder.llPhoto.removeAllViews();

            try {
                for (int position = 0; position < mSelectedCollection.count(); position++) {
                    Uri currentUri = mSelectedCollection.asListOfUri().get(position);
                    // ??????view
                    CameraLayout.ViewHolderImageView viewHolderImageView = new CameraLayout.ViewHolderImageView(View.inflate(getContext(), R.layout.item_horizontal_image_zjh, null));
                    mGlobalSpec.imageEngine.loadUriImage(getContext(), viewHolderImageView.imgPhoto, currentUri);
                    // ????????????
                    viewHolderImageView.imgCancel.setTag(position);
                    viewHolderImageView.imgCancel.setOnClickListener(v -> removeImage((Integer) viewHolderImageView.imgCancel.getTag(), viewHolderImageView.rootView));
                    mViewHolder.llPhoto.addView(viewHolderImageView.rootView);
                }
            } catch (Exception e) {
                Log.d("A.lee", "error" + e.toString());
            }
        } else {
            mViewHolder.hsvPhoto.setVisibility(View.GONE);
        }
    }

    private void removeImage(int position, View rootView) {
        mViewHolder.llPhoto.removeView(rootView);
        mSelectedCollection.remove(mSelectedCollection.asList().get(position));
        mFragmentLast.refreshSelection();
        mFragmentLast.refreshMediaGrid();
        updateBottomToolbar();
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

    @Override
    public void onProcessing() {

    }

    @Override
    public void onSuccess(String outputPath) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("A.lee", "compress success" + outputPath);
                dialog.dismiss();

                File newFile = new File(outputPath);
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.add(outputPath);
                ArrayList<Uri> arrayListUri = new ArrayList<>();
                arrayListUri.add(Uri.fromFile(newFile));
                // ??????????????????
                Intent result = new Intent();
                result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, arrayList);
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, arrayListUri);
                result.putExtra(EXTRA_MULTIMEDIA_TYPES, MultimediaTypes.VIDEO);
                result.putExtra(EXTRA_MULTIMEDIA_CHOICE, false);
                mActivity.setResult(RESULT_OK, result);
                mActivity.finish();
            }
        }, 1000);
    }

    @Override
    public void onFailed() {
        if (dialog.isShowing())
            dialog.dismiss();
    }

    public static class ViewHolder {
        public View rootView;
        public TextView selectedAlbum;
        public TextView selectedAlbum_title;
        public Toolbar toolbar;
        public TextView buttonPreview;
        public CheckRadioView original;
        public LinearLayout originalLayout;
        public TextView buttonApply;
        public FrameLayout bottomToolbar;
        public FrameLayout container;
        public TextView emptyViewContent;
        public FrameLayout emptyView;
        public RelativeLayout root;
        public ImageView imgClose;
        public HorizontalScrollView hsvPhoto;
        LinearLayout llPhoto;

        public ViewHolder(View rootView) {
            this.rootView = rootView;
            this.selectedAlbum = rootView.findViewById(R.id.selectedAlbum);
            this.selectedAlbum_title = rootView.findViewById(R.id.selectedAlbum_title);
            this.toolbar = rootView.findViewById(R.id.toolbar);
            this.buttonPreview = rootView.findViewById(R.id.buttonPreview);
            this.original = rootView.findViewById(R.id.original);
            this.originalLayout = rootView.findViewById(R.id.originalLayout);
            this.buttonApply = rootView.findViewById(R.id.buttonApply);
            this.bottomToolbar = rootView.findViewById(R.id.bottomToolbar);
            this.container = rootView.findViewById(R.id.container);
            this.emptyViewContent = rootView.findViewById(R.id.emptyViewContent);
            this.emptyView = rootView.findViewById(R.id.emptyView);
            this.root = rootView.findViewById(R.id.root);
            this.imgClose = rootView.findViewById(R.id.imgClose);
            this.hsvPhoto = rootView.findViewById(R.id.hsvPhoto);
            this.llPhoto = rootView.findViewById(R.id.llPhoto);
        }

    }
}
