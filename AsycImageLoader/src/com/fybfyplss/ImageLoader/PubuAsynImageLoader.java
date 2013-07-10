package com.fybfyplss.ImageLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.widget.ImageView;

/**
 * 公司：瀑布科技 名称：图片异步加载器 
 * E-mail:steven.feng.0901@gmail.com 
 * QQ:443889604
 * @author steven
 * @version 1.1.0
 */
public class PubuAsynImageLoader {
	//private static final String tag = "PubuAsynImageLoader";
	/**
	 * 加载图片成功参数
	 */
	private final int SUCCESS_LOADED_IMAGE = 0x000001;
	/**
	 * 加载图片失败参数
	 */
	private final int FAILED_LOADED_IMAGE = 0x000002;

	/**
	 * 图片内存缓存
	 */
	private Map<String, SoftReference<Bitmap>> imageCache;
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
	//private int preloadImgRessource;
	private ExecutorService executorService;

//	public int getPreloadImgRessource() {
//		return preloadImgRessource;
//	}
//
//	public void setPreloadImgRessource(int preloadImgRessource) {
//		this.preloadImgRessource = preloadImgRessource;
//	}

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
		imageCache = new HashMap<String, SoftReference<Bitmap>>();
		executorService = Executors.newCachedThreadPool();
		// temps = new HashMap<String, PubuAsynImageLoader.ImageLoaderObject>();
		handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				ImageLoaderObject object = (ImageLoaderObject) msg.obj;
				switch (msg.what) {
				case SUCCESS_LOADED_IMAGE:
					object.callBack.onSucess(object.iv, object.bitmap);
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
	 * 
	 * @param iv
	 *            显示bitmap的imageview
	 * @param imageUrl
	 *            加载imageUrl的地址
	 * @param callBack
	 *            首次加载回调参数
	 * @return 查找的bitmap
	 */
	public void loadBitMap(final ImageView iv, final String imageUrl,
			final ImageLoaderCallBack callBack) {
		// 先设置预存的图片（图片没有被加载出来的图片）
		// iv.setImageResource(preloadImgRessource);

		// 1.去内存集合找图片
		if (!TextUtils.isEmpty(imageUrl)) {
			if (null != imageCache && imageCache.containsKey(imageUrl)) {
				// iv.setImageBitmap(imageCache.get(imageUrl).get());
				//Log.i(tag, "在内存中找到");
				callBack.onSucess(iv, imageCache.get(imageUrl).get());
				return;
			}
		}

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
			//imageCache.put(imageUrl, new SoftReference<Bitmap>(bitmap));
			//Log.i(tag, "在文件中找到");
			callBack.onSucess(iv, bitmap);
			return;
		}
		
		
		Runnable task = new Runnable() {

			
			public void run() {
				

				ImageLoaderObject tempObj = new ImageLoaderObject(imageUrl, iv,
						callBack);
				// temps.put(imageUrl, tempObj);

				// 3.从网络中获取
				Message msg = handler.obtainMessage();
				msg.obj = tempObj;
				try {
					// 通过使用options来动态确定图片的大小，防止出现oom问题
					InputStream is = (new URL(imageUrl)).openStream();
					BitmapFactory.Options opts = new BitmapFactory.Options();
					opts.inJustDecodeBounds = true;
					BitmapFactory.decodeStream(is, null, opts);
					is.close();
					// setOptions(opts, 75, 75);
					opts.inSampleSize = computeSampleSize(opts, -1, 128 * 128);
					opts.inJustDecodeBounds = false;
					is = (new URL(imageUrl)).openStream();
					Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
					is.close();
					if (null != bitmap) {
						tempObj.setBitmap(bitmap);
						msg.what = SUCCESS_LOADED_IMAGE;
						handler.sendMessage(msg);
						//Log.i(tag, "从网上上下载");
					}
					// 保存信息
					// 1.保存到内存中
					imageCache.put(imageUrl, new SoftReference<Bitmap>(bitmap));

					// 2.放入rom中
					// 放入本地缓存
					File file = currentSavePath;
					String bitmapName = imageUrl.substring(imageUrl
							.lastIndexOf("/") + 1);
					File bitmapfile = new File(file.getAbsoluteFile() + "/"
							+ bitmapName);
					// Log.i(tag, "存储到手机里的路径：" + bitmapfile.getAbsolutePath());
					bitmapfile.createNewFile();

					FileOutputStream fos = new FileOutputStream(bitmapfile);
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);

					fos.close();
				} catch (Exception e) {
					msg.what = FAILED_LOADED_IMAGE;
					handler.sendMessage(msg);
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
					imageCache.clear();
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
				ImageLoaderCallBack callBack) {
			super();
			this.imageUrl = imageUrl;
			this.iv = iv;
			this.callBack = callBack;
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
