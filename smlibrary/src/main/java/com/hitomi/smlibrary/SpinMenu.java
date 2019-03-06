package com.hitomi.smlibrary;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * Created by hitomi on 2016/9/18. <br/>
 *
 * github : https://github.com/Hitomis <br/>
 *
 * email : 196425254@qq.com
 */
public class SpinMenu extends FrameLayout {

    static final String TAG = "SpinMenu";

    static final String TAG_ITEM_CONTAINER = "tag_item_container";

    static final String TAG_ITEM_PAGER = "tag_item_pager";

    static final String TAG_ITEM_HINT = "tag_item_hint";

    static final int MENU_STATE_CLOSE = -2;

    static final int MENU_STATE_CLOSED = -1;

    static final int MENU_STATE_OPEN = 1;

    static final int MENU_STATE_OPENED = 2;

    /**
     * Left and right menu Item Move the distance of the animation
     */
    static final float TRAN_SKNEW_VALUE = 160;

    /**
     * Hint relative to the top margin of the page
     */
    static final int HINT_TOP_MARGIN = 15;

    /**
     * Rotatable, rotating layout
     */
    private SpinMenuLayout spinMenuLayout;

    /**
     * Menu open close animation helper class
     */
    private SpinMenuAnimator spinMenuAnimator;

    /**
     * Page adapter
     */
    private PagerAdapter pagerAdapter;

    /**
     * Gesture recognizer
     */
    private GestureDetectorCompat menuDetector;

    /**
     * Menu state change listener
     */
    private OnSpinMenuStateChangeListener onSpinMenuStateChangeListener;

    /**
     *Cache a collection of Fragments for use by {@link #pagerAdapter}
     */
    private List pagerObjects;

    /**
     * Menu item collection
     */
    private List<SMItemLayout> smItemLayoutList;

    /**
     * Page title character set
     */
    private List<String> hintStrList;

    /**
     * Page title character size
     */
    private int hintTextSize = 14;

    /**
     * Page title character color
     */
    private int hintTextColor = Color.parseColor("#666666");

    /**
     * The ratio of page reduction when the menu is opened by default
     */
    private float scaleRatio = .36f;

    /**
     * Whether the control initializes the tag variable
     */
    private boolean init = true;

    /**
     * Whether to enable gesture recognition
     */
    private boolean enableGesture;

    /**
     * Current menu state, default is off
     */
    private int menuState = MENU_STATE_CLOSED;

    /**
     * Threshold between sliding and touch
     */
    private int touchSlop = 8;

    private OnSpinSelectedListener onSpinSelectedListener = new OnSpinSelectedListener() {
        @Override
        public void onSpinSelected(int position) {
            log("SpinMenu position:" + position);
        }
    };

    private com.hitomi.smlibrary.onMenuSelectedListener onMenuSelectedListener = new onMenuSelectedListener() {
        @Override
        public void onMenuSelected(SMItemLayout smItemLayout) {
            closeMenu(smItemLayout);
        }
    };

