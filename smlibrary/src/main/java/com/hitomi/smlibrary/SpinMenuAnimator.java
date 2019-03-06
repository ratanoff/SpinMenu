package com.hitomi.smlibrary;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

/**
 * Menu on/off animation <br/>
 *
 * Created by hitomi on 2016/9/19. <br/>
 *
 * github : https://github.com/Hitomis <br/>
 *
 * email : 196425254@qq.com
 */
public class SpinMenuAnimator {

    private final Interpolator interpolator = new OvershootInterpolator();

    private SpinMenuLayout spinMenuLayout;

    private SpinMenu spinMenu;

    private OnSpinMenuStateChangeListener onSpinMenuStateChangeListener;

    private float diffTranY;

    public SpinMenuAnimator(SpinMenu spinMenu, SpinMenuLayout spinMenuLayout, OnSpinMenuStateChangeListener listener) {
        this.spinMenu = spinMenu;
        this.spinMenuLayout = spinMenuLayout;
        this.onSpinMenuStateChangeListener = listener;
    }

    public void openMenuAnimator() {
        // Update the menu status to MENU_STATE_OPEN and display SpinMenuLayout before opening the menu
        spinMenu.updateMenuState(SpinMenu.MENU_STATE_OPEN);
        spinMenuLayout.setVisibility(View.VISIBLE);

        ViewGroup selectItemLayout = (ViewGroup) spinMenuLayout.getChildAt(spinMenuLayout.getSelectedPosition());
        final ViewGroup showingPager = (ViewGroup) spinMenu.getChildAt(spinMenu.getChildCount() - 1);
        final ViewGroup selectContainer = (ViewGroup) selectItemLayout.findViewWithTag(SpinMenu.TAG_ITEM_CONTAINER);
        final float scaleRatio = spinMenu.getScaleRatio();
        diffTranY = (showingPager.getHeight() * (1.f -  scaleRatio)) * .5f - selectItemLayout.getTop();

        // Get the menu item to the left of the middle position of the current menu, and set the right move picture
        ObjectAnimator leftTranXAnima = null, rightTranXAnima = null;
        if (spinMenuLayout.getSelectedPosition() - 1 > -1) {
            ViewGroup leftItemLayout = (ViewGroup) spinMenuLayout.getChildAt(spinMenuLayout.getSelectedPosition() - 1);
            leftTranXAnima = ObjectAnimator.ofFloat(leftItemLayout, "translationX", leftItemLayout.getTranslationX(), 0);
        } else if (spinMenuLayout.isCyclic() && spinMenuLayout.getSelectedPosition() == 0) {
            ViewGroup leftItemLayout = (ViewGroup) spinMenuLayout.getChildAt(spinMenuLayout.getMenuItemCount() - 1);
            leftTranXAnima = ObjectAnimator.ofFloat(leftItemLayout, "translationX", leftItemLayout.getTranslationX(), 0);
        }

        // Get the menu item to the right of the middle position of the current menu, and set the left moving picture
        if (spinMenuLayout.getSelectedPosition() + 1 < spinMenuLayout.getChildCount()) {
            ViewGroup rightItemLayout = (ViewGroup) spinMenuLayout.getChildAt(spinMenuLayout.getSelectedPosition() + 1);
            rightTranXAnima = ObjectAnimator.ofFloat(rightItemLayout, "translationX", rightItemLayout.getTranslationX(), 0);
        } else if (spinMenuLayout.isCyclic() && spinMenuLayout.getSelectedPosition() + 1 == spinMenuLayout.getMenuItemCount()) {
            ViewGroup rightItemLayout = (ViewGroup) spinMenuLayout.getChildAt(0);
            rightTranXAnima = ObjectAnimator.ofFloat(rightItemLayout, "translationX", rightItemLayout.getTranslationX(), 0);
        }

        // Set the current page's zoom and move the picture
        ObjectAnimator scaleXAnima = ObjectAnimator.ofFloat(
                showingPager, "scaleX", showingPager.getScaleX(), scaleRatio);
        ObjectAnimator scaleYAnima = ObjectAnimator.ofFloat(
                showingPager, "scaleY", showingPager.getScaleY(), scaleRatio);
        ObjectAnimator tranYAnima = ObjectAnimator.ofFloat(
                showingPager, "translationY", showingPager.getTranslationY(), -diffTranY
        );

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(interpolator);
        AnimatorSet.Builder animaBuilder = animatorSet.play(scaleXAnima)
                .with(scaleYAnima)
                .with(tranYAnima);
        if (leftTranXAnima != null) {
            animaBuilder.with(leftTranXAnima);
        }
        if (rightTranXAnima != null) {
            animaBuilder.with(rightTranXAnima);
        }
        animatorSet.start();

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Remove showingPager from SpinMenu
                spinMenu.removeView(showingPager);

                // Remove the FrameLayout used to occupy the placeholder from the selectContainer
                selectContainer.removeAllViews();

                // Add showingPager to selectContainer
                FrameLayout.LayoutParams pagerParams = new FrameLayout.LayoutParams(
                        showingPager.getWidth(),
                        showingPager.getHeight()
                );
                selectContainer.addView(showingPager, pagerParams);

                // Correct the position of showingPager in selectContainer
                float tranX = (showingPager.getWidth() * (1.f -  scaleRatio)) * .5f;
                float tranY = (showingPager.getHeight() * (1.f -  scaleRatio)) * .5f;
                showingPager.setTranslationX(-tranX);
                showingPager.setTranslationY(-tranY);

                if (onSpinMenuStateChangeListener != null) {
                    onSpinMenuStateChangeListener.onMenuOpened();
                }

                // After the menu is opened, the slide control spinMenuLayout is allowed to rotate. And update the menu status to MENU_STATE_OPENED
                spinMenuLayout.postEnable(true);
                spinMenu.updateMenuState(SpinMenu.MENU_STATE_OPENED);
            }
        });
    }

    public void closeMenuAnimator(SMItemLayout chooseItemLayout) {
        // The menu state is updated to MENU_STATE_CLOSE before the menu is closed, and the slide control spinMenuLayout is not allowed to rotate.
        spinMenu.updateMenuState(SpinMenu.MENU_STATE_CLOSE);
        spinMenuLayout.postEnable(false);

         // Remove the FrameLayout containing the display Fragment from the chooseItemLayout
        FrameLayout frameContainer = (FrameLayout) chooseItemLayout.findViewWithTag(SpinMenu.TAG_ITEM_CONTAINER);
        FrameLayout pagerLayout = (FrameLayout) frameContainer.findViewWithTag(SpinMenu.TAG_ITEM_PAGER);
        frameContainer.removeView(pagerLayout);

        // Create a FrameLayout for placeholders
        FrameLayout.LayoutParams pagerFrameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        FrameLayout holderLayout = new FrameLayout(chooseItemLayout.getContext());
        holderLayout.setLayoutParams(pagerFrameParams);

        // Add a placeholder FrameLayout to the frameContainer in the chooseItemLayout layout
        frameContainer.addView(holderLayout);

        // Add pagerLayout to the SpinMenu
        pagerLayout.setLayoutParams(pagerFrameParams);
        spinMenu.addView(pagerLayout);

        // Place pagerLayout to the same location
        int currTranX = (int) (spinMenu.getWidth() * (1.f - spinMenu.getScaleRatio()) * .5f);
        int currTranY = (int) (spinMenu.getHeight() * (1.f - spinMenu.getScaleRatio()) * .5f - diffTranY);
        pagerLayout.setTranslationX(currTranX);
        pagerLayout.setTranslationY(currTranY);
        pagerLayout.setScaleX(spinMenu.getScaleRatio());
        pagerLayout.setScaleY(spinMenu.getScaleRatio());

        // Get the menu item to the left of the middle position of the current menu, and set the left moving picture
        ObjectAnimator leftTranXAnima = null, rightTranXAnima = null;
        if (spinMenuLayout.getSelectedPosition() - 1 > -1) {
            ViewGroup leftItemLayout = (ViewGroup) spinMenuLayout.getChildAt(spinMenuLayout.getSelectedPosition() - 1);
            leftTranXAnima = ObjectAnimator.ofFloat(leftItemLayout, "translationX",
                    leftItemLayout.getTranslationX(), -SpinMenu.TRAN_SKNEW_VALUE);
        } else if (spinMenuLayout.isCyclic() && spinMenuLayout.getSelectedPosition() == 0) {
            ViewGroup leftItemLayout = (ViewGroup) spinMenuLayout.getChildAt(spinMenuLayout.getMenuItemCount() - 1);
            leftTranXAnima = ObjectAnimator.ofFloat(leftItemLayout, "translationX",
                    leftItemLayout.getTranslationX(), -SpinMenu.TRAN_SKNEW_VALUE);
        }

        // Get the menu item to the right of the middle of the current menu and set the right move picture
        if (spinMenuLayout.getSelectedPosition() + 1 < spinMenuLayout.getChildCount()) {
            ViewGroup rightItemLayout = (ViewGroup) spinMenuLayout.getChildAt(spinMenuLayout.getSelectedPosition() + 1);
            rightTranXAnima = ObjectAnimator.ofFloat(rightItemLayout, "translationX",
                    rightItemLayout.getTranslationX(), SpinMenu.TRAN_SKNEW_VALUE);
        } else if (spinMenuLayout.isCyclic() && spinMenuLayout.getSelectedPosition() + 1 == spinMenuLayout.getMenuItemCount()) {
            ViewGroup rightItemLayout = (ViewGroup) spinMenuLayout.getChildAt(0);
            rightTranXAnima = ObjectAnimator.ofFloat(rightItemLayout, "translationX",
                    rightItemLayout.getTranslationX(), SpinMenu.TRAN_SKNEW_VALUE);
        }

        // Set the zoom of the currently selected menu, move the picture left and right and down
        ObjectAnimator scaleXAnima =  ObjectAnimator.ofFloat(pagerLayout, "scaleX", pagerLayout.getScaleX(), 1.f);
        ObjectAnimator scaleYAnima =  ObjectAnimator.ofFloat(pagerLayout, "scaleY", pagerLayout.getScaleX(), 1.f);
        ObjectAnimator tranXAnima = ObjectAnimator.ofFloat(pagerLayout, "translationX", 0, 0);
        ObjectAnimator tranYAnima = ObjectAnimator.ofFloat(pagerLayout, "translationY", -diffTranY, 0);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(interpolator);
        AnimatorSet.Builder animaBuilder = animatorSet.play(scaleXAnima)
                .with(scaleYAnima)
                .with(tranXAnima)
                .with(tranYAnima);
        if (leftTranXAnima != null) {
            animaBuilder.with(leftTranXAnima);
        }
        if (rightTranXAnima != null) {
            animaBuilder.with(rightTranXAnima);
        }
        animatorSet.start();

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onSpinMenuStateChangeListener != null) {
                    onSpinMenuStateChangeListener.onMenuClosed();
                }

                // After the menu is closed, set the spinMenuLayout to hide and the menu status is updated to MENU_STATE_CLOSED
                spinMenuLayout.setVisibility(View.GONE);
                spinMenu.updateMenuState(SpinMenu.MENU_STATE_CLOSED);
            }
        });
    }
}
