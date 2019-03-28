package com.fr.facedetection;

import java.io.*;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.*;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;
import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;

import org.json.JSONArray;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class MainActivity extends Activity {

    private final String apiEndpoint = "https://southcentralus.api.cognitive.microsoft.com/face/v1.0";
    private final String subscriptionKey = "54b6a545238d4f168a230fa4db09560a";

    private final FaceServiceClient faceServiceClient =
            new FaceServiceRestClient(apiEndpoint, subscriptionKey);

    private Camera mCamera;
    private CameraPreview mCameraPreview;

    private int company_id;
    private Context handler = this;

    private static final int CAMERA_REQUEST = 1888;
    private ImageView imageView;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = getSharedPreferences("MyPref", MODE_PRIVATE);

        company_id = preferences.getInt("company_id", -1);
        if (company_id == -1) {
            Intent setting = new Intent(this, SettingActivity.class);
            startActivity(setting);
            return;
        }

        TextView textCompanyId = findViewById(R.id.text_company_id);
        textCompanyId.setText("Company Id: " + Integer.toString(company_id));

        imageView = (ImageView)this.findViewById(R.id.imageView);
        Button takeButton = (Button) this.findViewById(R.id.buttonTake);
        takeButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                }
                else
                {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
            }
        });

//        requestPermissions();
//
//        if (!checkPermissionIsGranted()) {
//            requestPermission();
//        }



//        startDetection();

