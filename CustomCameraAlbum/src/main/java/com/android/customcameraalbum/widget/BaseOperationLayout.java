package com.android.customcameraalbum.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.customcameraalbum.R;
import com.android.customcameraalbum.camera.common.Constants;
import com.android.customcameraalbum.camera.listener.ClickOrLongListener;
import com.android.customcameraalbum.camera.util.DisplayMetricsSpUtils;
import com.android.customcameraalbum.widget.clickorlongbutton.ClickOrLongButton;
import com.zhongjh.circularprogressview.CircularProgress;
import com.zhongjh.circularprogressview.CircularProgressListener;

import java.util.ArrayList;

/**
 * 集成各个控件的布局
 * {@link com.android.customcameraalbum.widget.clickorlongbutton.ClickOrLongButton 点击或者长按的按钮 }
 * {@link com.zhongjh.circularprogressview.CircularProgress 操作按钮(取消和确认) }
 *
 * @author zhongjh
 * @date 2018/8/7
 */
public abstract class BaseOperationLayout extends FrameLayout {

    // region 回调事件监听

    /**
     * 点击或长按监听
     */
    private ClickOrLongListener mClickOrLongListener;
    /**
     * 点击或长按监听结束后的 确认取消事件监控
     */
    private OperateListener mOperateListener;

    /**
     * 操作按钮的Listener
     */
    public interface OperateListener {
        void cancel();

        void confirm();

        void preview();
    }

    public void setPhotoVideoListener(ClickOrLongListener clickOrLongListener) {
        this.mClickOrLongListener = clickOrLongListener;
    }

    public void setOperateListener(OperateListener mOperateListener) {
        this.mOperateListener = mOperateListener;
    }

    // endregion

    public ViewHolder getViewHolder() {
        return viewHolder;
    }

    /**
     * 控件集合
     */
    public ViewHolder viewHolder;

    /**
     * 该布局宽度
     */
    private final int mLayoutWidth;
    /**
     * 该布局高度
     */
    private final int mLayoutHeight;

    /**
     * 是否第一次
     */
    private boolean mIsFirst = true;

    /**
     * 按钮左右分开移动动画
     */
    ObjectAnimator mAnimatorConfirm;
    ObjectAnimator mAnimatorPreview;

    /**
     * 创建
     *
     * @return ViewHolder
     */
    protected abstract ViewHolder newViewHolder();

    public BaseOperationLayout(@NonNull Context context) {
        this(context, null);
    }

