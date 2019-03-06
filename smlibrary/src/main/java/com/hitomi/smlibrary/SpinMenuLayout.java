package com.hitomi.smlibrary;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * Created by hitomi on 2016/9/13. <br/>
 *
 * github : https://github.com/Hitomis <br/>
 *
 * email : 196425254@qq.com
 */
public class SpinMenuLayout extends ViewGroup implements Runnable, View.OnClickListener{

    /**
     * Angle of view between views
     */
    private static final int ANGLE_SPACE = 45;

    /**
     * The minimum rotation angle of the view when rotating
     */
    private static final int MIN_PER_ANGLE = ANGLE_SPACE;

    /**
     * Faster for automatic scrolling, no other meaning
     */
    private static final float ACCELERATE_ANGLE_RATIO = 1.8f;

    /**
     * Used to lengthen the radius, no other meaning
     */
    private static final float RADIUS_HALF_WIDTH_RATIO = 1.2f;

    /**
     * The delay ratio of the rotation angle when the rotation angle exceeds the rotatable range
     */
    private static final float DELAY_ANGLE_RATIO = 5.6f;

    /**
     * Click and drag switching threshold
     */
    private final int touchSlopAngle = 2;

    /**
     * Minimum and maximum inertial roll angle values [-(getChildCount() - 1) * ANGLE_SPACE, 0]
     */
    private int minFlingAngle, maxFlingAngle;

    /**
     * delayAngle: the total angle value of the current rotation, perAngle: the angle value of each rotation
     */
    private float delayAngle, perAngle;

    /**
     * Radius: the midpoint from the bottom edge to the Child height
     */
    private float radius;

    /**
     * Coordinate value each time the finger is pressed
     */
    private float preX, preY;

    /**
     * Speed of each rotation
     */
    private float anglePerSecond;

    /**
     * Time value per finger press
     */
    private long preTimes;

    /**
     * Is it possible to loop through
     */
    private boolean isCyclic;

    /**
     * Whether to allow the menu to be rotated
     */
    private boolean enable;

    private Scroller scroller;

    private OnSpinSelectedListener onSpinSelectedListener;

    private com.hitomi.smlibrary.onMenuSelectedListener onMenuSelectedListener;

    public SpinMenuLayout(Context context) {
        this(context, null);
    }