    private GestureDetector.SimpleOnGestureListener menuGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (Math.abs(distanceX) < touchSlop && distanceY < -touchSlop * 3) {
                openMenu();
            }
            return true;
        }
    };

    public SpinMenu(Context context) {
        this(context, null);
    }

    public SpinMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpinMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SpinMenu);
        scaleRatio = typedArray.getFloat(R.styleable.SpinMenu_scale_ratio, scaleRatio);
        hintTextSize = typedArray.getDimensionPixelSize(R.styleable.SpinMenu_hint_text_size, hintTextSize);
        hintTextSize = px2Sp(hintTextColor);
        hintTextColor = typedArray.getColor(R.styleable.SpinMenu_hint_text_color, hintTextColor);
        typedArray.recycle();

        pagerObjects = new ArrayList();
        smItemLayoutList = new ArrayList<>();
        menuDetector = new GestureDetectorCompat(context, menuGestureListener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
            ViewConfiguration conf = ViewConfiguration.get(getContext());
            touchSlop = conf.getScaledTouchSlop();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        @IdRes final int smLayoutId = 0x6F060505;
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        spinMenuLayout = new SpinMenuLayout(getContext());
        spinMenuLayout.setId(smLayoutId);
        spinMenuLayout.setLayoutParams(layoutParams);
        spinMenuLayout.setOnSpinSelectedListener(onSpinSelectedListener);
        spinMenuLayout.setOnMenuSelectedListener(onMenuSelectedListener);
        addView(spinMenuLayout);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (init && smItemLayoutList.size() > 0) {
            // Adjust the overall size of the item view in the menu according to scaleRatio
            int pagerWidth = (int) (getMeasuredWidth() * scaleRatio);
            int pagerHeight = (int) (getMeasuredHeight() * scaleRatio);
            SMItemLayout.LayoutParams containerLayoutParams = new SMItemLayout.LayoutParams(pagerWidth, pagerHeight);
            SMItemLayout smItemLayout;
            FrameLayout frameContainer;
            TextView tvHint;
            for (int i = 0; i < smItemLayoutList.size(); i++) {
                smItemLayout = smItemLayoutList.get(i);
                frameContainer = (FrameLayout) smItemLayout.findViewWithTag(TAG_ITEM_CONTAINER);
                frameContainer.setLayoutParams(containerLayoutParams);
                if (i == 0) { // When the initial menu is displayed, the first Fragment is displayed by default.
                    FrameLayout pagerLayout = (FrameLayout) smItemLayout.findViewWithTag(TAG_ITEM_PAGER);
                    // First remove the first layout that contains Fragment
                    frameContainer.removeView(pagerLayout);

                    // Create a FrameLayout for placeholders
                    FrameLayout holderLayout = new FrameLayout(getContext());
                    LinearLayout.LayoutParams pagerLinLayParams = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
                    holderLayout.setLayoutParams(pagerLinLayParams);

                    // Add a placeholder FrameLayout to the frameContainer in the layout
                    frameContainer.addView(holderLayout, 0);

                    // Add the first layout containing the Fragment to the SpinMenu
                    FrameLayout.LayoutParams pagerFrameParams = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
                    pagerLayout.setLayoutParams(pagerFrameParams);
                    addView(pagerLayout);
                }

                // Show title
                if (hintStrList != null && !hintStrList.isEmpty() && i < hintStrList.size()) {
                    tvHint = (TextView) smItemLayout.findViewWithTag(TAG_ITEM_HINT);
                    tvHint.setText(hintStrList.get(i));
                    tvHint.setTextSize(hintTextSize);
                    tvHint.setTextColor(hintTextColor);
                }

                // The SMItemlayout on both sides of the currently displayed Fragment in the menu moves around TRAN_SKNEW_VALUE distance
                if (spinMenuLayout.getSelectedPosition() + 1 == i
                        || (spinMenuLayout.isCyclic()
                            && spinMenuLayout.getMenuItemCount() - i == spinMenuLayout.getSelectedPosition() + 1)) { // Right ItemMenu
                    smItemLayout.setTranslationX(TRAN_SKNEW_VALUE);
                } else if (spinMenuLayout.getSelectedPosition() - 1 == i
                        || (spinMenuLayout.isCyclic()
                            && spinMenuLayout.getMenuItemCount() - i == 1)) { // Left ItemMenu
                    smItemLayout.setTranslationX(-TRAN_SKNEW_VALUE);
                } else {
                    smItemLayout.setTranslationX(0);
                }
            }
            spinMenuAnimator = new SpinMenuAnimator(this, spinMenuLayout, onSpinMenuStateChangeListener);
            init = false;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (enableGesture) menuDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (enableGesture) {
            menuDetector.onTouchEvent(event);
            return true;
        } else {
            return super.onTouchEvent(event);
        }
    }

    /**
     * Convert from px (pixels) to sp by phone resolution
     * @param pxValue
     * @return
     */
    private int px2Sp(float pxValue) {
        final float fontScale = getContext().getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    private void log(String log) {
        Log.d(TAG, log);
    }

    public void setFragmentAdapter(PagerAdapter adapter) {
        if (pagerAdapter != null) {
            pagerAdapter.startUpdate(spinMenuLayout);
            for (int i = 0; i < adapter.getCount(); i++) {
                ViewGroup pager = (ViewGroup) spinMenuLayout.getChildAt(i).findViewWithTag(TAG_ITEM_PAGER);
                pagerAdapter.destroyItem(pager, i, pagerObjects.get(i));
            }
            pagerAdapter.finishUpdate(spinMenuLayout);
        }

        int pagerCount = adapter.getCount();
        if (pagerCount > spinMenuLayout.getMaxMenuItemCount())
            throw new RuntimeException(String.format("Fragment number can't be more than %d", spinMenuLayout.getMaxMenuItemCount()));

        pagerAdapter = adapter;

        SMItemLayout.LayoutParams itemLinLayParams = new SMItemLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        LinearLayout.LayoutParams containerLinlayParams = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        FrameLayout.LayoutParams pagerFrameParams = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        LinearLayout.LayoutParams hintLinLayParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        hintLinLayParams.topMargin = HINT_TOP_MARGIN;
        pagerAdapter.startUpdate(spinMenuLayout);
        for (int i = 0; i < pagerCount; i++) {
            // Create menu parent container layout
            SMItemLayout smItemLayout = new SMItemLayout(getContext());
            smItemLayout.setId(i + 1);
            smItemLayout.setGravity(Gravity.CENTER);
            smItemLayout.setLayoutParams(itemLinLayParams);

            // Create a package FrameLayout
            FrameLayout frameContainer = new FrameLayout(getContext());
            frameContainer.setId(pagerCount + i + 1);
            frameContainer.setTag(TAG_ITEM_CONTAINER);
            frameContainer.setLayoutParams(containerLinlayParams);

            // Create a Fragment container
            FrameLayout framePager = new FrameLayout(getContext());
            framePager.setId(pagerCount * 2 + i + 1);
            framePager.setTag(TAG_ITEM_PAGER);
            framePager.setLayoutParams(pagerFrameParams);
            Object object = pagerAdapter.instantiateItem(framePager, i);

            // Create a menu title TextView
            TextView tvHint = new TextView(getContext());
            tvHint.setId(pagerCount * 3 + i + 1);
            tvHint.setTag(TAG_ITEM_HINT);
            tvHint.setLayoutParams(hintLinLayParams);

            frameContainer.addView(framePager);
            smItemLayout.addView(frameContainer);
            smItemLayout.addView(tvHint);
            spinMenuLayout.addView(smItemLayout);

            pagerObjects.add(object);
            smItemLayoutList.add(smItemLayout);
        }
        pagerAdapter.finishUpdate(spinMenuLayout);
    }

    public void openMenu() {
        if (menuState == MENU_STATE_CLOSED) {
            spinMenuAnimator.openMenuAnimator();
        }
    }

    public void closeMenu(SMItemLayout chooseItemLayout) {
        if (menuState == MENU_STATE_OPENED) {
            spinMenuAnimator.closeMenuAnimator(chooseItemLayout);
        }
    }

    public int getMenuState() {
        return menuState;
    }

    public void updateMenuState(int state) {
        menuState = state;
    }

    public void setEnableGesture(boolean enable) {
        enableGesture = enable;
    }

    public void setMenuItemScaleValue(float scaleValue) {
        scaleRatio = scaleValue;
    }

    public void setHintTextSize(int textSize) {
        hintTextSize = textSize;
    }

    public void setHintTextColor(int textColor) {
        hintTextColor = textColor;
    }

    public void setHintTextStrList(List<String> hintTextList) {
        hintStrList = hintTextList;
    }

    public void setOnSpinMenuStateChangeListener(OnSpinMenuStateChangeListener listener) {
        onSpinMenuStateChangeListener = listener;
    }

    public float getScaleRatio() {
        return scaleRatio;
    }
}
