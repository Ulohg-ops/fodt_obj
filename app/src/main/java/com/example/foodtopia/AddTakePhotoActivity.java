package com.example.foodtopia;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.foodtopia.add.Upload;
import com.example.foodtopia.databinding.ActivityAddTakePhotoBinding;
import com.example.foodtopia.ml.Food101ModelUnquant;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

public class AddTakePhotoActivity extends AppCompatActivity {

    ActivityAddTakePhotoBinding binding;

    public static final String TAG = MainActivity.class.getSimpleName()+"My";

    Uri uri ;
    String imgURL;
    String mealtime;
    ImageView imageView;
    ProgressDialog progressDialog;
    StorageReference storageRef;
    private DatabaseReference mDatabase;

    private String mPath = "";//??????????????????????????????
    public static final int CAMERA_PERMISSION = 100;//?????????????????????
    public static final int REQUEST_HIGH_IMAGE = 101;//??????????????????

    private final int imageSize = 224;

    @SuppressLint("QueryPermissionsNeeded")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddTakePhotoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imageView = findViewById(R.id.cameraImageView);

        Intent intent=getIntent();
        mealtime = intent.getStringExtra("choice");

        /*??????????????????*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION);
        }

        /*??????????????????*/
        binding.buttonHigh.setOnClickListener(v->{
            Intent highIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //???????????????????????????
            if (highIntent.resolveActivity(getPackageManager()) == null) return;
            //?????????????????????URI???????????????????????????
            File imageFile = getImageFile();
            if (imageFile == null) return;
            //?????????????????????URI??????
            uri = FileProvider.getUriForFile(
                    this,
                    "com.example.foodtopia.CameraEx",//????????????AndroidManifest.xml??????authorities ??????
                    imageFile
            );
            binding.cameraImageView.setImageURI(uri);
            highIntent.putExtra(MediaStore.EXTRA_OUTPUT,uri);
            startActivityForResult(highIntent,REQUEST_HIGH_IMAGE);//????????????
        });