//        Button buttonTake = findViewById(R.id.buttonTake);
//        buttonTake.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mCamera.takePicture(null, null, mPicture);
//            }
//        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK)
        {
            final Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(outputStream.toByteArray());

            AsyncTask<InputStream, String, Face[]> detectTask =
                    new AsyncTask<InputStream, String, Face[]>() {
                        String exceptionMessage = "";

                        @Override
                        protected Face[] doInBackground(InputStream... params) {
                            try {
                                Face[] result = faceServiceClient.detect(
                                        params[0],
                                        true,
                                        false,
                                        new FaceServiceClient.FaceAttributeType[] {
                                                FaceServiceClient.FaceAttributeType.Gender,
                                                FaceServiceClient.FaceAttributeType.Age,
                                                FaceServiceClient.FaceAttributeType.FacialHair,
                                                FaceServiceClient.FaceAttributeType.Glasses,
                                                FaceServiceClient.FaceAttributeType.Makeup,
                                                FaceServiceClient.FaceAttributeType.Emotion,
                                                FaceServiceClient.FaceAttributeType.Occlusion,
                                                FaceServiceClient.FaceAttributeType.Accessories,
                                                FaceServiceClient.FaceAttributeType.Hair,
                                                FaceServiceClient.FaceAttributeType.Smile,
                                        }
                                );
                                if (result == null || result.length == 0){
                                    return null;
                                }


                                for (int i = 0; i < result.length; i ++) {
                                    String faceId;
                                    SimilarPersistedFace[] simliar = faceServiceClient.findSimilar(result[i].faceId, "face_list", 10);
                                    if (simliar.length == 0) {
                                        params[0].reset();
                                        faceServiceClient.AddFaceToFaceList("face_list", params[0], result[i].faceId.toString(), result[0].faceRectangle);
                                        faceId = result[i].faceId.toString();
                                    }
                                    else {
                                        faceId = simliar[0].persistedFaceId.toString();
                                    }

                                    long timestamp = new Date().getTime();

                                    FaceAttribute faceAttributes = result[i].faceAttributes;

                                    JSONObject jsonFacialHair = new JSONObject();
                                    jsonFacialHair.put("moustache", faceAttributes.facialHair.moustache);
                                    jsonFacialHair.put("beard", faceAttributes.facialHair.beard);
                                    jsonFacialHair.put("sideburns", faceAttributes.facialHair.sideburns);

                                    JSONObject jsonMakeup = new JSONObject();
                                    jsonMakeup.put("eyeMakeup", faceAttributes.makeup.eyeMakeup);
                                    jsonMakeup.put("lipMakeup", faceAttributes.makeup.lipMakeup);

                                    JSONObject jsonEmotion = new JSONObject();
                                    jsonEmotion.put("anger", faceAttributes.emotion.anger);
                                    jsonEmotion.put("contempt", faceAttributes.emotion.contempt);
                                    jsonEmotion.put("disgust", faceAttributes.emotion.disgust);
                                    jsonEmotion.put("fear", faceAttributes.emotion.fear);
                                    jsonEmotion.put("happiness", faceAttributes.emotion.happiness);
                                    jsonEmotion.put("neutral", faceAttributes.emotion.neutral);
                                    jsonEmotion.put("sadness", faceAttributes.emotion.sadness);
                                    jsonEmotion.put("surprise", faceAttributes.emotion.surprise);

                                    JSONObject jsonOcclusion = new JSONObject();
                                    jsonOcclusion.put("foreheadOccluded", faceAttributes.occlusion.foreheadOccluded);
                                    jsonOcclusion.put("eyeOccluded", faceAttributes.occlusion.eyeOccluded);
                                    jsonOcclusion.put("mouthOccluded", faceAttributes.occlusion.mouthOccluded);

                                    JSONArray jsonHairColorArr = new JSONArray();
                                    for (int j = 0; j < faceAttributes.hair.hairColor.length; j ++) {
                                        JSONObject hairColorItem = new JSONObject();
                                        hairColorItem.put("color", faceAttributes.hair.hairColor[j].color);
                                        hairColorItem.put("confidence", faceAttributes.hair.hairColor[j].confidence);
                                        jsonHairColorArr.put(hairColorItem);
                                    }



                                    JSONObject jsonHair = new JSONObject();
                                    jsonHair.put("bald", faceAttributes.hair.bald);
                                    jsonHair.put("invisible", faceAttributes.hair.invisible);
                                    jsonHair.put("hairColor", jsonHairColorArr);

                                    JSONObject jsonFaceAttribute = new JSONObject();
                                    jsonFaceAttribute.put("gender", faceAttributes.gender);
                                    jsonFaceAttribute.put("age", faceAttributes.age);
                                    jsonFaceAttribute.put("facialHair", jsonFacialHair);
                                    jsonFaceAttribute.put("glasses", faceAttributes.glasses);
                                    jsonFaceAttribute.put("makeup", jsonMakeup);
                                    jsonFaceAttribute.put("emotion", jsonEmotion);
                                    jsonFaceAttribute.put("occlusion", jsonOcclusion);
                                    jsonFaceAttribute.put("smile", faceAttributes.smile);
                                    jsonFaceAttribute.put("hair", jsonHair);

                                    try {
                                        String franposFaceEndpoint = "http://faceapi.franpos.com/api/visit";

                                        JSONObject jsonParams = new JSONObject();
                                        jsonParams.put("faceId", faceId);
                                        jsonParams.put("companyId", company_id);
                                        jsonParams.put("timestamp", timestamp);
                                        jsonParams.put("faceAttributes", jsonFaceAttribute.toString());
                                        StringEntity entity = new StringEntity(jsonParams.toString());
                                        SyncHttpClient httpClient = new SyncHttpClient();

//                                    httpClient.post(handler, franposFaceEndpoint, entity, "application/json", new TextHttpResponseHandler() {
//                                        @Override
//                                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
////                                            Toast.makeText(handler, "Failed to post API", Toast.LENGTH_LONG).show();
//                                        }
//
//                                        @Override
//                                        public void onSuccess(int statusCode, Header[] headers, String responseString) {
////                                            Toast.makeText(handler, "Save to database successfully", Toast.LENGTH_LONG).show();
//                                        }
//                                    });

                                        httpClient.post(handler, franposFaceEndpoint, entity, "application/json", new AsyncHttpResponseHandler() {
                                            @Override
                                            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
//                                            Toast.makeText(handler, "Save to database successfully", Toast.LENGTH_LONG).show();
                                            }

                                            @Override
                                            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
//                                            Toast.makeText(handler, "Failed to post API", Toast.LENGTH_LONG).show();
                                                exceptionMessage = "Failed to post API";
                                            }
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        exceptionMessage = e.getMessage();
                                    }
                                }

//                            if (needTrain){
//                                faceServiceClient.trainLargeFaceList("face_list");
//                            }

                                return result;
                            } catch (Exception e) {
                                exceptionMessage = String.format(
                                        "Detection failed: %s", e.getMessage());
                                return null;
                            }
                        }

                        @Override
                        protected void onPostExecute(Face[] result) {
                            //TODO: update face frames

                            if (result == null) return;

                            imageView.setImageBitmap(drawFaceRectanglesOnBitmap(photo, result));
                            photo.recycle();

                            if (exceptionMessage != null)
                                Toast.makeText(handler,"Save to database successfully", Toast.LENGTH_LONG).show();
                            else
                                Toast.makeText(handler,exceptionMessage, Toast.LENGTH_LONG).show();
                        }
                    };

            detectTask.execute(inputStream);
        }
    }

    private static Bitmap drawFaceRectanglesOnBitmap(
            Bitmap originalBitmap, Face[] faces) {
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        if (faces != null) {
            for (Face face : faces) {
                FaceRectangle faceRectangle = face.faceRectangle;
                canvas.drawRect(
                        faceRectangle.left,
                        faceRectangle.top,
                        faceRectangle.left + faceRectangle.width,
                        faceRectangle.top + faceRectangle.height,
                        paint);
            }
        }
        return bitmap;
    }

