package marc.com.zoomimage.view;

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
 * Description : 图片自动适应控件，支持多点触控放大与缩小，以及双击放大缩小
 * Version : 1.0
 * Creater : Marc
 * Createtime : 16/8/23.
 */
public class ZoomImageView extends ImageView implements ViewTreeObserver.OnGlobalLayoutListener
			,ScaleGestureDetector.OnScaleGestureListener,View.OnTouchListener{

	private boolean mOnce = false;

	//初始化的缩放的值
	private float mInitScale;
	//双击缩放的值
	private float mMidScale;
	//最大放大的值
	private float mMaxScale;

	private Matrix mScaleMatrix;

	//捕获用户多点触控时缩放的比例
	private ScaleGestureDetector mScaleGestureDetector;


	//-------------------------自由移动
	//记录上一次多点触控的数量
	private int mLastPointerCount;

	private float mLastX;
	private float mLastY;

	private int mTouchSlop;
	private boolean isCanDrag;

	private boolean isCheckLeftAndRight;
	private boolean isCheckTopAndBottom;
	//---------------------双击放大与缩小
	private GestureDetector mGestureDetector;
	private boolean isAutoScale;

	public ZoomImageView(Context context) {
		this(context,null);
	}

	public ZoomImageView(Context context, AttributeSet attrs) {
		this(context, attrs,0);
	}

	public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		//init
		mScaleMatrix = new Matrix();
		super.setScaleType(ScaleType.MATRIX);
		mScaleGestureDetector = new ScaleGestureDetector(context,this);
		setOnTouchListener(this);

		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

		mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				if(isAutoScale)
					return true;

				float x = e.getX();
				float y = e.getY();

				if(getScale() < mMidScale){
					/*mScaleMatrix.postScale(mMidScale/getScale(),mMidScale/getScale(),x,y);
					setImageMatrix(mScaleMatrix);*/
					postDelayed(new AutoScaleRunnable(mMidScale,x,y),16);
					isAutoScale = true;
				}else{
					/*mScaleMatrix.postScale(mInitScale/getScale(),mInitScale/getScale(),x,y);
					setImageMatrix(mScaleMatrix);*/
					postDelayed(new AutoScaleRunnable(mInitScale,x,y),16);
					isAutoScale = true;
				}

				return true;
			}
		});

	}

	/**
	 * 自动放大与缩小
	 */
	private class AutoScaleRunnable implements Runnable{
		//缩放的目标值
		private float mTargetScale;
		//缩放的中心点
		private float x;
		private float y;
		//缩放的梯度
		private final float BIGGER  = 1.07f;
		private final float SMALL  = 0.93f;

		private float tempScale;

		public AutoScaleRunnable(float mTargetScale, float x, float y) {
			this.mTargetScale = mTargetScale;
			this.x = x;
			this.y = y;

			if(getScale() < mTargetScale)
				tempScale = BIGGER;
			else
				tempScale = SMALL;

		}

		@Override
		public void run() {
			//进行缩放
			mScaleMatrix.postScale(tempScale,tempScale,x,y);
			checkBorderAndCenterWhenScale();
			setImageMatrix(mScaleMatrix);

			float currenScale = getScale();
			if((tempScale>1.0f && currenScale<mTargetScale) ||
					(tempScale<1.0f&&currenScale>mTargetScale)){
				postDelayed(this,16);
			}else{
				//设置目标值
				float scale  = mTargetScale/currenScale;
				mScaleMatrix.postScale(scale,scale,x,y);
				checkBorderAndCenterWhenScale();
				setImageMatrix(mScaleMatrix);
				isAutoScale = false;
			}
		}
	}

	//获取imageview加载完成的图片
	@Override
	public void onGlobalLayout() {
		if(!mOnce){

			//获得控件的宽和高
			int width = getWidth();
			int height = getHeight();
			//得到图片以及宽和高
			Drawable d = getDrawable();
			if(d == null)
				return;
			int dw = d.getIntrinsicWidth();
			int dh = d.getIntrinsicHeight();

			float scale = 1.0f;
			//如果图片的宽带大于控件的宽度，高度小于控件的高度，将图片缩小
			if(dw > width && dh < height){
				scale = width*1.0f/dw;
			}
			//如果图片的高度大于控件的高度，宽度小于控件的宽度，将图片缩小
			if(dh>height && dw<width){
				scale = height*1.0f/dh;
			}
			//如果图片的宽高都大于控件的宽高，或者都小于控件的宽高，取缩放的最小值
			if((dw>width && dh>height)||(dw<width && dh<height)){
				scale = Math.min(width*1.0f/dw,height*1.0f/dh);
			}

			//初始化时缩放的比例
			mInitScale  =scale;
			mMaxScale  = mInitScale*4;
			mMidScale = mInitScale*2;


			//将图片移动到控件的中心
			int dx = getWidth()/2 - dw/2;
			int dy = getHeight()/2 - dh/2;

			mScaleMatrix.postTranslate(dx,dy);
			mScaleMatrix.postScale(mInitScale,mInitScale,getWidth()/2,getHeight()/2);
			setImageMatrix(mScaleMatrix);

			mOnce = true;
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		getViewTreeObserver().addOnGlobalLayoutListener(this);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		getViewTreeObserver().removeOnGlobalLayoutListener(this);
	}

	/**
	 * 获取当前图片的缩放值
	 * @return
	 */
	public float getScale(){
		float[] values = new float[9];
		mScaleMatrix.getValues(values);

		return values[Matrix.MSCALE_X];
	}

	//缩放区间initScale - maxScale
	@Override
	public boolean onScale(ScaleGestureDetector scaleGestureDetector) {

		float scale = getScale();
		float scaleFactor = scaleGestureDetector.getScaleFactor();

		if(getDrawable() == null)
			return true;
		//缩放范围的控制
		if((scale < mMaxScale && scaleFactor > 1.0f) || (scale>mInitScale&&scaleFactor<1.0f)){
			if(scale*scaleFactor < mInitScale){
				scaleFactor = mInitScale/scale;
			}

			if(scale*scaleFactor > mMaxScale){
				scaleFactor = mMaxScale/scale;
			}

			//缩放
			mScaleMatrix.postScale(scaleFactor,scaleFactor,scaleGestureDetector.getFocusX(),scaleGestureDetector.getFocusY());

			checkBorderAndCenterWhenScale();

			setImageMatrix(mScaleMatrix);
		}


		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

	}

	@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {

		if(mGestureDetector.onTouchEvent(motionEvent))
			return true;

		mScaleGestureDetector.onTouchEvent(motionEvent);

		float x = 0;
		float y = 0;

		//拿到多点触控的数量
		int pointCount = motionEvent.getPointerCount();
		for(int i=0;i<pointCount;i++){
			x += motionEvent.getX(i);
			y += motionEvent.getY(i);
		}
		y /= pointCount;
		x /= pointCount;

		if(mLastPointerCount != pointCount){
			isCanDrag = false;
			mLastX = x;
			mLastY = y;
		}
		mLastPointerCount = pointCount;

		RectF rect = getMatrixRectF();

		switch (motionEvent.getAction()){
			case MotionEvent.ACTION_DOWN:

				if(rect.width() > getWidth()+0.01 || rect.height()>getHeight()+0.01){
					if(getParent() instanceof ViewPager)
						getParent().requestDisallowInterceptTouchEvent(true);
				}
				break;
			case MotionEvent.ACTION_MOVE:

				if(rect.width() > getWidth()+0.01 || rect.height()>getHeight()+0.01){
					if(getParent() instanceof ViewPager)
						getParent().requestDisallowInterceptTouchEvent(true);
				}

				float dx = x - mLastX;
				float dy = y - mLastY;
				if(!isCanDrag){
					isCanDrag = isMoveAction(dx,dy);
				}

				if(isCanDrag){
					RectF rectF = getMatrixRectF();
					if(getDrawable() != null){

						isCheckLeftAndRight=isCheckTopAndBottom = true;

						//如果宽度小于控件宽度,不允许横向移动
						if(rectF.width() < getWidth()){
							isCheckLeftAndRight = false;
							dx = 0;
						}
						//如果高度小于控件高度，不允许纵向移动
						if(rectF.height() < getHeight()){
							isCheckTopAndBottom = false;
							dy = 0;
						}

						mScaleMatrix.postTranslate(dx,dy);

						checkBorderAndCenterWhenTranslate();
						setImageMatrix(mScaleMatrix);
					}
				}
				mLastX = x;
				mLastY = y;
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mLastPointerCount = 0;

				break;
		}

		return true;
	}

	/**
	 * 判断是否move
	 * @param dx
	 * @param dy
	 * @return
	 */
	private boolean isMoveAction(float dx,float dy){
		return Math.sqrt(dx*dx + dy*dy) > mTouchSlop;
	}

	/**
	 * 获得图片放大缩小以后的宽和高以及l,r,b,t
	 * @return
	 */
	private RectF getMatrixRectF(){
		Matrix matrix = mScaleMatrix;
		RectF rectF = new RectF();

		Drawable drawable = getDrawable();
		if(drawable != null){
			rectF.set(0,0,drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight());
			matrix.mapRect(rectF);
		}

		return rectF;
	}

	/**
	 * 在缩放的时候进行边界以及位置的控制
	 */
	private void checkBorderAndCenterWhenScale(){
		RectF rect = getMatrixRectF();

		float deltaX = 0;
		float deltaY = 0;

		int width = getWidth();
		int height = getHeight();

		//缩放时进行边界检测，防止出现白边
		if(rect.width() >= width){
			if(rect.left > 0){
				deltaX = -rect.left;
			}
			if(rect.right <width){
				deltaX = width - rect.right;
			}
		}
		if(rect.height() >= height){
			if(rect.top > 0 ){
				deltaY = -rect.top;
			}
			if(rect.bottom < height){
				deltaY = height - rect.bottom;
			}
		}

		//如果宽度或者高度小于控件的宽和高，居中
		if(rect.width() < width){
			deltaX = width/2f - rect.right + rect.width()/2;
		}
		if(rect.height() < height){
			deltaY = height/2f - rect.bottom + rect.height()/2;
		}

		mScaleMatrix.postTranslate(deltaX,deltaY);
	}

	/**
	 * 当移动时进行边界检查
	 */
	private void checkBorderAndCenterWhenTranslate(){
		RectF rectF = getMatrixRectF();

		float deltaX = 0;
		float deltaY = 0;

		int width =  getWidth();
		int height = getHeight();

		if(rectF.top > 0 && isCheckTopAndBottom){
			deltaY = -rectF.top;
		}
		if(rectF.bottom<height && isCheckTopAndBottom){
			deltaY = height-rectF.bottom;
		}
		if(rectF.left>0 && isCheckLeftAndRight){
			deltaX = -rectF.left;
		}
		if(rectF.right<width && isCheckLeftAndRight){
			deltaX = width- rectF.right;
		}
		mScaleMatrix.postTranslate(deltaX,deltaY);

	}
}
