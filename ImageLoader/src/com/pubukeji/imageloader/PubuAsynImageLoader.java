package com.pubukeji.imageloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.pubukeji.imageloader.exeception.AnimNotFoundExeception;
import com.pubukeji.imageloader.exeception.ImgTypeNotSettingException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

/**
 * 公司：瀑布科技 名称：图片异步加载器<br />
 * E-mail:steven.feng.0901@gmail.com<br />
 * QQ:443889604<br />
 * 
 * @author steven
 * @version 1.2.0
 */
public class PubuAsynImageLoader {
	private static final String tag = "PubuAsynImageLoader";
	/**
	 * 加载图片成功参数
	 */
	private final int SUCCESS_LOADED_IMAGE = 0x000001;
	/**
	 * 加载图片失败参数
	 */
	private final int FAILED_LOADED_IMAGE = 0x000002;
	private final int ANIM_SHOWING_TIME = 450;
	/**
	 * 图片内存缓存
	 */
	private LruCache<String, SoftReference<Bitmap>> imageCache;
	/**
	 * 临时信息匹配的集合
	 */
	// private Map<String, ImageLoaderObject> temps;
	/**
	 * 自己的引用
	 */
	private static Context context;
	private Handler handler;
	/**
	 * 当前的图片存储路径
	 */
	private static File currentSavePath;
	private boolean isEnableClear;
	/**
	 * 预先加载的图片资源
	 */
	private int preloadImgRessource;
	private ExecutorService executorService;
	/**
	 * 是否从网络获取
	 */
	private boolean fetchFromWeb;

	/**
	 * 图片加载类型(src和background)
	 * 
	 * @author steven
	 * 
	 */
	public static enum ImageType {
		SRC, BACKGROUND;
	}

	/**
	 * 图片显示动画
	 */
	private Animation animImgShow;

	// public int getPreloadImgRessource() {
	// return preloadImgRessource;
	// }
	//
	// public void setPreloadImgRessource(int preloadImgRessource) {
	// this.preloadImgRessource = preloadImgRessource;
	// }

	/**
	 * 构造函数
	 */
	private PubuAsynImageLoader() {
		initArgs();
		// collectionEnv();
	}

	/**
	 * 收集一下环境变量
	 */
	private static void collectionEnv() {
		// 1.判断有无外置SD卡
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {
			// SD卡可用状态
			if (null != context) {
				currentSavePath = context.getExternalCacheDir();
			} else {
				currentSavePath = context.getCacheDir();
			}

		}
	}

	/**
	 * 初始化参数
	 */

