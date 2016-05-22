package com.java.www.zoomimageview.View;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Created by zhangchenggeng
 * Time 2016/3/29 15:19.
 * Descripton:
 * History:
 * 版权所有
 */
public class ZoomImageView extends ImageView implements ViewTreeObserver.OnGlobalLayoutListener, ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {


    private boolean isload = false; //是否初始化
    private float mInitScale; //初始化时缩小的值
    private float mMidScale; //双击放大时的值
    private float mMaxScale; //放大的最大值
    private Matrix matrix;   //变换矩阵

    private ScaleGestureDetector mScaleGestureDetector; //放大识别器


    //==============================

    private  int mLastPointerCount; //上一次多点触控的数量

    private float mLastX;       //上一次多点触控的X轴坐标
    private float mLastY;       //上一次多点触控的Y轴坐标

    private float mTouchSlop;   //判断是否是移动时的参考量
    private boolean isCanDrag=false;  //是否可以拖动

    private boolean isCheckLeftAndRight;
    private boolean isCheckTopAndBottom;

    //======================双击放大缩小
    private GestureDetector gestureDetector;
    private boolean isAutoScale;
    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        matrix = new Matrix();
        setScaleType(ScaleType.MATRIX);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        setOnTouchListener(this);

        //获取比较move的值
        mTouchSlop= ViewConfiguration.get(context).getScaledTouchSlop();