    public SpinMenuLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpinMenuLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        scroller = new Scroller(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Width, height are consistent with the parent container
        ViewGroup parent = ((ViewGroup )getParent());
        int measureWidth = parent.getMeasuredWidth();
        int measureHeight = parent.getMeasuredHeight();
        setMeasuredDimension(measureWidth, measureHeight);

        if (getChildCount() > 0) {
            // Measuring child elements
            measureChildren(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean b, int left, int top, int right, int bottom) {
        final int childCount = getChildCount();
        if (childCount <= 0) return;

        isCyclic = getChildCount() == 360 / MIN_PER_ANGLE;
        computeFlingLimitAngle();

        delayAngle %= 360.f;
        float startAngle = delayAngle;

        View child;
        int childWidth, childHeight;
        int centerX = getMeasuredWidth() / 2;
        int centerY = getMeasuredHeight();
        radius = centerX * RADIUS_HALF_WIDTH_RATIO + getChildAt(1).getMeasuredHeight() / 2;

        for (int i = 0; i < childCount; i++) {
            child = getChildAt(i);
            childWidth = child.getMeasuredWidth();
            childHeight = child.getMeasuredHeight();

            left = (int) (centerX + Math.sin(Math.toRadians(startAngle)) * radius);
            top = (int) (centerY - Math.cos(Math.toRadians(startAngle)) * radius);

            child.layout(left - childWidth / 2, top - childHeight / 2,
                        left + childWidth / 2, top + childHeight / 2);

            child.setOnClickListener(this);
            child.setRotation(startAngle);
            startAngle += ANGLE_SPACE;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!enable) return super.dispatchTouchEvent(ev);
        float curX = ev.getX();
        float curY = ev.getY();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                preX = curX;
                preY = curY;
                preTimes = System.currentTimeMillis();
                perAngle = 0;

                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float diffX = curX - preX;
                float start = computeAngle(preX, preY);
                float end = computeAngle(curX, curY);

                float perDiffAngle;
                if (diffX > 0) {
                    perDiffAngle = Math.abs(start - end);
                } else {
                    perDiffAngle = -Math.abs(end - start);
                }
                if (!isCyclic && (delayAngle < minFlingAngle || delayAngle > maxFlingAngle)) {
                    // Currently not in the cyclic scroll mode, and the angle of rotation is outside the range of the rotatable angle
                    perDiffAngle /= DELAY_ANGLE_RATIO;
                }
                delayAngle += perDiffAngle;
                perAngle += perDiffAngle;

                preX = curX;
                preY = curY;
                requestLayout();
                break;
            case MotionEvent.ACTION_UP:
                anglePerSecond = perAngle * 1000 / (System.currentTimeMillis() - preTimes);
                int startAngle = (int) delayAngle;
                if (Math.abs(anglePerSecond) > MIN_PER_ANGLE && startAngle >= minFlingAngle && startAngle <= maxFlingAngle) {
                    scroller.fling(startAngle, 0, (int) (anglePerSecond * ACCELERATE_ANGLE_RATIO), 0, minFlingAngle, maxFlingAngle, 0, 0);
                    scroller.setFinalX(scroller.getFinalX() + computeDistanceToEndAngle(scroller.getFinalX() % ANGLE_SPACE));
                } else {
                    scroller.startScroll(startAngle, 0, computeDistanceToEndAngle(startAngle % ANGLE_SPACE), 0, 300);
                }

                if (!isCyclic) { // When it is not a cyclic rotation, the angle needs to be corrected.
                    if (scroller.getFinalX() >= maxFlingAngle) {
                        scroller.setFinalX(maxFlingAngle);
                    } else if (scroller.getFinalX() <= minFlingAngle) {
                        scroller.setFinalX(minFlingAngle);
                    }
                }
                // Post a task, scroll automatically
                post(this);
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * Calculate the minimum and maximum inertial roll angles
     */
    private void computeFlingLimitAngle() {
        // Since the center point is at the midpoint of the bottom edge (the coordinate system is opposite),
        // the min and max calculated here are opposite to the actual one.
        minFlingAngle = isCyclic ? Integer.MIN_VALUE : -ANGLE_SPACE * (getChildCount() - 1);
        maxFlingAngle = isCyclic ? Integer.MAX_VALUE : 0;
    }

    /**
     * Calculate the angle of rotation based on the coordinates of the current touch point
     * @param xTouch
     * @param yTouch
     * @return
     */
    private float computeAngle(float xTouch, float yTouch) {
        // The center point is at the midpoint of the bottom edge,
        // and the point is converted to the corresponding coordinate x, y according to the center point.
        float x = Math.abs(xTouch - getMeasuredWidth() / 2);
        float y = Math.abs(getMeasuredHeight() - yTouch);
        return (float) (Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI);
    }

    /**
     * Calculate the angle value at the end of automatic scrolling
     * @param remainder
     * @return
     */
    private int computeDistanceToEndAngle(int remainder) {
        int endAngle;
        if (remainder > 0) {
            if (Math.abs(remainder) > ANGLE_SPACE / 2) {
                if (perAngle < 0) { // Counterclockwise
                    endAngle = ANGLE_SPACE - remainder;
                } else { // Clockwise
                    endAngle = ANGLE_SPACE - Math.abs(remainder);
                }
            } else {
                endAngle = -remainder;
            }
        } else {
            if (Math.abs(remainder) > ANGLE_SPACE / 2) {
                if (perAngle < 0) {
                    endAngle = -ANGLE_SPACE - remainder;
                } else {
                    endAngle = Math.abs(remainder) - ANGLE_SPACE;
                }
            } else {
                endAngle = -remainder;
            }
        }
        return endAngle;
    }

    private int computeClickToEndAngle(int clickIndex, int currSelPos) {
        int endAngle;
        if (isCyclic) {
            clickIndex = clickIndex == 0 && currSelPos == getMenuItemCount() - 1 ? getMenuItemCount() : clickIndex;
            currSelPos = currSelPos == 0 && clickIndex != 1 ? getMenuItemCount() : currSelPos;
        }
        endAngle = (currSelPos - clickIndex) * ANGLE_SPACE;
        return endAngle;
    }

    @Override
    public void run() {
        if (scroller.isFinished()) {
            int position = Math.abs(scroller.getCurrX() / ANGLE_SPACE);
            if (onSpinSelectedListener != null) {
                onSpinSelectedListener.onSpinSelected(position);
            }
        }
        if (scroller.computeScrollOffset()) {
            delayAngle = scroller.getCurrX();
            postDelayed(this, 16);
            requestLayout();
        }
    }

    @Override
    public void onClick(View view) {
        int index = indexOfChild(view);
        int selPos = getSelectedPosition();
        if (Math.abs(perAngle) <= touchSlopAngle) {
            if (index != selPos) {
                // The current click is an Item on both sides, and the item clicked is scrolled to the [Positive] position.
                scroller.startScroll(-getSelectedPosition() * ANGLE_SPACE, 0, computeClickToEndAngle(index, selPos), 0, 300);
                post(this);
            } else {
                if (view instanceof SMItemLayout
                        && onMenuSelectedListener != null
                        && enable) {
                    onMenuSelectedListener.onMenuSelected((SMItemLayout) view);
                }

            }
        }
    }

    /**
     * Get the currently selected location
     * @return
     */
    public int getSelectedPosition() {
        if (scroller.getFinalX() > 0) {
            return (360 - scroller.getFinalX()) / ANGLE_SPACE;
        } else {
            return (Math.abs(scroller.getFinalX())) / ANGLE_SPACE;
        }
    }

    /**
     * Get the true radius of the circular rotation menu<br/>
     * The radius is based on the height of the child plus half the width of the SpinMenuLayout<br/>
     * So when there is no child, the radius is -1
     * @return
     */
    public int getRealRadius() {
        if (getChildCount() > 0) {
            return getMeasuredWidth() / 2 + getChildAt(0).getHeight();
        } else {
            return -1;
        }
    }

    public int getMaxMenuItemCount() {
        return 360 / ANGLE_SPACE;
    }

    public int getMenuItemCount() {
        return getChildCount();
    }

    public boolean isCyclic() {
        return isCyclic;
    }

    public void postEnable(boolean isEnable) {
        enable = isEnable;
    }

    public void setOnSpinSelectedListener(OnSpinSelectedListener listener) {
        onSpinSelectedListener = listener;
    }

    public void setOnMenuSelectedListener(com.hitomi.smlibrary.onMenuSelectedListener listener) {
        onMenuSelectedListener = listener;
    }
}
