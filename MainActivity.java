package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.Manifest;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MainActivity extends CameraActivity {

    CameraBridgeViewBase cameraBridgeViewBase;
    private static final int REQUEST_CODE = 1;
    Bitmap bitmap;
    Mat mat;
    Mat processMat;
    int thresholdValue;
    SeekBar seekBar;
    Button button;
    Button button2;
    Button button3;
    int start = 0;
    Boolean Real = true;
    Boolean Binary = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Cập nhật giá trị threshold khi thanh kéo thay đổi
                thresholdValue = progress;
                // Ví dụ: hiển thị giá trị threshold trong Toast
//                Toast.makeText(MainActivity.this, "Threshold: " + thresholdValue, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Không cần xử lý
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Không cần xử lý
            }
        });
        button = findViewById(R.id.button);
        pickImage();
        getPermission();
        cameraBridgeViewBase = findViewById(R.id.cameraView);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {

            }
            @Override
            public void onCameraViewStopped() {
            }
            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                Mat cam = inputFrame.rgba();
                Mat result = new Mat();
                Mat resizedMat2 = new Mat();
                Imgproc.resize(processMat, resizedMat2, cam.size());
//                Core.addWeighted(cam, 0.5, resizedMat2, 0.5, 2.0, result);
//                Core.add(cam,resizedMat2,result);
                if (Binary){
                Core.bitwise_and(cam,resizedMat2,result);}
                else if (Real) {
                    Core.addWeighted(cam, 1-(float)thresholdValue/255.0, resizedMat2, (float)thresholdValue/255.0, 2.0, result);
                } else{
//                Core.bitwise_or(cam,resizedMat2,result);
                Core.absdiff(cam,resizedMat2,result);}
                return result;
            }
        });
        OpenCVLoader.initDebug();
//        if (OpenCVLoader.initDebug()) {
//            cameraBridgeViewBase.enableView();
//        }
    }
    @Override
    protected void onResume(){
        super.onResume();
        cameraBridgeViewBase.enableView();
    }
    protected void onDestroy(){
        super.onDestroy();
        cameraBridgeViewBase.disableView();
    }
    @Override
    protected void onPause(){
        super.onPause();
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }


    void getPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            getPermission();
        }
    }
    public void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            // Xử lý ảnh được chọn ở đây
            try {
                ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(selectedImageUri, "r");
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                mat = new Mat();
                Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                Utils.bitmapToMat(bmp32, mat);
//                mat = convertToGray(mat,100);
                processMat = mat;
                parcelFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    public Mat convertToGray(Mat rgbImage,int threshold) {
        // Tạo một đối tượng Mat để lưu trữ ảnh nhị phân
        Mat binary = new Mat();
        rgbImage.copyTo(binary);
        // Lấy kích thước của ảnh màu
        int rows = rgbImage.rows();
        int cols = rgbImage.cols();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double[] pixel = rgbImage.get(i, j);
                double grayValue = (pixel[0] + pixel[1] + pixel[2]) / 3.0;
                double roundedValue = Math.floor(grayValue * 10) / 10;
                if(roundedValue>threshold){roundedValue = 255.0;}
                else{roundedValue = 0;}
                double[] array = {roundedValue, roundedValue, roundedValue, 255.0};
                binary.put(i, j, array);
            }
        }
//        Toast.makeText(getApplicationContext(), String.format("Rows: %d, Cols: %d, dims: %d", rows, cols,dims), Toast.LENGTH_SHORT).show();
//        showToastWithDoubleArray(getApplicationContext(),cl);
        return binary;
    }
    public void showToastWithDoubleArray(Context context, double[] array) {
        StringBuilder message = new StringBuilder("Array values: ");
        for (double value : array) {
            message.append(value).append(", ");
        }
        // Xóa dấu phẩy và khoảng trắng cuối cùng
        if (array.length > 0) {
            message.setLength(message.length() - 2);
        }
        Toast.makeText(context, message.toString(), Toast.LENGTH_SHORT).show();
    }
    public void change(View v) {
        // Xử lý khi nút được nhấn
        processMat = convertToGray(mat,thresholdValue);
        Binary = true;
        Real = false;
//                Toast.makeText(MainActivity.this, "Button clicked", Toast.LENGTH_SHORT).show();
    }
    public Mat convertToGrayWithEdges(Mat rgbImage, int threshold) {
        // Tạo một ảnh xám từ ảnh màu
        Mat grayImage = new Mat();
        Imgproc.cvtColor(rgbImage, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Áp dụng Canny edge detection để tìm các đường biên
        Mat edges = new Mat();
        Imgproc.Canny(grayImage, edges, threshold, threshold * 2);

        // Tạo ảnh mới để lưu trữ kết quả
        Mat result = new Mat();

        // Kết hợp ảnh xám với ảnh đường biên để tạo ra ảnh mới có đường biên và màu sắc
        Core.bitwise_or(grayImage, edges, result);

        Mat edge = new Mat();
        rgbImage.copyTo(edge);

        int rows = edges.rows();
        int cols = edges.cols();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double[] pixel = edges.get(i, j);
                double[] array = {pixel[0], pixel[0], pixel[0], 255.0};
                edge.put(i, j, array);
            }
        }
//        double[] pixel = result.get(50, 50);
//        showToastWithDoubleArray(getApplicationContext(),pixel);

        return edge;
    }
    public void edges(View v){
        processMat = convertToGrayWithEdges(mat,thresholdValue);
        Binary = false;
        Real = false;
    }
    public Mat processImage(Mat inputImage, int startValue, int range) {
        // Chuyển đổi ảnh sang ảnh xám
        Mat grayImage = new Mat();
        Imgproc.cvtColor(inputImage, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Tạo ảnh nhị phân từ ảnh xám dựa trên các giá trị bắt đầu và khoảng
        Mat binaryImage = new Mat();
        Core.inRange(grayImage, new Scalar(startValue), new Scalar(startValue + range), binaryImage);
        Core.bitwise_not(binaryImage, binaryImage);
        Mat step = new Mat();
        inputImage.copyTo(step);

        int rows = binaryImage.rows();
        int cols = binaryImage.cols();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double[] pixel = binaryImage.get(i, j);
                double[] array = {pixel[0], pixel[0], pixel[0], 255.0};
                step.put(i, j, array);
            }
        }
        return step;
    }
    public void steps(View v){
        processMat = processImage(mat,start,thresholdValue);
        start = start + thresholdValue;
        if(start>255){start = 0;}
        Binary = true;
        Real = false;
    }
    public void reals(View v){
        processMat = mat;
        Real = true;
        Binary = false;
    }
    public  void rssteps(View v){
        start = 0;
    }
}