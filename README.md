PuBuImageLoader
===============

#图片加载组件
#版本：1.2.0
修正：
1.增加了圆角功能
2.增加了图片动画功能
3.提供了图片网络加载开关





使用说明：
imageLoader = PubuAsynImageLoader.getInstanceof(context);   //得到加载器实例
 
imageLoader.setDefaultResource(R.drawable.icon_null);      //设置默认加载图片


imageLoader.setImgLoadSwitch(loading);   //设置网络加载图片开关

//2种方式加载图片载入动画，不设置默认是渐变动画
imageLoader.setAnimOfImage(Animation animation);
imageLoader.setAnimOfImage(int AnimResource);

//2种加载图片方式
imageLoader.loadBitMap(iv, imageUrl, callBack, needRoundCorner, RoundCornerPixels);   //手工方式，使用回调自己操作
imageLoader.loadBitMap(iv, imageUrl, type, needRoundCorner, RoundCornerPixels);       //自动方式，使用type确定图片的类型(src or background)



imageLoader.clearAllCache(callBack)  //清除图片缓存
