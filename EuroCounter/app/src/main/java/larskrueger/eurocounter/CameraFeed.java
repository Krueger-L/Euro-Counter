package larskrueger.eurocounter;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;


public class CameraFeed extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback{

    byte[] originalFrame;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private static final String TAG = "CameraFeed";

    public CameraFeed(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera)
    {
        mCamera = camera;
    }

    //needed when Activity is resumed
    public void refreshCamera(Camera camera) {
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        // change camera settings only after stopping preview
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: preview doesnt exist
        }
        // change settings here
        setCamera(camera);
        try {
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(VIEW_LOG_TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    //set Camera preview to this Surfaceholder and start the preview
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // not needed when surface doesnt change

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // camera is released in mainActivity
    }

    //store Frame data to access it later
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        setOriginalFrame(data);
    }

    public void setOriginalFrame(byte[] data){

        originalFrame = data;
    }

    public byte[] getOriginalFrame(){

        return originalFrame;
    }

    //get Preview height
    public int getPreviewH(){
        Camera.Parameters par = mCamera.getParameters();
        Camera.Size size = par.getPreviewSize();
        return size.height;
    }

    //get Preview width
    public int getPreviewW(){
        Camera.Parameters par = mCamera.getParameters();
        Camera.Size size = par.getPreviewSize();
        return size.width;
    }
}
