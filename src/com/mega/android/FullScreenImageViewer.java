package com.mega.android;

import java.io.File;
import java.util.ArrayList;

import com.mega.components.ExtendedViewPager;
import com.mega.components.TouchImageView;
import com.mega.sdk.MegaApiAndroid;
import com.mega.sdk.MegaApiJava;
import com.mega.sdk.MegaError;
import com.mega.sdk.MegaNode;
import com.mega.sdk.MegaRequest;
import com.mega.sdk.MegaRequestListenerInterface;
import com.mega.sdk.NodeList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class FullScreenImageViewer extends Activity implements OnPageChangeListener, OnClickListener, MegaRequestListenerInterface{
	
	private Display display;
	private DisplayMetrics outMetrics;
	private float density;
	private float scaleW;
	private float scaleH;
	
	private boolean aBshown = true;
	
	ProgressDialog statusDialog;
	
	private MegaFullScreenImageAdapter adapterMega;
	private MegaOfflineFullScreenImageAdapter adapterOffline;
	private int positionG;
	private ArrayList<Long> imageHandles;
	
	private ImageView actionBarIcon;
	private ImageView overflowIcon;
	private ImageView shareIcon;
	
	private RelativeLayout bottomLayout;
    private RelativeLayout topLayout;
	private ExtendedViewPager viewPager;	
	
	static FullScreenImageViewer fullScreenImageViewer;
    private MegaApiAndroid megaApi;

    private ArrayList<String> paths;
    
    int adapterType = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fullScreenImageViewer = this;
		MegaApplication app = (MegaApplication)getApplication();
		megaApi = app.getMegaApi();
		
		fullScreenImageViewer = this;
		
		setContentView(R.layout.activity_full_screen_image_viewer);
		
		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		
	    scaleW = Util.getScaleW(outMetrics, density);
	    scaleH = Util.getScaleH(outMetrics, density);
		
		viewPager = (ExtendedViewPager) findViewById(R.id.image_viewer_pager);
		viewPager.setPageMargin(40);
		
		Intent intent = getIntent();
		positionG = intent.getIntExtra("position", 0);
		
		imageHandles = new ArrayList<Long>();
		paths = new ArrayList<String>();
		long parentNodeHandle = intent.getLongExtra("parentNodeHandle", -1);
		MegaNode parentNode;		
		
		adapterType = intent.getIntExtra("adapterType", 0);
		if (adapterType == ManagerActivity.OFFLINE_ADAPTER){
			File offlineDirectory = null;
			if (Environment.getExternalStorageDirectory() != null){
				offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR);
			}
//			if (getExternalFilesDir(null) != null){
//				offlineDirectory = getExternalFilesDir(null);
//			}
			else{
				offlineDirectory = getFilesDir();
			}
			
			paths.clear();			
			int imageNumber = 0;
			int index = 0;
			File[] fList = offlineDirectory.listFiles();
			for (File f : fList){
				if (MimeType.typeForName(f.getName()).isImage()){
					paths.add(f.getAbsolutePath());
					if (index == positionG){
						positionG = imageNumber; 
					}
					imageNumber++;
				}
				index++;
				
				
				
//				if (f.isDirectory()){
//					File[] document = f.listFiles();
//					if (document.length == 0){
//						try {
//							Util.deleteFolderAndSubfolders(f);
//						} catch (Exception e) {}
//					}
//					else{
//						if (MimeType.typeForName(document[0].getName()).isImage()){
//							paths.add(document[0].getAbsolutePath());
//							if (index == positionG){
//								positionG = imageNumber; 
//							}
//							imageNumber++;
//						}
//						index++;
//					}
//				}
			}
			Toast.makeText(this, "Offline: _" + paths.size(), Toast.LENGTH_LONG).show();
			
			adapterOffline = new MegaOfflineFullScreenImageAdapter(fullScreenImageViewer,paths);
			
			viewPager.setAdapter(adapterOffline);
			
			viewPager.setCurrentItem(positionG);
	
			viewPager.setOnPageChangeListener(this);
			
			actionBarIcon = (ImageView) findViewById(R.id.full_image_viewer_icon);
			actionBarIcon.setOnClickListener(this);
			
			overflowIcon = (ImageView) findViewById(R.id.full_image_viewer_overflow);
			overflowIcon.setVisibility(View.GONE);
			
			shareIcon = (ImageView) findViewById(R.id.full_image_viewer_share);
			shareIcon.setVisibility(View.GONE);
			
			bottomLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_bottom);
		    topLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_top);
			
			
		}
		else{

			if (parentNodeHandle == -1){
	
				switch(adapterType){
					case ManagerActivity.FILE_BROWSER_ADAPTER:{
						parentNode = megaApi.getRootNode();
						break;
					}
					case ManagerActivity.RUBBISH_BIN_ADAPTER:{
						parentNode = megaApi.getRubbishNode();
						break;
					}
					case ManagerActivity.SHARED_WITH_ME_ADAPTER:{
						parentNode = megaApi.getInboxNode();
						break;
					}
					default:{
						parentNode = megaApi.getRootNode();
						break;
					}
				}
				
			}
			else{
				parentNode = megaApi.getNodeByHandle(parentNodeHandle);
			}
			
			NodeList nodes = megaApi.getChildren(parentNode);
			int imageNumber = 0;
			for (int i=0;i<nodes.size();i++){
				MegaNode n = nodes.get(i);
				if (MimeType.typeForName(n.getName()).isImage()){
					imageHandles.add(n.getHandle());
					if (i == positionG){
						positionG = imageNumber; 
					}
					imageNumber++;
				}
			}
//			Toast.makeText(this, ""+parentNode.getName() + "_" + imageHandles.size(), Toast.LENGTH_LONG).show();
				
			adapterMega = new MegaFullScreenImageAdapter(fullScreenImageViewer,imageHandles, megaApi);
			
			viewPager.setAdapter(adapterMega);
			
			viewPager.setCurrentItem(positionG);
	
			viewPager.setOnPageChangeListener(this);
			
			actionBarIcon = (ImageView) findViewById(R.id.full_image_viewer_icon);
			actionBarIcon.setOnClickListener(this);
			
			overflowIcon = (ImageView) findViewById(R.id.full_image_viewer_overflow);
			overflowIcon.setOnClickListener(this);
			
			shareIcon = (ImageView) findViewById(R.id.full_image_viewer_share);
			shareIcon.setOnClickListener(this);
			
			bottomLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_bottom);
		    topLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_top);
		}
		
	}

	@Override
	public void onPageSelected(int position) {
		return;
	}
	
	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		return;
	}
	
	@Override
	public void onPageScrollStateChanged(int state) {

		if (state == ViewPager.SCROLL_STATE_IDLE){
			if (viewPager.getCurrentItem() != positionG){
				int oldPosition = positionG;
				int newPosition = viewPager.getCurrentItem();
				positionG = newPosition;
				
				try{
					if (adapterType == ManagerActivity.OFFLINE_ADAPTER){
						
					}
					else{
						TouchImageView tIV = adapterMega.getVisibleImage(oldPosition);
						if (tIV != null){
							tIV.setZoom(1);
						}
					}
				}
				catch(Exception e){}
//				title.setText(names.get(positionG));
			}
		}
	}
	
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//	    MenuInflater inflater = getMenuInflater();
//	    inflater.inflate(R.menu.full_screen_image_view_menu, menu);
//	    	    
//	    return super.onCreateOptionsMenu(menu);
//	}

	@Override
	public void onClick(View v) {

		switch (v.getId()){
			case R.id.full_image_viewer_icon:{
				finish();
				break;
			}
			case R.id.full_image_viewer_overflow:{
				Toast.makeText(this, "OVERFLOW CLICKED", Toast.LENGTH_LONG).show();
				break;
			}
			case R.id.full_image_viewer_share:{
				MegaNode n = megaApi.getNodeByHandle(imageHandles.get(positionG));
				
				File previewFolder = PreviewUtils.getPreviewFolder(this);
				File previewFile = new File(previewFolder, n.getBase64Handle() + ".jpg");
				
				if (previewFile.exists()){
					Intent share = new Intent(android.content.Intent.ACTION_SEND);
					share.setType("image/*");
					share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + previewFile));
					startActivity(Intent.createChooser(share, getString(R.string.context_share_image)));
				}
				else{
					Toast.makeText(this, getString(R.string.full_image_viewer_not_preview), Toast.LENGTH_LONG).show();
				}
				
				break;
			}
		}
	}
	
	@Override
	public void onSaveInstanceState (Bundle savedInstanceState){
		super.onSaveInstanceState(savedInstanceState);

		savedInstanceState.putInt("adapterType", adapterType);
		if (adapterType == ManagerActivity.OFFLINE_ADAPTER){
			
		}
		else{
			savedInstanceState.putBoolean("aBshown", adapterMega.isaBshown());
		}
	}
	
	@Override
	public void onRestoreInstanceState (Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		
		adapterType = savedInstanceState.getInt("adapterType");
		
		if (adapterType == ManagerActivity.OFFLINE_ADAPTER){
			
		}
		else{
			aBshown = savedInstanceState.getBoolean("aBshown");
			adapterMega.setaBshown(aBshown);
		}
		
		if (!aBshown){
			TranslateAnimation animBottom = new TranslateAnimation(0, 0, 0, Util.px2dp(48, outMetrics));
			animBottom.setDuration(0);
			animBottom.setFillAfter( true );
			bottomLayout.setAnimation(animBottom);
			
			TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, Util.px2dp(-48, outMetrics));
			animTop.setDuration(0);
			animTop.setFillAfter( true );
			topLayout.setAnimation(animTop);
		}
	}
	