        gestureDetector=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){

            //双击事件
            @Override
            public boolean onDoubleTap(MotionEvent e) {

                if(isAutoScale){
                    return true;
                }

                float x = e.getX();
                float y = e.getY();

                if(getScale()<mMidScale){
//                    matrix.postScale(mMidScale/getScale(),mMidScale/getScale(),x,y);
//                    setImageMatrix(matrix);
                    postDelayed(new AutoScaleRunnable(mMidScale,x,y),16);
                    isAutoScale=true;
                }else{
//                    matrix.postScale(mInitScale/getScale(),mInitScale/getScale(),x,y);
//                    setImageMatrix(matrix);

                    postDelayed(new AutoScaleRunnable(mInitScale,x,y),16);
                    isAutoScale=true;
                }

                return true;
            }
        });

    }

    private class AutoScaleRunnable implements Runnable{

        private float mTargetScale;
        private float x;
        private float y;

        private final  float BIGGER=1.07f;
        private final  float SMALL=0.93f;
        private float tmpScale;

        public AutoScaleRunnable(float mTargetScale, float x, float y) {
            this.mTargetScale = mTargetScale;
            this.x = x;
            this.y = y;

            if(getScale()<mTargetScale){
                tmpScale=BIGGER;
            }
            if(getScale()>mTargetScale){
                tmpScale=SMALL;
            }
        }

        @Override
        public void run() {
            matrix.postScale(tmpScale,tmpScale,x,y);
            checkBorderAndCenterWhenScale();
            setImageMatrix(matrix);

            float currentScale=getScale();
            if(tmpScale>1.0f&&currentScale<mTargetScale||tmpScale<1.0f&&currentScale>mTargetScale){
                postDelayed(this,16);
            }else{ //设置为我们的目标
                float scale=mTargetScale/currentScale;
                matrix.postScale(scale,scale,x,y);
                checkBorderAndCenterWhenScale();
                setImageMatrix(matrix);

                isAutoScale=false;
            }
        }
    }

    //一个参数的构造函数调用两个参数的构造方法
    public ZoomImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    //一个参数的构造函数调用两个参数的构造方法
    public ZoomImageView(Context context) {
        this(context, null);
    }


    //加载到窗口上时
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    //从窗口上卸载时
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //兼容低版本--
        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    @Override
    public void onGlobalLayout() {
        if (!isload) {
            //获得空间的宽和高
            int height = getHeight();
            int width = getWidth();

            //得到图片以及宽和高
            Drawable drawable = getDrawable();
            if (drawable == null) {
                return;
            }

            //获取图片的宽高
            int intrinsicHeight = drawable.getIntrinsicHeight();
            int intrinsicWidth = drawable.getIntrinsicWidth();
            float scale = 1.0f;

            //图片的宽大于控件的宽
            if (intrinsicWidth > width && intrinsicHeight < height) {
                scale = (width * 1.0f / intrinsicWidth);
            }

            //图片的高度大于空间的高度
            if (intrinsicWidth < width && intrinsicHeight > height) {
                scale = (height * 1.0f / intrinsicHeight);
            }

            //高度
            if (intrinsicWidth > width && intrinsicHeight > height || intrinsicWidth < width && intrinsicHeight < height) {
                scale = Math.min(width * 1.0f / intrinsicWidth, height * 1.0f / intrinsicHeight);
            }

            //确定各种比例
            mInitScale = scale;
            mMaxScale = 4 * scale;
            mMidScale = 2 * scale;


            //将图片移动至空间中心
            int dx = getWidth() / 2 - intrinsicWidth / 2;
            int dy = getHeight() / 2 - intrinsicHeight / 2;

            //设置平移跟缩放
            matrix.postTranslate(dx, dy);
            matrix.postScale(mInitScale, mInitScale, width / 2, height / 2);
            setImageMatrix(matrix);


            isload = true;
        }
    }

    //获取到缩放比例
    public float getScale() {
        float[] values = new float[9];
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    //缩放区间 initScale maxScale
    @Override
    public boolean onScale(ScaleGestureDetector detector) {

        float scaleFactor = mScaleGestureDetector.getScaleFactor();
        float scale = getScale();

        if (getDrawable() == null) return true;

        //缩放范围的控制
        if (scale < mMaxScale && scaleFactor > 1.0f || scale > mInitScale && scaleFactor < 1.0f) {

            if (scale * scaleFactor < mInitScale) {
                scaleFactor = mInitScale / scale;
            }

            if (scale * scaleFactor > mMaxScale) {
                scaleFactor = mMaxScale / scale;
            }


            matrix.postScale(scaleFactor, scaleFactor,detector.getFocusX(), detector.getFocusY());
            checkBorderAndCenterWhenScale();
            setImageMatrix(matrix);

        }

        return true;
    }

    /**
     * 获得图片放大以后的宽和高
     *
     * @return
     */
    private RectF getMatrixRectF() {
        RectF rectF = new RectF();
        Drawable drawable = getDrawable();

        if (drawable != null) {
            rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            matrix.mapRect(rectF);
        }
        return  rectF;
    }


    /**
     * 在缩放的时候检测边界和中心点
     */
    private void checkBorderAndCenterWhenScale() {
        RectF rectF = getMatrixRectF();

        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int heigh = getHeight();

        if(rectF.width()>=width){ //水平方向控制
            if(rectF.left>0){
                deltaX=-rectF.left;
            }
            if( rectF.right<width){
                deltaX=width-rectF.right;
            }
        }

        if(rectF.height()>=heigh){ //垂直方向控制
            if(rectF.top>0){
                deltaY=-rectF.top;
            }
            if(rectF.bottom<heigh){
                deltaY=heigh-rectF.bottom;
            }
        }

        //宽度高度小于控件的宽和高 屏幕中间
        if(rectF.width()<width){
            deltaX=width/2-rectF.right+rectF.width()/2;
        }
        if(rectF.height()<heigh){
            deltaY=heigh/2-rectF.bottom+rectF.height()/2;
        }

        matrix.postTranslate(deltaX,deltaY);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;  //必须return true
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if(gestureDetector.onTouchEvent(event)){
            return true;
        }


        mScaleGestureDetector.onTouchEvent(event);

        float x=0;
        float y=0;

        int pointerCount = event.getPointerCount(); //拿到触控点的个数

        for(int i=0;i<pointerCount;i++){
            x+=event.getX(i);
            y+=event.getY(i);
        }

        x/=pointerCount;
        y/=pointerCount;

        if(mLastPointerCount!=pointerCount){
            isCanDrag=false;
            mLastX=x;
            mLastY=y;
        }

        mLastPointerCount=pointerCount;

        RectF rectF=getMatrixRectF();
        switch (event.getAction()){

            case MotionEvent.ACTION_DOWN:
                if(rectF.width()>getWidth()+0.01||rectF.height()>getHeight()+0.01){
                    getParent().requestDisallowInterceptTouchEvent(true); //请求父控件不要拦截
                }
                break;
            case MotionEvent.ACTION_MOVE:

                if(rectF.width()>getWidth()+0.01||rectF.height()>getHeight()+0.01){
                    getParent().requestDisallowInterceptTouchEvent(true); //请求父控件不要拦截
                }

                float dx=x-mLastX;
                float dy=y-mLastY;

                if(!isCanDrag){
                    //判断dx dy 是否足以让图片移动
                    isCanDrag=isMoveAction(dx,dy);
                }

                if(isCanDrag){
                    //完成图片的移动

                    if(getDrawable()!=null){

                        isCheckLeftAndRight=isCheckTopAndBottom=true;
                        if(rectF.width()<getWidth()){  //允许横向移动
                            isCheckLeftAndRight=false;
                            dx=0;
                        }
                        if(rectF.height()<getHeight()){ //不允许
                            isCheckTopAndBottom=false;
                            dy=0;
                        }

                        matrix.postTranslate(dx,dy);
                        checkBorderWhenTraslate();
                        setImageMatrix(matrix);
                    }
                }
                mLastX=x;
                mLastY=y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLastPointerCount=0;
                break;
        }

        return true;  //自己消费掉 就返回true
    }

    /**
     * 在图片自由移动的时候进行边界控制
     */
    private void checkBorderWhenTraslate() {

        RectF rectF=getMatrixRectF();

        float deltaX=0;
        float deltaY=0;

        int width=getWidth();
        int height=getHeight();

        if(rectF.top>0 && isCheckTopAndBottom){
            deltaY=-rectF.top;
        }

        if(rectF.bottom<height&&isCheckTopAndBottom){
            deltaY=height-rectF.bottom;
        }

        if(rectF.left>0&&isCheckLeftAndRight){
            deltaX=-rectF.left;
        }
        if(rectF.right<width&&isCheckLeftAndRight){
            deltaX=width-rectF.right;
        }
        matrix.postTranslate(deltaX, deltaY);
    }

    /**
     * 判断是否移动
     * @param dx
     * @param dy
     * @return
     */
    private boolean isMoveAction(float dx, float dy) {
        return Math.sqrt(dx*dx+dy*dy)>mTouchSlop;
    }




}
