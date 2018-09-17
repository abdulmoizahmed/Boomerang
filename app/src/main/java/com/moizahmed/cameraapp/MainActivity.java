package com.moizahmed.cameraapp;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.otaliastudios.cameraview.*;
import com.otaliastudios.cameraview.CameraUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 3;
    private static final int MY_PERMISSIONS_REQUEST_STORAGE = 4;
    private static final int MY_PERMISSIONS_REQUEST_STORAGE_READ = 5;
    private Bitmap bitmap;
    Button capture;
    ArrayList<Bitmap> bitmaps = new ArrayList<>();
    CameraView camera;
    TextView count;
    ProgressDialog dialog;
    FFmpeg ffmpeg;
    File output;
    String reversedPath;
    String finalPath;
    File logo;
    Button cameraButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        camera = (CameraView) findViewById(R.id.camera);
        cameraButton  = (Button) findViewById(R.id.startCamera);
        camera.setPlaySounds(false);
        camera.setSessionType(SessionType.VIDEO);
        camera.setAudio(Audio.OFF);

        count = (TextView) findViewById(R.id.countdown);
        capture = (Button) findViewById(R.id.startCamera);
        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setTitle("Please wait");
        dialog.setMessage("Creating GIF from images");
        //camera.addCameraListener(new CustomCameraListener());
        requestCamera();
        createWaterMarkImage();
        loadFFMpegBinary();
    }

    private void createWaterMarkImage() {
        try{
            logo=new File("/sdcard/LUX/lux.png");
            InputStream inputStream = getResources().openRawResource(R.drawable.lux); // id drawable
            OutputStream out=new FileOutputStream(logo);
            byte buf[]=new byte[1024];
            int len;
            while((len=inputStream.read(buf))>0)
                out.write(buf,0,len);
            out.close();
            inputStream.close();
        }
        catch (IOException e){}

}

    private void requestCamera() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.CAMERA)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(this, "You need to give this permission", Toast.LENGTH_SHORT).show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }


    }

    public void onClick(View View) {
        if (View == findViewById(R.id.startCamera)) {
            cameraButton.setClickable(false);
            startCountDown();
        } else {
            camera.toggleFacing();
        }
    }

    private void startVideo() {
        camera.addCameraListener(new CameraListener() {
            @Override
            public void onVideoTaken(File video) {
                reversedPath = "/sdcard/LUX/"+System.currentTimeMillis()+".mp4";
                String command[] = {"-i", video.getAbsolutePath(),"-preset", "ultrafast", "-vf", "reverse", "-af", "areverse", reversedPath};
                execFFmpegBinary(command);
            }
        });

        File folder = new File("/sdcard/LUX/");
        folder.mkdir();
        String input = "" + System.currentTimeMillis() + ".mp4";
        output = new File(folder, input);

        camera.setVideoQuality(VideoQuality.MAX_480P);
        camera.setVideoMaxDuration(3000);
        camera.startCapturingVideo(output);


    }

    private void startCountDown() {
        count.setVisibility(View.VISIBLE);
        new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long l) {
                count.setText("" + l / 1000);
            }

            @Override
            public void onFinish() {

                count.setVisibility(View.GONE);
                startVideo();
                captureCountDown();

            }
        }.start();
    }

    private void captureCountDown() {
        count.setText("Creating GIF. Please Wait...");
        new CountDownTimer(3000, 1000) {

            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                count.setVisibility(View.VISIBLE);
                camera.stopCapturingVideo();
                }
        }.start();
    }

    private void loadFFMpegBinary() {
        try {
            if (ffmpeg == null) {
                ffmpeg = FFmpeg.getInstance(MainActivity.this);
            }
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure()
                {
                    Toast.makeText(MainActivity.this, "failed loaded", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess()
                {
                    Toast.makeText(MainActivity.this, "successfully loaded", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (FFmpegNotSupportedException e) {
        } catch (Exception e) {
        }
    }

    private void execFFmpegBinary(final String[] command)
    {
        try {
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {

                    Log.v("reverse","failed : "+s);
                }

                @Override
                public void onSuccess(String s)
                {
                    Log.v("reverse","success : "+s);
                    finalPath =  "/sdcard/LUX/"+System.currentTimeMillis()+".mp4";
                    String command[] = {"-i",output.getAbsolutePath(),"-i",reversedPath,"-i",output.getAbsolutePath(),"-i",reversedPath,"-i",logo.getAbsolutePath(),"-preset", "ultrafast","-filter_complex","[0:0] [1:0] [2:0] [3:0] concat=n=4:v=1:a=0,overlay=main_w-overlay_w-5:5",finalPath};
                    concatVideos(command);
                }


                @Override
                public void onProgress(String s)
                {
                    Log.v("reverse","progress : "+s);
                }

               });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Log.v("reverse","exception : "+e);

        }
    }

    public String getURLForResource (int resourceId) {
        File file = new File(Uri.parse("android.resource://"+R.class.getPackage().getName()+"/" +resourceId).toString());
        return file.getAbsolutePath();
    }

    private void concatVideos(String[] command) {
        try {
            ffmpeg.execute(command,new ExecuteBinaryResponseHandler()
            {
                @Override
                public void onFailure(String s) {
                    Log.v("reverse","failed : "+s);
                }

                @Override
                public void onSuccess(String s)
                {
                    deleteVideoFile(reversedPath);
                    deleteVideoFile(output.getAbsolutePath());
                    Log.v("reverse","success : "+s);
                    Intent intent = new Intent(MainActivity.this, PreviewGIF.class);
                    intent.putExtra("fileName", finalPath);
                    startActivity(intent);
                }


                @Override
                public void onProgress(String s)
                {
                    Log.v("reverse","progress : "+s);
                }



            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    private void deleteVideoFile(String absolutePath) {
        File file = new File(absolutePath);
        file.delete();
    }

    private Bitmap addWaterMark(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, 0, 0, null);

        Bitmap waterMark = BitmapFactory.decodeResource(getResources(), R.drawable.ic_lux);
        canvas.drawBitmap(waterMark, w - 150, -50, null);

        return result;
    }

    private void reverseBitmaps() {
        ArrayList<Bitmap> reverseBitmaps = new ArrayList<>();
        reverseBitmaps.addAll(bitmaps);
        Collections.reverse(bitmaps);
        reverseBitmaps.addAll(bitmaps);
        bitmaps.clear();
        bitmaps.addAll(reverseBitmaps);
    }


    @Override
    protected void onResume() {
        super.onResume();
        bitmaps.clear();
        camera.start();
        count.setVisibility(View.GONE);
        cameraButton.setClickable(true);
    }

    @Override
    protected void onPause() {
        camera.stop();
        super.onPause();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestStorage();
                }
                return;
            }

            case MY_PERMISSIONS_REQUEST_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestReadStorage();
                }
                return;
            }

        }
    }

    private void requestReadStorage() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(this, "You need to give this permission", Toast.LENGTH_SHORT).show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_STORAGE_READ);

                // Permission has already been granted
            }
        }
    }

    private void requestStorage() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(this, "You need to give this permission", Toast.LENGTH_SHORT).show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_STORAGE);

                // Permission has already been granted
            }
        }
    }


    /*public String generateDefaultGIF() {

        String gifName = "";
        try {
            AnimatedGIFWriter writer = new AnimatedGIFWriter(true);
            File folder = new File("/sdcard/LUX/");
            folder.mkdir();
            gifName = "" + System.currentTimeMillis() + ".gif";
            File output = new File(folder, gifName);
            OutputStream os = new FileOutputStream(output);

            writer.prepareForWrite(os, -1, -1);
            for (Bitmap bitmap : bitmaps) {
                writer.writeFrame(os, bitmap);
            }
            writer.finishWrite(os);


            //progressBar.setVisibility(View.INVISIBLE);

        } catch (Exception e) {

            e.printStackTrace();
        }

        return gifName;
    }
*/