//	public void getPublicLinkAndShareIt(){
//		
//		if (!Util.isOnline(this)){
//			Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
//			return;
//		}
//		
//		if(isFinishing()){
//			return;	
//		}
//		
//		ProgressDialog temp = null;
//		try{
//			temp = new ProgressDialog(this);
//			temp.setMessage(getString(R.string.context_creating_link));
//			temp.show();
//		}
//		catch(Exception e){
//			return;
//		}
//		statusDialog = temp;
//		
//		megaApi.exportNode(node, this);
//	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		
//		node = megaApi.getNodeByHandle(request.getNodeHandle());
//		
//		log("onRequestFinish");
//		if (request.getType() == MegaRequest.TYPE_EXPORT){
//			try { 
//				statusDialog.dismiss();	
//			} 
//			catch (Exception ex) {}
//			
//			if (e.getErrorCode() == MegaError.API_OK){
//				String link = request.getLink();
//				if (fullScreenImageViewer != null){
//					Intent intent = new Intent(Intent.ACTION_SEND);
//					intent.setType("text/plain");
//					intent.putExtra(Intent.EXTRA_TEXT, link);
//					startActivity(Intent.createChooser(intent, getString(R.string.context_share_image)));
//				}
//			}
//			else{
//				Toast.makeText(this, "Impossible to get the link", Toast.LENGTH_LONG).show();
//			}
//			log("export request finished");
//		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());	
	}
	
	public static void log(String message) {
		Util.log("FilePropertiesActivity", message);
	}
}
