package marc.com.ttbviewpager;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import marc.com.zoomimage.view.ZoomImageView;

public class MainActivity extends AppCompatActivity {

	private ViewPager mViewPager;

	private int[] mImgs = new int[]{
			R.drawable.test,R.drawable.splash
	};

	private ImageView[] mImageViews = new ImageView[mImgs.length];

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mViewPager = (ViewPager)findViewById(R.id.viewpager);
		mViewPager.setAdapter(new PagerAdapter() {

			@Override
			public Object instantiateItem(ViewGroup container, int position) {
				ZoomImageView imageView = new ZoomImageView(getApplicationContext());
				imageView.setImageResource(mImgs[position]);
				container.addView(imageView);
				mImageViews[position] = imageView;

				return imageView;
			}

			@Override
			public void destroyItem(ViewGroup container, int position, Object object) {
				container.removeView(mImageViews[position]);
			}

			@Override
			public int getCount() {
				return mImageViews.length;
			}

			@Override
			public boolean isViewFromObject(View view, Object object) {
				return view==object;
			}
		});
	}
}
