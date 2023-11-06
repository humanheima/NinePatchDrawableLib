package com.dmw.lib.ninepatch;

import android.graphics.drawable.AnimationDrawable;

/**
 * Created by dmw on 2023/10/30
 * Desc: 可以指定循环次数的帧动画
 */

/**
 * Created by dmw on 2023/10/25
 * Desc: 1. 监听帧动画的结束，需要的话
 * 2. 可以指定循环次数
 */
public class CanStopAnimationDrawable extends AnimationDrawable {

    private static final String TAG = "CanStopAnimationDrawabl";

    /**
     * 动画结束的次数，因为帧动画可能会播放一次，或者一直播放
     */
    private int finishCount = 1;

    private int innerCount = 0;

    private IAnimationFinishListener animationFinishListener;

    public IAnimationFinishListener getAnimationFinishListener() {
        return animationFinishListener;
    }

    public void setFinishCount(int finishCount) {
        this.finishCount = finishCount;
    }

    public void setAnimationFinishListener(IAnimationFinishListener animationFinishListener) {
        this.animationFinishListener = animationFinishListener;
    }

    @Override
    public void start() {
        super.start();
        if (animationFinishListener != null) {
            animationFinishListener.onAnimationStart();
        }
    }

    @Override
    public boolean selectDrawable(int index) {
        boolean ret = super.selectDrawable(index);
        if ((index != 0) && (index == getNumberOfFrames() - 1)) {
            innerCount++;
            if (innerCount >= finishCount) {
                setOneShot(true);
            }
            if (animationFinishListener != null) {
                animationFinishListener.onAnimationFinished(innerCount);
            }
        }
        return ret;
    }

    public interface IAnimationFinishListener {

        void onAnimationStart();

        /**
         * 动画结束
         *
         * @param endCount 第一次结束
         */
        void onAnimationFinished(int endCount);
    }

}