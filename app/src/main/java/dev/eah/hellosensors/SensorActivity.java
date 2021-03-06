package dev.eah.hellosensors;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class SensorActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mSensorGyro;
    private Sensor mSensorLinAccel;
    private Sensor mSensorAccel;
    private Sensor mSensorMagneto;
    private Vibrator vibrator;

    private float currentDeg = 0f;
    private float[] accelArr = new float[3];
    private float[] magnetoArr = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];
    private boolean hasAccelArr = false;
    private boolean hasMagnetoArr = false;

    private ImageView imgCompass;
    private TextView txtGyroStatus;
    private TextView txtAccelStatus;
    private TextView txtMagneto;
    private TextView txtPitch;
    private TextView txtXRads;
    private TextView txtYRads;
    private TextView txtZRads;
    private TextView txtXForce;
    private TextView txtYForce;
    private TextView txtZForce;

    private final static float LIN_ACCEL_DELTA = 1.5f;
    private final static int LIN_SENSOR_DELAY = 250000;
    private final static float PITCH_DEG_DELTA = 10f;
    private final static int PERMISSIONS_ID = 1337;
    private final static float LOWPASS_ALPHA = 0.10f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        txtGyroStatus = (TextView) findViewById(R.id.txtGyroStatus);
        txtAccelStatus = (TextView) findViewById(R.id.txtAccelStatus);
        imgCompass = (ImageView) findViewById(R.id.imgCompass);
        txtMagneto = (TextView) findViewById(R.id.txtMagneto);
        txtPitch = (TextView) findViewById(R.id.txtPitch);
        txtXRads = (TextView) findViewById(R.id.txtXRads);
        txtYRads = (TextView) findViewById(R.id.txtYRads);
        txtZRads = (TextView) findViewById(R.id.txtZRads);
        txtXForce = (TextView) findViewById(R.id.txtXForce);
        txtYForce = (TextView) findViewById(R.id.txtYForce);
        txtZForce = (TextView) findViewById(R.id.txtZForce);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorLinAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorMagneto = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        mSensorManager.registerListener(this, mSensorGyro, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorLinAccel, LIN_SENSOR_DELAY);
        mSensorManager.registerListener(this, mSensorAccel, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorMagneto, SensorManager.SENSOR_DELAY_GAME);

        if (checkSelfPermission(Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            // need to get permission

            requestPermissions(new String[]{Manifest.permission.VIBRATE}, PERMISSIONS_ID);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorGyro, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorLinAccel, LIN_SENSOR_DELAY);
        mSensorManager.registerListener(this, mSensorAccel, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorMagneto, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mSensorGyro) {
            float xRads = event.values[0];
            float yRads = event.values[1];
            float zRads = event.values[2];

            txtXRads.setText("" + xRads);
            txtYRads.setText("" + yRads);
            txtZRads.setText("" + zRads);

            if (Math.abs(zRads) > 3) {
                txtGyroStatus.setText("You are spinning insanely fast!");
            } else if (Math.abs(zRads) > 1) {
                txtGyroStatus.setText("You are spinning!");
            } else {
                txtGyroStatus.setText("Still.");
            }
        } else if (event.sensor == mSensorLinAccel) {
            float xForce = event.values[0];
            float yForce = event.values[1];
            float zForce = event.values[2];

            txtXForce.setText("" + xForce);
            txtYForce.setText("" + yForce);
            txtZForce.setText("" + zForce);

            if (zForce < -LIN_ACCEL_DELTA) {
                txtAccelStatus.setText("Falling!");
            } else if (zForce > LIN_ACCEL_DELTA) {
                txtAccelStatus.setText("Elevating!");
            } else {
                txtAccelStatus.setText("Still.");
            }
        } else if (event.sensor == mSensorMagneto) {
            txtMagneto.setText(event.values[0] + "\n" +
                    event.values[1] + "\n" +
                    event.values[2]);

            magnetoArr = lowPass(event.values.clone(), magnetoArr);

            // System.arraycopy(event.values, 0, magnetoArr, 0, event.values.length);
            hasMagnetoArr = true;
        } else if (event.sensor == mSensorAccel) {
            accelArr = lowPass(event.values.clone(), accelArr);

            //System.arraycopy(event.values, 0, accelArr, 0, event.values.length);
            hasAccelArr = true;
        }

        if (hasAccelArr && hasMagnetoArr) {
            SensorManager.getRotationMatrix(rotationMatrix, null, accelArr, magnetoArr);
            SensorManager.getOrientation(rotationMatrix, orientation);
            float azimuthInRadians = orientation[0];
            float azimuthInDegress = (float)(Math.toDegrees(azimuthInRadians)+360)%360;
            RotateAnimation ra = new RotateAnimation(
                    currentDeg,
                    -azimuthInDegress,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);

            ra.setDuration(250);

            ra.setFillAfter(true);

            imgCompass.startAnimation(ra);
            currentDeg = -azimuthInDegress;

            float pitch = orientation[1] * 180f / (float) Math.PI;
            if (pitch > PITCH_DEG_DELTA) {
                txtPitch.setText("Front tilt: " + pitch + " degrees");
            } else if(pitch < -PITCH_DEG_DELTA) {
                txtPitch.setText("Back tilt: " + pitch + " degrees");
            } else {
                txtPitch.setText("No tilt.");
            }

            if (checkSelfPermission(Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
                if (-currentDeg < 15 || -currentDeg > 345) {
                    vibrator.vibrate(500);
                } else if (-currentDeg < 45 || -currentDeg > 315) {
                    vibrator.vibrate(2);
                }
            }
        }
    }

    private float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;

        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + LOWPASS_ALPHA * (input[i] - output[i]);
        }

        return output;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