/*    private class CustomCameraListener extends CameraListener {
        @Override
        public void onPictureTaken(byte[] jpeg) {
            super.onPictureTaken(jpeg);
            com.otaliastudios.cameraview.CameraUtils.decodeBitmap(jpeg, new CameraUtils.BitmapCallback() {
                @Override
                public void onBitmapReady(Bitmap bitmap) {
                   // bitmaps.add(addWaterMark(getResizedBitmap(bitmap, 500)));
                }
            });
        }
    }*/

    /*public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image, width, height, true);
    }
*/

/*    private class GifOperations extends AsyncTask {

        public GifOperations(MainActivity activity) {
            dialog = new ProgressDialog(activity);
        }

        private ProgressDialog dialog;
*/


  /*      @Override
        protected void onPreExecute() {
            super.onPreExecute();
     *//*       dialog.show();
            dialog.setMessage("Making your GIF, please wait...");*//*
        }

        @Override
        protected String doInBackground(Object[] objects) {
            return generateDefaultGIF();
        }


        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
          *//*  if (dialog != null && dialog.isShowing())
                dialog.dismiss();
*//*
            count.setText("");
            count.setVisibility(View.GONE);
            Intent intent = new Intent(MainActivity.this, PreviewGIF.class);
            intent.putExtra("fileName", o.toString());
            startActivity(intent);

        }
    }
*/

}
