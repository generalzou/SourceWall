package net.nashlegend.sourcewall;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import net.nashlegend.sourcewall.util.Consts;
import net.nashlegend.sourcewall.util.DisplayUtil;
import net.nashlegend.sourcewall.util.SharedPreferencesUtil;

public class SwipeActivity extends BaseActivity {

    private SwipeLayout swipeLayout;
    protected boolean swipeAnyWhere = false;//是否可以在页面任意位置右滑关闭页面，如果是false则从左边滑才可以关闭

    @Override
    public void setTheme(int resid) {
        if (SharedPreferencesUtil.readBoolean(Consts.Key_Is_Night_Mode, false)) {
            resid = R.style.AppThemeNight;
        } else {
            resid = R.style.AppTheme;
        }
        super.setTheme(resid);
    }

    public void directlySetTheme(int resid) {
        super.setTheme(resid);
    }

    public SwipeActivity() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        swipeLayout = new SwipeLayout(this);
    }

    @Override
    protected void onResume() {
        swipeAnyWhere = false;
        super.onResume();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        swipeLayout.replaceLayer(this);
    }

    public static int getScreenWidth(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) context
                .getSystemService(WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    private boolean swipeFinished = false;

    @Override
    public void finish() {
        if (swipeFinished) {
            super.finish();
            overridePendingTransition(0, 0);
        } else {
            swipeLayout.cancelPotentialAnimation();
            super.finish();
            overridePendingTransition(0, R.anim.slide_out_right);
        }
    }

    class SwipeLayout extends FrameLayout {

        //private View backgroundLayer;用来设置滑动时的背景色
        private Drawable leftShadow;

        public SwipeLayout(Context context) {
            super(context);
        }

        public SwipeLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public SwipeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        public void replaceLayer(Activity activity) {
            leftShadow = activity.getResources().getDrawable(R.drawable.left_shadow);
            touchSlop = DisplayUtil.dip2px(touchSlopDP, activity);
            sideWidth = (int) (sideWidthInDP * activity.getResources().getDisplayMetrics().density);
            mActivity = activity;
            screenWidth = getScreenWidth(activity);
            setClickable(true);
//            backgroundLayer = new View(activity);
//            backgroundLayer.setBackgroundColor(Color.parseColor("#88000000"));
            final ViewGroup root = (ViewGroup) activity.getWindow().getDecorView();
            content = root.getChildAt(0);
            //在Android5.0上，content的高度不再是屏幕高度，而是变成了Activity高度，比屏幕高度低一些，
            //如果this.addView(content),就会使用以前的params，这样content会像root一样比content高出一部分，导致底部空出一部分
            //在装有Android 5.0的Nexus5上，root,SwipeLayout和content的高度分别是1920、1776、1632，144的等差数列……
            //在装有Android4.4.3的HTC One M7上，root,SwipeLayout和content的高度分别相同，都是1920
            //所以我们要做的就是给content一个新的LayoutParams，Match_Parent那种，也就是下面的-1
            ViewGroup.LayoutParams params = content.getLayoutParams();
            ViewGroup.LayoutParams params2 = new ViewGroup.LayoutParams(-1, -1);
//            ViewGroup.LayoutParams params3 = new ViewGroup.LayoutParams(-1, -1);
            root.removeView(content);
//            this.addView(backgroundLayer, params3);
            this.addView(content, params2);
            root.addView(this, params);
        }

        @Override
        protected boolean drawChild(@NonNull Canvas canvas, @NonNull View child, long drawingTime) {
            boolean result = super.drawChild(canvas, child, drawingTime);
            final int shadowWidth = leftShadow.getIntrinsicWidth();
            int left = (int) (getContentX()) - shadowWidth;
            leftShadow.setBounds(left, child.getTop(),
                    left + shadowWidth, child.getBottom());
            leftShadow.draw(canvas);
            return result;
        }

        boolean canSwipe = false;
        View content;
        Activity mActivity;
        int sideWidthInDP = 16;
        int sideWidth = 72;
        int screenWidth = 1080;
        VelocityTracker tracker;

        float downX;
        float downY;
        float lastX;
        float currentX;
        float currentY;


        int touchSlopDP = 30;
        int touchSlop = 60;

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (swipeAnyWhere) {
                switch (ev.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = ev.getX();
                        downY = ev.getY();
                        currentX = downX;
                        currentY = downY;
                        lastX = downX;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = ev.getX() - downX;
                        float dy = ev.getY() - downY;
                        if ((dy == 0f || Math.abs(dx / dy) > 1) && (dx * dx + dy * dy > touchSlop * touchSlop)) {
                            downX = ev.getX();
                            downY = ev.getY();
                            currentX = downX;
                            currentY = downY;
                            lastX = downX;
                            canSwipe = true;
                            tracker = VelocityTracker.obtain();
                            return true;
                        }
                        break;
                }
            } else if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getX() < sideWidth) {
                canSwipe = true;
                tracker = VelocityTracker.obtain();
                return true;
            }
            return super.onInterceptTouchEvent(ev);
        }

        boolean hasIgnoreFirstMove;

        @Override
        public boolean onTouchEvent(@NonNull MotionEvent event) {
            if (canSwipe) {
                tracker.addMovement(event);
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        currentX = downX;
                        currentY = downY;
                        lastX = downX;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        currentX = event.getX();
                        currentY = event.getY();
                        float dx = currentX - lastX;
                        if (dx != 0f && !hasIgnoreFirstMove) {
                            hasIgnoreFirstMove = true;
                            dx = dx / dx;
                        }
                        if (getContentX() + dx < 0) {
                            setContentX(0);
                        } else {
                            setContentX(getContentX() + dx);
                        }
                        lastX = currentX;
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        tracker.computeCurrentVelocity(10000);
                        tracker.computeCurrentVelocity(1000, 20000);
                        canSwipe = false;
                        hasIgnoreFirstMove = false;
                        int mv = screenWidth / 200 * 1000;
                        if (Math.abs(tracker.getXVelocity()) > mv) {
                            animateFromVelocity(tracker.getXVelocity());
                        } else {
                            if (getContentX() > screenWidth / 2) {
                                animateFinish(false);
                            } else {
                                animateBack(false);
                            }
                        }
                        tracker.recycle();
                        break;
                    default:
                        break;
                }
            }
            return super.onTouchEvent(event);
        }

        ObjectAnimator animator;

        public void cancelPotentialAnimation() {
            if (animator != null) {
                animator.removeAllListeners();
                animator.cancel();
            }
        }

        public void setContentX(float x) {
            int ix = (int) x;
            content.setX(ix);
            invalidate();
//            if (backgroundLayer != null) {
//                backgroundLayer.setAlpha(1 - x / getWidth());
//            }
        }

        public float getContentX() {
            return content.getX();
        }


        /**
         * 弹回，不关闭，因为left是0，所以setX和setTranslationX效果是一样的
         *
         * @param withVel 使用计算出来的时间
         */
        private void animateBack(boolean withVel) {
            cancelPotentialAnimation();
            animator = ObjectAnimator.ofFloat(this, "contentX", getContentX(), 0);
            int tmpDuration = withVel ? ((int) (duration * getContentX() / screenWidth)) : duration;
            if (tmpDuration < 100) {
                tmpDuration = 100;
            }
            animator.setDuration(tmpDuration);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.start();
        }

        private void animateFinish(boolean withVel) {
            cancelPotentialAnimation();
            animator = ObjectAnimator.ofFloat(this, "contentX", getContentX(), screenWidth);
            int tmpDuration = withVel ? ((int) (duration * (screenWidth - getContentX()) / screenWidth)) : duration;
            if (tmpDuration < 100) {
                tmpDuration = 100;
            }
            animator.setDuration(tmpDuration);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addListener(new AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!mActivity.isFinishing()) {
                        swipeFinished = true;
                        mActivity.finish();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }
            });
            animator.start();
        }

        private final int duration = 200;

        private void animateFromVelocity(float v) {
            if (v > 0) {
                if (getContentX() < screenWidth / 2
                        && v * duration / 1000 + getContentX() < screenWidth) {
                    animateBack(false);
                } else {
                    animateFinish(true);
                }
            } else {
                if (getContentX() > screenWidth / 2
                        && v * duration / 1000 + getContentX() > screenWidth / 2) {
                    animateFinish(false);
                } else {
                    animateBack(true);
                }
            }

        }
    }

}
