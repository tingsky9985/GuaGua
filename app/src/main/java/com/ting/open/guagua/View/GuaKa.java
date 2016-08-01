package com.ting.open.guagua.View;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.ting.open.guagua.R;

/**
 * Created by lt on 2016/7/31.
 */
public class GuaKa extends View {

    //遮盖层view
    private Paint mOutterPaint;
    private Path mPath;
    private Canvas mCanvas;
    private Bitmap mBitmap;

    private int mLastX;
    private int mLastY;


    private Bitmap mOutterBitmap;
    //绘制底层图片
    //private Bitmap bitmap;

    //绘制底层文本
    private String mText;
    //绘制底层文本画笔
    private Paint mBackPaint;
    //记录底层刮奖信息文本的宽高
    private Rect mTextBound;
    //记录底层图层文字的大小
    private int mTextSize;
    //记录底层图层文字的颜色
    private int mTextColor;

    //判断覆盖层区域是否消除达到最大值
    //volatile:两个线程对比进行操作，保证子线程更新，主线程可见
    private volatile boolean mComplete = false;

    //刮刮卡完毕后的回调信息
    public interface OnGuaKaCompleteListener{
        void complete();
    }

    public void setOnGuaKaCompleteListener(OnGuaKaCompleteListener mListener) {
        this.mListener = mListener;
    }

    private OnGuaKaCompleteListener mListener;


    public GuaKa(Context context) {
        this(context,null);
    }

    public GuaKa(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public GuaKa(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //初始化信息
        init();


        //自定义文本信息
        TypedArray a = null;
        try {
            a = context.getTheme().obtainStyledAttributes(attrs,R.styleable.GuaKa,defStyleAttr,0);

            int n = a.getIndexCount();

            for(int i = 0; i < n; i ++){
                int attr = a.getIndex(i);
                switch (attr){
                    case R.styleable.GuaKa_text:
                        mText = a.getString(attr);
                        break;
                    case R.styleable.GuaKa_textSize:
                        mTextSize = (int) a.getDimension(attr, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,22,getResources().getDisplayMetrics()));
                        break;
                    case R.styleable.GuaKa_textColor:
                        mTextColor = a.getColor(attr,0x000000);
                        break;
                    default:

                }
            }
        }finally {
            if(a != null){
                a.recycle();
            }
        }

    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //获取控件宽高
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        //初始化Bitmap
        mBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        //绘制path画笔的属性
        setupOutPaint();
        //设置绘制获奖信息的画笔属性
        setOutBackPaint();

        //遮盖层图层颜色
        //mCanvas.drawColor(Color.parseColor("#c0c0c0"));
        mCanvas.drawRoundRect(new RectF(0,0,width,height),30,30,mOutterPaint);
        mCanvas.drawBitmap(mOutterBitmap,null,new Rect(0,0,width,height),null);
    }

    /*
    *设置绘制获奖信息的画笔属性
     */
    private void setOutBackPaint() {
        mBackPaint.setColor(mTextColor);
        mBackPaint.setStyle(Paint.Style.FILL);
        mBackPaint.setTextSize(mTextSize);
        //获得当前画笔绘制文本的宽和高
        mBackPaint.getTextBounds(mText,0,mText.length(),mTextBound);
    }

    /*
     *绘制path画笔的属性
    */
    private void setupOutPaint() {
        mOutterPaint.setColor(Color.parseColor("#c0c0c0"));
        mOutterPaint.setAntiAlias(true);
        mOutterPaint.setDither(true);
        mOutterPaint.setStrokeJoin(Paint.Join.ROUND);
        mOutterPaint.setStrokeCap(Paint.Cap.ROUND);
        mOutterPaint.setStyle(Paint.Style.FILL);
        mOutterPaint.setStrokeWidth(20);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getAction();

        //获取当前事件坐标值
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (action){
            case MotionEvent.ACTION_DOWN:

                mLastX = x;
                mLastY = y;
                mPath.moveTo(mLastX,mLastY);

                break;
            case MotionEvent.ACTION_MOVE:
                //移动x,y轴的绝对值
                int dx = Math.abs(x - mLastX);
                int dy = Math.abs(y - mLastY);

                //增加距离判断，避免频繁的调用
                if(dx>3 || dy>3){
                    mPath.lineTo(x,y);
                }

                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
                new Thread(mRunnable).start();
                break;
            default:
                break;
        }

        invalidate();
        return true;
}


    @Override
    protected void onDraw(Canvas canvas) {

        //绘制底层图片
        //  canvas.drawBitmap(bitmap,0,0,null);
        canvas.drawText(mText, getWidth()/2 - mTextBound.width()/2, getHeight()/2 + mTextBound.height()/2, mBackPaint);

        //在UI线程实现回调，防止主线程
        if(mComplete){
            //回调监听
            if(mListener != null){
                mListener.complete();
            }
        }

        //若遮盖层区域大于指定比例，则不绘制遮盖层图层
        if(!mComplete) {
            drawPath();
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }
    }

    private void drawPath() {

        //绘制时改变为STROKE
        mOutterPaint.setStyle(Paint.Style.STROKE);
        //底层图片
        mOutterPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        mCanvas.drawPath(mPath,mOutterPaint);
    }



    /*
     * 声明初始化操作
     */
    private void init() {

        mOutterPaint = new Paint();
        mPath = new Path();

        //引入底部图片
        //bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.t2);
        mOutterBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.fg_guaguaka);
        //初始化底层图层变量
        mText = "谢谢惠顾";
        mTextSize = 60;
        mTextBound = new Rect();
        mBackPaint = new Paint();

        mTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,22,getResources().getDisplayMetrics());


    }

    //启动任务，判断是否消除图层内容达到最大值
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            int w = getWidth();
            int h = getHeight();

            float wipeArea = 0;
            //总共像素值
            float totalArea = w * h;

            Bitmap bitmap = mBitmap;
            //bitmap上的像素数组信息
            int[] mPixels = new int[w * h];

            //获取Bitmap上的所有像素信息
            bitmap.getPixels(mPixels,0,w,0,0,w,h);

            for(int i = 0; i < w; i ++){
                for(int j = 0; j < h; j ++){
                    int index = i + j * w;

                    if(mPixels[index] == 0){
                        wipeArea ++;
                    }
                }
            }

            if(wipeArea > 0 && totalArea > 0){
                //获取挂图层信息的百分比
                int percent = (int) (wipeArea * 100 / totalArea);

                Log.e("TAG",percent + "");

                //若清楚掉图层区域大于60%
                if(percent > 60){
                    //清除掉图层区域
                    mComplete = true;
                    postInvalidate();
                }
            }

        }
    };

    /*
    *对外提供字体内容改变接口
     */
    public void setText(String text){
        this.mText = text;
        //重新测量字体宽度，获取当前画笔绘制的文本宽度和高度
        mBackPaint.getTextBounds(mText,0,mText.length(),mTextBound);
    }
}
