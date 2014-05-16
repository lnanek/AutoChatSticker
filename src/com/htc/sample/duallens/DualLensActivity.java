package com.htc.sample.duallens;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.htc.lib1.duallens.Constants;
import com.htc.lib1.duallens.DualLens;

public class DualLensActivity extends Activity {

	private static final String TAG = DualLensActivity.class.getSimpleName();

	private Button button;
	private TextView errorText;
	private ImageView image;
	private ImageView origImage;
	private boolean mIsBokehReady;
	private int mStrength = 60;

	final String filename = "dualLensSample.jpg";
	final String filepath = 
			Environment.getExternalStorageDirectory() 
			+ java.io.File.separator + filename;
	Bitmap originalImageBitmap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mIsBokehReady = false;
		image = (ImageView) findViewById(R.id.imageViewMap);
		origImage = (ImageView) findViewById(R.id.imageView);
		button = (Button) findViewById(R.id.button1);
		errorText = (TextView) findViewById(R.id.errorText);
		AssetManager assetManager = getAssets();
		try {
			InputStream istr = assetManager.open(filename);
			
			originalImageBitmap = BitmapFactory.decodeStream(istr);
			origImage.setImageBitmap(originalImageBitmap);
			addListenerOnButton();
			Log.d(TAG, "originalImageBitmap width = " + originalImageBitmap.getWidth());
			Log.d(TAG, "originalImageBitmap height = " + originalImageBitmap.getHeight());
			
			FileUtils.copyInputStreamToFile(assetManager.open(filename), new File(filepath));			
		} catch (IOException e) {
			Toast.makeText(this, "Error loading image", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}

	}

	public void addListenerOnButton() {
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (mIsBokehReady) {
					mStrength += 10;
					if (mStrength > 100) {
						mStrength = 0;
					}
					button.setText("strength " + mStrength);
					mIsBokehReady = false;
					mDualLens.setStrength(mStrength);
					try {
						mDualLens.calculateBokeh();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	private void drawMask() {
		DualLens.Holder<byte[]> buf = mDualLens.new Holder<byte[]>();
		DualLens.DataInfo datainfo = mDualLens.getStrengthMap(buf);
		int[] outputImage = new int[datainfo.width * datainfo.height];
		int pixelDepth;
		for (int i = 0; i < datainfo.width * datainfo.height; i++) {
			pixelDepth = buf.value[i] & 0x000000ff;
			int depthX = i % datainfo.width;
			int depthY = i / datainfo.width;
			int originalX = originalImageBitmap.getWidth() * depthX / datainfo.width;
			int originalY = originalImageBitmap.getHeight() * depthY / datainfo.height;
			// White out background, pick original color from foreground.
			outputImage[i] = pixelDepth > 64 ? Color.WHITE :
					originalImageBitmap.getPixel(originalX, originalY);
		}
		Bitmap bmp = Bitmap.createBitmap(outputImage, datainfo.width, datainfo.height, Config.ARGB_8888);
		image.setImageBitmap(bmp);
		image.setBackgroundColor(Color.WHITE);
	}

	private DualLens mDualLens = null;
	private int[] mColorBar = null;

	@Override
	protected void onResume() {
		super.onResume();
		try {
			Bitmap tmp = BitmapFactory.decodeStream(this.getAssets().open("coloridx.bmp"));
			mColorBar = new int[tmp.getHeight() * tmp.getWidth()];
			for (int i = 0; i < tmp.getHeight(); i++) {
				for (int j = 0; j < tmp.getWidth(); j++) {
					mColorBar[j + i * tmp.getWidth()] = tmp.getPixel(j, i);
				}
			}
		} catch (IOException e) {
			Log.e("DualLensActivity", "IOException!");
		}

		try {
			mDualLens = new DualLens(DualLensActivity.this, filepath);

			mDualLens.setOnCompletionListener(new DualLens.OnCompletionListener() {
				@Override
				public void onCompletion(DualLens arg0, int event, int extra, String path) {
					switch (event) {
					case Constants.TASK_COMPLETED_PREPARE:
						if (extra == 0) {
							mIsBokehReady = false;
							mDualLens.setStrength(mStrength);
							try {
								mDualLens.calculateBokeh();
							} catch (IllegalStateException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						break;
					case Constants.TASK_COMPLETED_BOKEH:
						mIsBokehReady = true;
						drawMask();
						break;
					}
				}
			});
			mDualLens.prepare();
		} catch (UnsatisfiedLinkError error) {
			errorText.setText("HTC One M8 with Sense 6 required");
			button.setVisibility(View.GONE);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mDualLens != null) {
			mDualLens.release();
		}
	}

}