    public BaseOperationLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseOperationLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mLayoutWidth = DisplayMetricsSpUtils.getScreenWidth(context);
        // 中心的按钮大小
        int mButtonSize = (int) (mLayoutWidth / 4.5f);
        mLayoutHeight = mButtonSize + (mButtonSize / 5) * 2 + 100;
        initView();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(mLayoutWidth, mLayoutHeight);
    }

    /**
     * 初始化view
     */
    private void initView() {
        // 自定义View中如果重写了onDraw()即自定义了绘制，那么就应该在构造函数中调用view的setWillNotDraw(false).
        setWillNotDraw(false);

        viewHolder = newViewHolder();

        mAnimatorConfirm = ObjectAnimator.ofFloat(viewHolder.btnConfirm, "translationX", -mLayoutWidth / 4F, 0);
        mAnimatorPreview = ObjectAnimator.ofFloat(viewHolder.btnPreview, "translationX", mLayoutWidth / 4F, 0);

        // 默认隐藏
        viewHolder.btnConfirm.setVisibility(GONE);
        viewHolder.btnPreview.setVisibility(GONE);
        viewHolder.btnCancel.setVisibility(VISIBLE);

        initListener();
    }

    /**
     * 初始化事件
     */
    protected void initListener() {
        btnClickOrLongListener();
        btnCancelListener();
        btnConfirmListener();
        btnPreviewListener();
    }

    /**
     * btnClickOrLong事件
     */
    private void btnClickOrLongListener() {
        viewHolder.btnClickOrLong.setRecordingListener(new ClickOrLongListener() {
            @Override
            public void actionDown() {
                if (mClickOrLongListener != null) {
                    mClickOrLongListener.actionDown();
                }
            }

            @Override
            public void onClick() {
                if (mClickOrLongListener != null) {
                    mClickOrLongListener.onClick();
                }
            }

            @Override
            public void onLongClickShort(long time) {
                if (mClickOrLongListener != null) {
                    mClickOrLongListener.onLongClickShort(time);
                }
                startTipAlphaAnimation();
            }

            @Override
            public void onLongClick() {
                if (mClickOrLongListener != null) {
                    mClickOrLongListener.onLongClick();
                }
                startTipAlphaAnimation();
            }

            @Override
            public void onLongClickEnd(long time) {
                if (mClickOrLongListener != null) {
                    mClickOrLongListener.onLongClickEnd(time);
                }
                startTipAlphaAnimation();
            }

            @Override
            public void onLongClickError() {
                if (mClickOrLongListener != null) {
                    mClickOrLongListener.onLongClickError();
                }
            }
        });
    }

    /**
     * 返回事件
     */
    private void btnCancelListener() {
        viewHolder.btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOperateListener != null) {
                    mOperateListener.cancel();
                }
                startTipAlphaAnimation();
            }
        });
    }

    /**
     * 提交事件
     */
    private void btnConfirmListener() {
        viewHolder.btnConfirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOperateListener != null) {
                    mOperateListener.confirm();
                }
                startTipAlphaAnimation();
            }
        });
    }

    /**
     * 提交事件
     */
    private void btnPreviewListener() {
        viewHolder.btnPreview.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOperateListener != null) {
                    mOperateListener.preview();
                }
                startTipAlphaAnimation();
            }
        });
    }

    /**
     * 隐藏中间的核心操作按钮
     */
    public void hideBtnClickOrLong() {
        viewHolder.btnClickOrLong.setVisibility(INVISIBLE);
    }

    /**
     * 点击长按结果后的动画
     * 显示左右两边的按钮
     */
    public void startShowLeftRightButtonsAnimator() {
        // 显示提交和取消按钮
        viewHolder.btnCancel.setVisibility(GONE);
        viewHolder.btnConfirm.setVisibility(VISIBLE);
        viewHolder.btnPreview.setVisibility(VISIBLE);
        // 动画未结束前不能让它们点击
        viewHolder.btnConfirm.setClickable(false);
        viewHolder.btnPreview.setClickable(false);
        // 显示动画
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(mAnimatorPreview, mAnimatorConfirm);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                // 动画结束使得按钮可点击
                viewHolder.btnConfirm.setClickable(true);
                viewHolder.btnPreview.setClickable(true);
            }
        });
        animatorSet.setDuration(300);
        animatorSet.start();
    }

    /**
     * 多图片拍照后显示的右侧按钮
     */
    public void startOperaeBtnAnimatorMulti() {
        // 如果本身隐藏的，就显示出来
        if (viewHolder.btnConfirm.getVisibility() == View.GONE) {
            // 显示提交按钮
            viewHolder.btnConfirm.setVisibility(VISIBLE);
            // 动画未结束前不能让它们点击
            viewHolder.btnConfirm.setClickable(false);

            // 显示动画
            ObjectAnimator animatorConfirm = ObjectAnimator.ofFloat(viewHolder.btnConfirm, "translationX", -mLayoutWidth / 4F, 0);
            AnimatorSet set = new AnimatorSet();
            set.playTogether(animatorConfirm);
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    // 动画结束使得按钮可点击
                    viewHolder.btnConfirm.setClickable(true);
                }
            });
            set.setDuration(200);
            set.start();
        }
    }

    // region 对外提供的api

    /**
     * 设置提示文本
     *
     * @param tip 提示文本
     */
    public void setTip(String tip) {
        viewHolder.tvTip.setText(tip);
    }

    /**
     * 提示文本框 - 浮现渐现动画
     */
    public void startTipAlphaAnimation() {
        if (mIsFirst) {
            ObjectAnimator animatorTxtTip = ObjectAnimator.ofFloat(viewHolder.tvTip, "alpha", 1f, 0f);
            animatorTxtTip.setDuration(500);
            animatorTxtTip.start();
            mIsFirst = false;
        }
    }

    /**
     * 提示文本框 - 浮现渐现动画，显示新的文字
     *
     * @param tip 提示文字
     */
    public void setTipAlphaAnimation(String tip) {
        viewHolder.tvTip.setText(tip);
        ObjectAnimator animatorTxtTip = ObjectAnimator.ofFloat(viewHolder.tvTip, "alpha", 0f, 1f, 1f, 0f);
        animatorTxtTip.setDuration(2500);
        animatorTxtTip.start();
    }

    /**
     * 设置按钮 最长长按时间
     *
     * @param duration 时间秒
     */
    public void setDuration(int duration) {
        viewHolder.btnClickOrLong.setDuration(duration);
    }

    /**
     * 最短录制时间
     *
     * @param duration 时间
     */
    public void setMinDuration(int duration) {
        viewHolder.btnClickOrLong.setMinDuration(duration);
    }

    /**
     * 重置本身
     */
    public void reset() {
        viewHolder.btnClickOrLong.resetState();
        // 隐藏第二层的view
        viewHolder.btnCancel.setVisibility(VISIBLE);
        viewHolder.btnConfirm.setVisibility(View.GONE);
        viewHolder.btnPreview.setVisibility(View.GONE);
        // 显示第一层的view
        viewHolder.btnClickOrLong.setVisibility(View.VISIBLE);
    }

    /**
     * 设置按钮支持的功能：
     *
     * @param buttonStateBoth {@link Constants#BUTTON_STATE_ONLY_CLICK 只能点击
     * @link Constants#BUTTON_STATE_ONLY_LONG_CLICK 只能长按
     * @link Constants#BUTTON_STATE_BOTH 两者皆可
     * }
     */
    public void setButtonFeatures(int buttonStateBoth) {
        viewHolder.btnClickOrLong.setButtonFeatures(buttonStateBoth);
    }

    public int getButtonFeatures() {
       return viewHolder.btnClickOrLong.getButtonFeatures();
    }

    /**
     * 设置是否可点击
     */
    @Override
    public void setEnabled(boolean enabled) {
        viewHolder.btnClickOrLong.setEnabled(enabled);
    }

    /**
     * 赋值时间长度，目前用于分段录制
     */
    public void setData(ArrayList<Long> videoTimes) {
        viewHolder.btnClickOrLong.setCurrentTime(videoTimes);
    }

    /**
     * 刷新点击长按按钮
     */
    public void invalidateClickOrLongButton() {
        viewHolder.btnClickOrLong.invalidate();
    }


    public static class ViewHolder {
        View rootView;
        ImageButton btnCancel;
        public Button btnConfirm;
        public Button btnPreview;
        public ClickOrLongButton btnClickOrLong;
        TextView tvTip;
        public TextView tvSectionRecord;

        public ViewHolder(View rootView) {
            this.rootView = rootView;
            this.btnCancel = rootView.findViewById(R.id.btnCancel);
            this.btnConfirm = rootView.findViewById(R.id.btnConfirm);
            this.btnPreview = rootView.findViewById(R.id.btnPreview);
            this.btnClickOrLong = rootView.findViewById(R.id.btnClickOrLong);
            this.tvTip = rootView.findViewById(R.id.tvTip);
            this.tvSectionRecord = rootView.findViewById(R.id.tvSectionRecord);
        }

    }

    // endregion


}