	private void initArgs() {
		fetchFromWeb = true; // 默认情况下可以从网络获取图片
		imageCache = new LruCache<String, SoftReference<Bitmap>>(
				4 * 1024 * 1024);
		executorService = Executors.newCachedThreadPool();

		// 设置初始的图片加载动画
		animImgShow = new AlphaAnimation(0.0f, 1.0f);
		animImgShow.setFillAfter(true);
		animImgShow.setDuration(ANIM_SHOWING_TIME);
		// temps = new HashMap<String, PubuAsynImageLoader.ImageLoaderObject>();
		handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				ImageLoaderObject object = (ImageLoaderObject) msg.obj;
				switch (msg.what) {
				case SUCCESS_LOADED_IMAGE:
					if (null != object.callBack) {
						object.callBack.onSucess(object.iv, object.bitmap);
					} else {
						if (null != object.type) {
							if (object.type.equals(ImageType.SRC)) {
								setSrcNoAnim(object.getIv(),
										object.getBitmap(),
										object.isNeedRoundCorner(),
										object.getRoundCornerPixels());
							} else if (object.type.equals(ImageType.BACKGROUND)) {
								setBackgroundNoAnim(object.getIv(),
										object.getBitmap(),
										object.isNeedRoundCorner(),
										object.getRoundCornerPixels());
							}
						} else {
							// 没有设置自动加载参数
							throw new ImgTypeNotSettingException(
									"ImageType参数未设置");
						}
					}

					break;

				case FAILED_LOADED_IMAGE:
					object.callBack.onFailed(object.iv, "加载失败");
					break;
				}
				object = null;
			}

		};
	}

	private static class ImageLoaderProxy {
		public static PubuAsynImageLoader loaderProxy = new PubuAsynImageLoader();
	}

	/**
	 * 自动设置图片
	 * 
	 * @param iv
	 *            显示bitmap的imageview
	 * @param imageUrl
	 *            加载imageUrl的地址
	 * @param type
	 *            图片加载类型
	 * @param needRoundCorner
	 *            是否需要圆角样式
	 * @param RoundCornerPixels
	 *            圆角的像素
	 * 
	 */
	public void loadBitMap(ImageView iv, String imageUrl, ImageType type,
			boolean needRoundCorner, int RoundCornerPixels) {
		loadBitMap(iv, imageUrl, type, null, needRoundCorner, RoundCornerPixels);
	}

	/**
	 * 手动设置图片
	 * 
	 * @param iv
	 *            显示bitmap的imageview
	 * @param imageUrl
	 *            加载imageUrl的地址
	 * @param callBack
	 *            首次加载回调参数
	 * @param needRoundCorner
	 *            是否需要圆角样式
	 * @param RoundCornerPixels
	 *            圆角的像素
	 */
	public void loadBitMap(ImageView iv, String imageUrl,
			ImageLoaderCallBack callBack, boolean needRoundCorner,
			int RoundCornerPixels) {
		loadBitMap(iv, imageUrl, null, callBack, needRoundCorner,
				RoundCornerPixels);
	}

	/**
	 * 
	 * @param iv
	 *            显示bitmap的imageview
	 * @param imageUrl
	 *            加载imageUrl的地址
	 * @param type
	 *            图片加载类型
	 * @param callBack
	 *            首次加载回调参数
	 * @param needRoundCorner
	 *            是否需要圆角样式
	 * @param RoundCornerPixels
	 *            圆角的像素
	 */
	private void loadBitMap(final ImageView iv, final String imageUrl,
			final ImageType type, final ImageLoaderCallBack callBack,
			final boolean needRoundCorner, final int RoundCornerPixels) {
		// 先设置预存的图片（图片没有被加载出来的图片）
		iv.setImageResource(preloadImgRessource);

		// ---------------------------------------------------------
		// 1.去内存集合找图片
		if (!TextUtils.isEmpty(imageUrl)) {
			if (null != imageCache && null != imageCache.get(imageUrl)) {
				// iv.setImageBitmap(imageCache.get(imageUrl).get());

				Bitmap bitmap = imageCache.get(imageUrl).get();
				if (null != bitmap) {
					Log.i(tag, "在内存中找到");
					if (null != callBack) {
						callBack.onSucess(iv, bitmap);
					} else {
						// 使用自动加载
						if (null != type) {
							if (type.equals(ImageType.SRC)) {
								setSrc(iv, bitmap, needRoundCorner,
										RoundCornerPixels);
							} else if (type.equals(ImageType.BACKGROUND)) {
								setBackground(iv, bitmap, needRoundCorner,
										RoundCornerPixels);
							}
						} else {
							// 没有设置自动加载参数
							throw new ImgTypeNotSettingException(
									"ImageType参数未设置");
						}
					}
					return;
				}

			}
		}
		// ---------------------------------------------------------
		Runnable task = new Runnable() {

			public void run() {
				ImageLoaderObject tempObj = new ImageLoaderObject(imageUrl, iv,
						callBack, null, type, needRoundCorner,
						RoundCornerPixels);
				// temps.put(imageUrl, tempObj);

				Message msg = handler.obtainMessage();
				msg.obj = tempObj;

				// 2.去rom里找图片
				String imageName = imageUrl
						.substring(imageUrl.lastIndexOf("/") + 1);
				File imageFile = null;
				// Log.i(tag, "imageName==" + imageName);
				File cacheDir = currentSavePath;
				File[] files = cacheDir.listFiles();
				if (null != files) {
					for (File file : files) {
						if (imageName.equals(file.getName())) {
							imageFile = file;
							break;
						}
					}
				}
				if (null != imageFile) {
					// 在文件中找到图片，将图片返回
					// Log.i(tag, "从手机中找到图片路径：" + imageFile.getAbsolutePath());
					Bitmap bitmap = BitmapFactory.decodeFile(imageFile
							.getAbsolutePath());
					// imageCache.put(imageUrl, new
					// SoftReference<Bitmap>(bitmap));
					Log.i(tag, "在文件中找到");
					if (null != bitmap) {
						tempObj.setBitmap(bitmap);
						msg.what = SUCCESS_LOADED_IMAGE;
						handler.sendMessage(msg);
					}
					return;
				}

				// 3.从网络中获取
				if (fetchFromWeb) {
					// 同意从网络中获取图片
					try {
						// 通过使用options来动态确定图片的大小，防止出现oom问题
						InputStream is = (new URL(imageUrl)).openStream();
						BitmapFactory.Options opts = new BitmapFactory.Options();
						opts.inJustDecodeBounds = true;
						BitmapFactory.decodeStream(is, null, opts);
						is.close();
						// setOptions(opts, 75, 75);
						opts.inSampleSize = computeSampleSize(opts, -1,
								128 * 128);
						opts.inJustDecodeBounds = false;
						is = (new URL(imageUrl)).openStream();
						Bitmap bitmap = BitmapFactory.decodeStream(is, null,
								opts);
						is.close();
						if (null != bitmap) {
							tempObj.setBitmap(bitmap);
							msg.what = SUCCESS_LOADED_IMAGE;
							handler.sendMessage(msg);
							Log.i(tag, "从网上上下载");
						}
						// 保存信息
						// 1.保存到内存中
						imageCache.put(imageUrl, new SoftReference<Bitmap>(
								bitmap));

						// 2.放入rom中
						// 放入本地缓存
						File file = currentSavePath;
						String bitmapName = imageUrl.substring(imageUrl
								.lastIndexOf("/") + 1);
						File bitmapfile = new File(file.getAbsoluteFile() + "/"
								+ bitmapName);
						// Log.i(tag, "存储到手机里的路径：" +
						// bitmapfile.getAbsolutePath());
						bitmapfile.createNewFile();

						FileOutputStream fos = new FileOutputStream(bitmapfile);
						bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);

						fos.close();
					} catch (Exception e) {
						msg.what = FAILED_LOADED_IMAGE;
						handler.sendMessage(msg);
					}
				}

			}
		};
		executorService.submit(task);
	}

	/**
	 * 得到ImageLoader的实例对象
	 * 
	 * @param context
	 *            上下文
	 * @return ImageLoader实例对象
	 */
	public static PubuAsynImageLoader getInstanceof(Context context) {
		if (null == PubuAsynImageLoader.context) {
			PubuAsynImageLoader.context = context;
			collectionEnv();
		}
		return ImageLoaderProxy.loaderProxy;
	}

	/**
	 * 设置图片网络加载开关
	 * 
	 * @param loading
	 *            是否从网络获取图片
	 */
	public void setImgLoadSwitch(boolean loading) {
		this.fetchFromWeb = loading;
	}

	/**
	 * 设置默认图片资源
	 * 
	 * @param resId
	 */
	public void setDefaultResource(int resId) {
		this.preloadImgRessource = resId;
	}

	/**
	 * 设置图片动画
	 * 
	 * @param animation
	 */
	public void setAnimOfImage(Animation animation) {
		if (null == animation) {
			throw new AnimNotFoundExeception("动画资源没有找到");
		}
		this.animImgShow = animation;
	}

	/**
	 * 设置图片动画
	 * 
	 * @param AnimResource
	 */
	public void setAnimOfImage(int AnimResource) {
		Animation loadAnimation = AnimationUtils.loadAnimation(context,
				AnimResource);
		setAnimOfImage(loadAnimation);
	}

	/**
	 * 清除图片的一、二级缓存
	 * 
	 * @param callBack
	 *            回调方法
	 */
	public void clearAllCache(final ClearCacheCallBack callBack) {
		if (!isEnableClear) {
			isEnableClear = true;
			new Thread() {
				public void run() {
					String size = "0MB";
					if (null == context || null == currentSavePath) {
						callBack.onClearComplete(false, size);
						return;
					}
					// 1.删除文件缓存
					size = Formatter.formatFileSize(context,
							currentSavePath.length());
					boolean flag = deleteAllImage();
					// 2.删除内存缓存
					imageCache = null;
					isEnableClear = false;
					callBack.onClearComplete(flag, size);
				};
			}.start();
		}
		// do nothing
	}

	/**
	 * 删除图片文件
	 * 
	 * @param currentSavePath
	 * @return
	 */
	private boolean deleteAllImage() {
		File[] files = currentSavePath.listFiles();
		if (null != files) {
			for (File file : files) {
				if (".png".endsWith(file.getName())) {
					file.delete();
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * 设置图片(src方式)
	 * 
	 * @param imageView
	 * @param bitmap
	 */
	private void setSrc(ImageView imageView, Bitmap bitmap,
			boolean needRoundCorner, int RoundCornerPixels) {
		imageView.clearAnimation();
		// imageView.setScaleType(ScaleType.CENTER_CROP);
		// imageView.setImageBitmap(bitmap);
		setSrcNoAnim(imageView, bitmap, needRoundCorner, RoundCornerPixels);
		imageView.startAnimation(animImgShow);
	}

	private void setSrcNoAnim(ImageView imageView, Bitmap bitmap,
			boolean needRoundCorner, int RoundCornerPixels) {
		if (needRoundCorner) {
			bitmap = toRoundCorner(bitmap, RoundCornerPixels);
		}
		imageView.setScaleType(ScaleType.CENTER_CROP);
		imageView.setImageBitmap(bitmap);
	}

	/**
	 * 设置图片(background方式)
	 * 
	 * @param imageView
	 * @param bitmap
	 */
	private void setBackground(ImageView imageView, Bitmap bitmap,
			boolean needRoundCorner, int RoundCornerPixels) {
		imageView.clearAnimation();
		// imageView.setBackgroundDrawable(new BitmapDrawable(bitmap));
		setBackgroundNoAnim(imageView, bitmap, needRoundCorner,
				RoundCornerPixels);
		imageView.startAnimation(animImgShow);
	}

	private void setBackgroundNoAnim(ImageView imageView, Bitmap bitmap,
			boolean needRoundCorner, int RoundCornerPixels) {
		if (needRoundCorner) {
			bitmap = toRoundCorner(bitmap, RoundCornerPixels);
		}
		imageView.setBackgroundDrawable(new BitmapDrawable(bitmap));
	}

	public interface ClearCacheCallBack {
		/**
		 * 清除完成的方法
		 * 
		 * @param isSucess
		 *            是否成功
		 * @param size
		 *            清除内容的大小(MB)
		 */
		void onClearComplete(boolean isSucess, String size);
	}

	/**
	 * 图片加载回调
	 * 
	 * @author steven
	 * 
	 */
	public interface ImageLoaderCallBack {
		/**
		 * 成功的回调
		 * 
		 * @param imageView
		 * @param bitmap
		 */
		void onSucess(ImageView imageView, Bitmap bitmap);

		/**
		 * 失败的回调
		 * 
		 * @param imageView
		 * @param errorMsg
		 */
		void onFailed(ImageView imageView, String errorMsg);
	}

	/**
	 * 加载图片相关信息的实体
	 * 
	 * @author steven
	 * 
	 */
	@SuppressWarnings("unused")
	private class ImageLoaderObject {
		private String imageUrl;
		private ImageView iv;
		private ImageLoaderCallBack callBack;
		private Bitmap bitmap;
		private ImageType type;
		private boolean needRoundCorner;
		private int RoundCornerPixels;

		public boolean isNeedRoundCorner() {
			return needRoundCorner;
		}

		public void setNeedRoundCorner(boolean needRoundCorner) {
			this.needRoundCorner = needRoundCorner;
		}

		public int getRoundCornerPixels() {
			return RoundCornerPixels;
		}

		public void setRoundCornerPixels(int roundCornerPixels) {
			RoundCornerPixels = roundCornerPixels;
		}

		public ImageLoaderObject(String imageUrl, ImageView iv,
				ImageLoaderCallBack callBack, Bitmap bitmap, ImageType type,
				boolean needRoundCorner, int roundCornerPixels) {
			super();
			this.imageUrl = imageUrl;
			this.iv = iv;
			this.callBack = callBack;
			this.bitmap = bitmap;
			this.type = type;
			this.needRoundCorner = needRoundCorner;
			RoundCornerPixels = roundCornerPixels;
		}

		public ImageType getType() {
			return type;
		}

		public void setType(ImageType type) {
			this.type = type;
		}

		public Bitmap getBitmap() {
			return bitmap;
		}

		public void setBitmap(Bitmap bitmap) {
			this.bitmap = bitmap;
		}

		public ImageLoaderObject() {
			super();
		}

		public ImageLoaderObject(String imageUrl, ImageView iv,
				ImageLoaderCallBack callBack, Bitmap bitmap, ImageType type) {
			super();
			this.imageUrl = imageUrl;
			this.iv = iv;
			this.callBack = callBack;
			this.bitmap = bitmap;
			this.type = type;
		}

		public String getImageUrl() {
			return imageUrl;
		}

		public void setImageUrl(String imageUrl) {
			this.imageUrl = imageUrl;
		}

		public ImageView getIv() {
			return iv;
		}

		public void setIv(ImageView iv) {
			this.iv = iv;
		}

		public ImageLoaderCallBack getCallBack() {
			return callBack;
		}

		public void setCallBack(ImageLoaderCallBack callBack) {
			this.callBack = callBack;
		}

	}

	private int computeSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		int initialSize = computeInitialSampleSize(options, minSideLength,
				maxNumOfPixels);
		int roundedSize;
		if (initialSize <= 8) {
			roundedSize = 1;
			while (roundedSize < initialSize) {
				roundedSize <<= 1;
			}
		} else {
			roundedSize = (initialSize + 7) / 8 * 8;
		}
		return roundedSize;
	}

	private int computeInitialSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		double w = options.outWidth;
		double h = options.outHeight;
		int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math
				.sqrt(w * h / maxNumOfPixels));
		int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(
				Math.floor(w / minSideLength), Math.floor(h / minSideLength));
		if (upperBound < lowerBound) { // return the larger one when there is no
										// overlapping zone.
			return lowerBound;
		}
		if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
			return 1;
		} else if (minSideLength == -1) {
			return lowerBound;
		} else {
			return upperBound;
		}
	}

	/**
	 * 将图片设置为圆角
	 */
	private Bitmap toRoundCorner(Bitmap bitmap, int pixels) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);
		final float roundPx = pixels;
		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);
		return output;
	}

	/*
	 * private void setOptions(Options opts, int TARGET_HEIGHT, int
	 * TARGET_WIDTH) { Boolean scaleByHeight = Math.abs(opts.outHeight -
	 * TARGET_HEIGHT) >= Math .abs(opts.outWidth - TARGET_WIDTH);
	 * 
	 * if (opts.outHeight * opts.outWidth * 2 >= 200 * 200 * 2) { double
	 * sampleSize = scaleByHeight ? opts.outHeight / TARGET_HEIGHT :
	 * opts.outWidth / TARGET_WIDTH; opts.inSampleSize = (int) Math.pow(2d,
	 * Math.floor(Math.log(sampleSize) / Math.log(2d))); } }
	 */
}