//    private void startDetection() {
//
//        mCamera = getCameraInstance();
//
//        if (mCamera == null) {
////            Toast.makeText(this, "Camera Instance is null", Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        mCameraPreview = new CameraPreview(this, mCamera);
//        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
//        preview.addView(mCameraPreview);
//
//        mCamera.startPreview();
//
//
////        new Timer().schedule(new TimerTask() {
////            @Override
////            public void run() {
////                mCamera.takePicture(null, null, mPicture);
////            }
////        }, 0, 3000);
//    }

//    private void requestPermissions() {
//        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.INTERNET};
//
//        ActivityCompat.requestPermissions(this, permissions, 1);
//    }
//
//    private void requestPermission() {
//        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.INTERNET}, 111);
//    }
//
//    private boolean checkPermissionIsGranted() {
//        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
//                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
//    }
//
//    @Override
//    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == 111) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startDetection();
//            } else {
//                //Toast.makeText(handler, "Please check camera permission", Toast.LENGTH_LONG).show();
//            }
//        }
//    }
//
//    protected void onResume() {
//        super.onResume();
//
//        if (checkPermissionIsGranted()) {
//            startDetection();
//        }
//    }

//    private Camera getCameraInstance() {
//        Camera camera = null;
//        try {
//            camera = Camera.open();
//        } catch (Exception e) {
//            //Toast.makeText(handler, e.getMessage(), Toast.LENGTH_LONG).show();
//            // cannot get camera or does not exist
//        }
//        return camera;
//    }

    PictureCallback mPicture = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            processTakenPhoto(data);
            camera.startPreview();
        }
    };

    private void processTakenPhoto(byte[] data) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);

        AsyncTask<InputStream, String, Face[]> detectTask =
                new AsyncTask<InputStream, String, Face[]>() {
                    String exceptionMessage = "";

                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    true,
                                    false,
                                    new FaceServiceClient.FaceAttributeType[] {
                                            FaceServiceClient.FaceAttributeType.Gender,
                                            FaceServiceClient.FaceAttributeType.Age,
                                            FaceServiceClient.FaceAttributeType.FacialHair,
                                            FaceServiceClient.FaceAttributeType.Glasses,
                                            FaceServiceClient.FaceAttributeType.Makeup,
                                            FaceServiceClient.FaceAttributeType.Emotion,
                                            FaceServiceClient.FaceAttributeType.Occlusion,
                                            FaceServiceClient.FaceAttributeType.Accessories,
                                            FaceServiceClient.FaceAttributeType.Hair,
                                            FaceServiceClient.FaceAttributeType.Smile,
                                    }
                            );
                            if (result == null || result.length == 0){
                                return null;
                            }


                            for (int i = 0; i < result.length; i ++) {
                                String faceId;
                                SimilarPersistedFace[] simliar = faceServiceClient.findSimilar(result[i].faceId, "face_list", 10);
                                if (simliar.length == 0) {
                                    params[0].reset();
                                    faceServiceClient.AddFaceToFaceList("face_list", params[0], result[i].faceId.toString(), result[0].faceRectangle);
                                    faceId = result[i].faceId.toString();
                                }
                                else {
                                    faceId = simliar[0].persistedFaceId.toString();
                                }

                                long timestamp = new Date().getTime();

                                FaceAttribute faceAttributes = result[i].faceAttributes;

                                JSONObject jsonFacialHair = new JSONObject();
                                jsonFacialHair.put("moustache", faceAttributes.facialHair.moustache);
                                jsonFacialHair.put("beard", faceAttributes.facialHair.beard);
                                jsonFacialHair.put("sideburns", faceAttributes.facialHair.sideburns);

                                JSONObject jsonMakeup = new JSONObject();
                                jsonMakeup.put("eyeMakeup", faceAttributes.makeup.eyeMakeup);
                                jsonMakeup.put("lipMakeup", faceAttributes.makeup.lipMakeup);

                                JSONObject jsonEmotion = new JSONObject();
                                jsonEmotion.put("anger", faceAttributes.emotion.anger);
                                jsonEmotion.put("contempt", faceAttributes.emotion.contempt);
                                jsonEmotion.put("disgust", faceAttributes.emotion.disgust);
                                jsonEmotion.put("fear", faceAttributes.emotion.fear);
                                jsonEmotion.put("happiness", faceAttributes.emotion.happiness);
                                jsonEmotion.put("neutral", faceAttributes.emotion.neutral);
                                jsonEmotion.put("sadness", faceAttributes.emotion.sadness);
                                jsonEmotion.put("surprise", faceAttributes.emotion.surprise);

                                JSONObject jsonOcclusion = new JSONObject();
                                jsonOcclusion.put("foreheadOccluded", faceAttributes.occlusion.foreheadOccluded);
                                jsonOcclusion.put("eyeOccluded", faceAttributes.occlusion.eyeOccluded);
                                jsonOcclusion.put("mouthOccluded", faceAttributes.occlusion.mouthOccluded);

                                JSONArray jsonHairColorArr = new JSONArray();
                                for (int j = 0; j < faceAttributes.hair.hairColor.length; j ++) {
                                    JSONObject hairColorItem = new JSONObject();
                                    hairColorItem.put("color", faceAttributes.hair.hairColor[j].color);
                                    hairColorItem.put("confidence", faceAttributes.hair.hairColor[j].confidence);
                                    jsonHairColorArr.put(hairColorItem);
                                }



                                JSONObject jsonHair = new JSONObject();
                                jsonHair.put("bald", faceAttributes.hair.bald);
                                jsonHair.put("invisible", faceAttributes.hair.invisible);
                                jsonHair.put("hairColor", jsonHairColorArr);

                                JSONObject jsonFaceAttribute = new JSONObject();
                                jsonFaceAttribute.put("gender", faceAttributes.gender);
                                jsonFaceAttribute.put("age", faceAttributes.age);
                                jsonFaceAttribute.put("facialHair", jsonFacialHair);
                                jsonFaceAttribute.put("glasses", faceAttributes.glasses);
                                jsonFaceAttribute.put("makeup", jsonMakeup);
                                jsonFaceAttribute.put("emotion", jsonEmotion);
                                jsonFaceAttribute.put("occlusion", jsonOcclusion);
                                jsonFaceAttribute.put("smile", faceAttributes.smile);
                                jsonFaceAttribute.put("hair", jsonHair);

                                try {
                                    String franposFaceEndpoint = "http://faceapi.franpos.com/api/visit";

                                    JSONObject jsonParams = new JSONObject();
                                    jsonParams.put("faceId", faceId);
                                    jsonParams.put("companyId", company_id);
                                    jsonParams.put("timestamp", timestamp);
                                    jsonParams.put("faceAttributes", jsonFaceAttribute.toString());
                                    StringEntity entity = new StringEntity(jsonParams.toString());
                                    SyncHttpClient httpClient = new SyncHttpClient();

//                                    httpClient.post(handler, franposFaceEndpoint, entity, "application/json", new TextHttpResponseHandler() {
//                                        @Override
//                                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
////                                            Toast.makeText(handler, "Failed to post API", Toast.LENGTH_LONG).show();
//                                        }
//
//                                        @Override
//                                        public void onSuccess(int statusCode, Header[] headers, String responseString) {
////                                            Toast.makeText(handler, "Save to database successfully", Toast.LENGTH_LONG).show();
//                                        }
//                                    });

                                    httpClient.post(handler, franposFaceEndpoint, entity, "application/json", new AsyncHttpResponseHandler() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
//                                            Toast.makeText(handler, "Save to database successfully", Toast.LENGTH_LONG).show();
                                        }

                                        @Override
                                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
//                                            Toast.makeText(handler, "Failed to post API", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(handler, e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }

//                            if (needTrain){
//                                faceServiceClient.trainLargeFaceList("face_list");
//                            }

                            return result;
                        } catch (Exception e) {
                            exceptionMessage = String.format(
                                    "Detection failed: %s", e.getMessage());
                            return null;
                        }
                    }
                };

        detectTask.execute(inputStream);

        Toast.makeText(handler, "Save to database successfully", Toast.LENGTH_LONG).show();
    }

}

class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {

        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){

        }
    }
}