        /*??????????????????*/
        binding.cameraPhotoUploadBtn.setOnClickListener(view -> {
            uploadImage();
//                Toast.makeText(TakePhoto.this,"????????????",Toast.LENGTH_SHORT).show();
        });
        /*??????????????????*/
        binding.cameraBackFab.setOnClickListener(view -> {
            Intent intent1 = new Intent(AddTakePhotoActivity.this, MainActivity.class);
            startActivity(intent1);
        });
    }

    private void uploadImage() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("?????????...");
        progressDialog.show();

        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.TAIWAN);
        String date = formatter.format(now);
        SimpleDateFormat timeFormatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.TAIWAN);
        String time = timeFormatter.format(now);

        storageRef = FirebaseStorage.getInstance().getReference("meals");
        final StorageReference fileReference = storageRef.child(time
                + "." + getFileExtension(uri));
        UploadTask uploadTask = fileReference.putFile(uri);

        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            return fileReference.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                imgURL = downloadUri.toString();

                String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

                mDatabase = FirebaseDatabase.getInstance().getReference("uploads");
                //new node
                String uploadID = mDatabase.push().getKey();
                Upload photo = new Upload(uid, date, mealtime, imgURL);

                assert uploadID != null;
                mDatabase.child(uploadID).setValue(photo);

                if (progressDialog.isShowing()){
                    progressDialog.dismiss();
                }

            } else {
                Toast.makeText(AddTakePhotoActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(AddTakePhotoActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
        Bitmap imageBitmap = BitmapFactory.decodeFile(mPath);
        imageBitmap = Bitmap.createScaledBitmap(imageBitmap,imageSize,imageSize,false);
        classifyImage(imageBitmap);
    }

    public void classifyImage(Bitmap image){
        try {
            //TODO Change Model
            Food101ModelUnquant model = Food101ModelUnquant.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            // get 1D array of 224 * 224 pixels in image
            int [] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

            // iterate over pixels and extract R, G, and B values. Add to bytebuffer.
            int pixel = 0;
            for(int i = 0; i < imageSize; i++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Food101ModelUnquant.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();

            TextView result = findViewById(R.id.textView_taking_result);
            // TODO Adjust label
            //label
            String[] classes = {"apple pie", "baby back ribs", "baklava", "beef carpaccio", "beef tartare",
                    "beet salad", "beignets","bibimbap"};
            result.setText("????????????:");

            TreeMap<Float, String> confidenceMap = new TreeMap<>();
            for(int i = 0; i < classes.length; i++){
                confidenceMap.put(confidences[i] * 100,classes[i]);
            }
//confidence
            List<Float> keyList = new ArrayList<>(confidenceMap.keySet());
            //label classes
            List<String> valueList = new ArrayList<>(confidenceMap.values());

            Button predict1 = findViewById(R.id.btn_taking_predict1);
            Button predict2 = findViewById(R.id.btn_taking_predict2);
            Button predict3 = findViewById(R.id.btn_taking_predict3);
            predict1.setText(1+". "+valueList.get(valueList.size()-1)+
                    ", Confidence: "+String.format("%.1f%%",keyList.get(keyList.size()-1)));
            predict2.setText(2+". "+valueList.get(valueList.size()-2)+
                    ", Confidence: "+String.format("%.1f%%",keyList.get(keyList.size()-2)));
            predict3.setText(3+". "+valueList.get(valueList.size()-3)+
                    ", Confidence: "+String.format("%.1f%%",keyList.get(keyList.size()-3)));

            predict1.setOnClickListener(view -> {
                Toast.makeText(this,valueList.get(valueList.size()-1),Toast.LENGTH_SHORT).show();
            });
            predict2.setOnClickListener(view -> {
                Toast.makeText(this,valueList.get(valueList.size()-2),Toast.LENGTH_SHORT).show();
            });
            predict3.setOnClickListener(view -> {
                Toast.makeText(this,valueList.get(valueList.size()-3),Toast.LENGTH_SHORT).show();
            });

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {

        }
    }

    //???????????????
    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    /**?????????????????????URI???????????????????????????*/
    private File getImageFile() {
        String time = new SimpleDateFormat("yyMMdd").format(new Date());
        String fileName = time+"_";
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            //?????????????????????????????????
            File imageFile = File.createTempFile(fileName,".jpg",dir);
            //???????????????????????????????????????????????????????????????
            mPath = imageFile.getAbsolutePath();
            return imageFile;
        } catch (IOException e) {
            return null;
        }
    }

    /*??????????????????*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /*???????????????????????????????????????requestCode?????????????????????resultCode???-1??????????????????0????????????????????????*/
        Log.d(TAG, "onActivityResult: requestCode: "+requestCode+", resultCode "+resultCode);

        /*?????????????????????????????????*/
        if (requestCode == REQUEST_HIGH_IMAGE && resultCode == -1){
            ImageView imageHigh = binding.cameraImageView;
            new Thread(()->{
                //???BitmapFactory????????????URI???????????????????????????????????????AtomicReference<Bitmap>???????????????????????????
                AtomicReference<Bitmap> getHighImage = new AtomicReference<>(BitmapFactory.decodeFile(mPath));
                Matrix matrix = new Matrix();
//                matrix.setRotate(90f);//???90???
                getHighImage.set(Bitmap.createBitmap(getHighImage.get()
                        ,0,0
                        ,getHighImage.get().getWidth()
                        ,getHighImage.get().getHeight()
                        ,matrix,true));
                runOnUiThread(()->{
                    //???Glide????????????(?????????????????????????????????????????????LAG????????????????????????Thread?????????)
                    Glide.with(this)
                            .load(getHighImage.get())
                            .centerCrop()
                            .into(imageHigh);
                });
            }).start();
        }
        else{
            Toast.makeText(this, "??????????????????", Toast.LENGTH_SHORT).show();
        }

    }
}