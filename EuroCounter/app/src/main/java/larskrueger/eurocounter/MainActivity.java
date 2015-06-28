package larskrueger.eurocounter;

import android.app.Activity;


import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;


public class MainActivity extends Activity{


    private Camera mCamera;
    private CameraFeed mPreview;
    private ImageProcessor iProcessor =  new ImageProcessor();
    TextView txViewCoin1;
    TextView txViewCoin2;
    TextView txViewCoin3;
    TextView txViewCoin4;
    TextView txViewCoin5;
    TextView txViewCoin6;
    TextView txViewCoin7;
    TextView txViewCoin8;
    TextView txViewSum;

    // implements opencvManager
    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    //Log.i(TAG, "OpenCV loaded successfully");

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    // called to update onscreen coin information
    private void setTextViews(){
        txViewCoin1.setText(iProcessor.coin1+"x");
        txViewCoin2.setText(iProcessor.coin2+"x");
        txViewCoin3.setText(iProcessor.coin3+"x");
        txViewCoin4.setText(iProcessor.coin4+"x");
        txViewCoin5.setText(iProcessor.coin5+"x");
        txViewCoin6.setText(iProcessor.coin6+"x");
        txViewCoin7.setText(iProcessor.coin7+"x");
        txViewCoin8.setText(iProcessor.coin8+"x");
        txViewSum.setText(iProcessor.coinSum());
    }

    // do this when the application starts up
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create an instance of Camera
        mCamera = getCameraInstance();
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraFeed(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.CameraFeed);
        preview.addView(mPreview);

        // implement button listener
        final Button button = (Button) findViewById(R.id.button_id);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // the only directive Imageprocessor needs to process the image
                iProcessor.setOriginalMat(
                        mPreview.getOriginalFrame(),
                        mPreview.getPreviewW(),
                        mPreview.getPreviewH());
                // update onscreen information
                setTextViews();

                // display DebugView
                Bitmap bm = Bitmap.createBitmap(
                        iProcessor.getdebugMat().cols(),
                        iProcessor.getdebugMat().rows(),
                        Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(iProcessor.getdebugMat(), bm);
                ImageView iv = (ImageView) findViewById(R.id.imageView2);
                iv.setImageBitmap(bm);

            }
        });

        // initialize textviews for later
        txViewCoin1 = (TextView)findViewById(R.id.textView19);
        txViewCoin2 = (TextView)findViewById(R.id.textView18);
        txViewCoin3 = (TextView)findViewById(R.id.textView17);
        txViewCoin4 = (TextView)findViewById(R.id.textView16);
        txViewCoin5 = (TextView)findViewById(R.id.textView15);
        txViewCoin6 = (TextView)findViewById(R.id.textView14);
        txViewCoin7 = (TextView)findViewById(R.id.textView13);
        txViewCoin8 = (TextView)findViewById(R.id.textView12);
        txViewSum = (TextView)findViewById(R.id.textView20);

    }

    // release camera on paused activity
    @Override
    protected void onPause(){
        super.onPause();
        if(mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    // get camera and refresh CameraFeed + opencvmanager reconnect
    @Override
    protected void onResume(){
        super.onResume();
        if(mCamera == null){
            mCamera = getCameraInstance();
            mPreview.refreshCamera(mCamera);
        }
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mOpenCVCallBack))
        {
            //Log.e(TAG, "Cannot connect to OpenCV Manager");
        }
    }

    // safe way to get camera instance
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
            // Camera is not available
        }
        return c; // returns null if camera is unavailable
    }




    //Functions below are not used because menu is hidden in xml-file
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
