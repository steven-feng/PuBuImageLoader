PuBuImageLoader
===============

#ͼƬ�������
#�汾��1.2.1
������
1.������ͼƬ��ת����





ʹ��˵����
imageLoader = PubuAsynImageLoader.getInstanceof(context);   //�õ�������ʵ��
 
imageLoader.setDefaultResource(R.drawable.icon_null);      //����Ĭ�ϼ���ͼƬ


imageLoader.setImgLoadSwitch(loading);   //�����������ͼƬ����

//2�ַ�ʽ����ͼƬ���붯����������Ĭ���ǽ��䶯��
imageLoader.setAnimOfImage(Animation animation);
imageLoader.setAnimOfImage(int AnimResource);

//2�ּ���ͼƬ��ʽ
imageLoader.loadBitMap(iv, imageUrl, callBack, needRoundCorner, RoundCornerPixels,isRotate,angle);   //�ֹ���ʽ��ʹ�ûص��Լ�����
imageLoader.loadBitMap(iv, imageUrl, type, needRoundCorner, RoundCornerPixels,isRotate,angle);       //�Զ���ʽ��ʹ��typeȷ��ͼƬ������(src or background)



imageLoader.clearAllCache(callBack)  //���ͼƬ����
