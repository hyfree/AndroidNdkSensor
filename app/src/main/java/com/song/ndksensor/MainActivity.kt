package com.song.ndksensor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract.Directory
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File



class MainActivity : AppCompatActivity() {

    private lateinit var ndkSensor_ACCELEROMETER: NdkSensor//加速度传感器
    private var ACCELEROMETER_Count:Int=0
    private var GYROSCOPE_Count:Int=0
    private var MAGNETIC_FIELD_Count:Int=0
    //private val Max_Count=1_000_000;
    private val Max_Count=8_000_000;
    private val path="/storage/emulated/0/1/2023/";
    val   pathSet=HashSet<String>();
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ACCELEROMETER_Count=0;
        GYROSCOPE_Count=0;
        MAGNETIC_FIELD_Count=0;
        //界面文本
        val text_ACCELEROMETER = findViewById<TextView>(R.id.sample_text_ACCELEROMETER)
        val text_GYROSCOPE = findViewById<TextView>(R.id.sample_text_GYROSCOPE)
        val text_MAGNETIC_FIELD = findViewById<TextView>(R.id.sample_text_MAGNETIC_FIELD)

        //实例化传感器
        ndkSensor_ACCELEROMETER = NdkSensor();


        //注册加速度监听器
        ndkSensor_ACCELEROMETER.registerSensor(
            intArrayOf(
                Sensor.TYPE_ACCELEROMETER,
                Sensor.TYPE_GYROSCOPE,
                Sensor.TYPE_MAGNETIC_FIELD
            ),
            20_000,//50Hz
            object : NdkSensor.NdkSensorListener {
                @SuppressLint("SetTextI18n")
                override fun onSensorChanged(event: NdkSensorEvent) {
                    var x=event.value!!.get(0);
                    var y=event.value!!.get(1);
                    var z=event.value!!.get(2);
                    var xBytes = float2byte(event.value!!.get(0))
                    var yBytes = float2byte(event.value!!.get(1))
                    var zBytes = float2byte(event.value!!.get(2))
                    var xHex = HexUtil.byteArrayToHex(xBytes);
                    var yHex = HexUtil.byteArrayToHex(yBytes);
                    var zHex = HexUtil.byteArrayToHex(zBytes);

                    when (event.type) {
                        Sensor.TYPE_ACCELEROMETER -> {
                            var message:String;
                            if (ACCELEROMETER_Count<Max_Count){
                                writeFile("加速度传感器","x",xBytes!!);
                                writeFile("加速度传感器","y",yBytes!!);
                                writeFile("加速度传感器","z",zBytes!!);
                                ACCELEROMETER_Count++;
                                message=(ACCELEROMETER_Count.toFloat()/Max_Count).toString()
                                text_ACCELEROMETER.text =
                                    "type:加速度传感器\n ts: ${event.timestamp}\n, " +
                                            "x=${x.toString()},\ny=${y.toString()}\nz=${z.toString()}\n${message}\n"+
                                            "已经采集：${ACCELEROMETER_Count}\n"+
                                            "剩余采集：${Max_Count-ACCELEROMETER_Count}\n"
                            }else{
                                text_ACCELEROMETER.text ="加速度传感器数据--采集完成";
                            }

                        }

                        Sensor.TYPE_GYROSCOPE -> {
                            var message:String;
                            if (GYROSCOPE_Count<Max_Count) {
                                writeFile("陀螺仪","x",xBytes!!);
                                writeFile("陀螺仪","y",yBytes!!);
                                writeFile("陀螺仪","z",zBytes!!);
                                GYROSCOPE_Count++;
                                message=(GYROSCOPE_Count.toFloat()/Max_Count).toString()
                                text_GYROSCOPE.text ="type: 陀螺仪\n ts: ${event.timestamp}\n, " +
                                        "x=${x.toString()},\ny=${y.toString()}\nz=${z.toString()}\n${message}\n"+
                                        "已经采集：${GYROSCOPE_Count}\n"+
                                        "剩余采集：${Max_Count-GYROSCOPE_Count}\n"
                            }else{
                                text_GYROSCOPE.text ="陀螺仪数据--采集完成";
                            }


                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            var message:String;
                            if (MAGNETIC_FIELD_Count<Max_Count) {
                                writeFile("磁场传感器","x",xBytes!!);
                                writeFile("磁场传感器","y",yBytes!!);
                                writeFile("磁场传感器","z",zBytes!!);
                                MAGNETIC_FIELD_Count++;
                                message=(MAGNETIC_FIELD_Count.toFloat()/Max_Count).toString()

                                text_MAGNETIC_FIELD.text =
                                    "type: 磁场传感器\n ts: ${event.timestamp}\n, " +
                                            "x=${x.toString()},\ny=${y.toString()}\nz=${z.toString()}\n${message}\n"+
                                            "已经采集：${MAGNETIC_FIELD_Count}\n"+
                                            "剩余采集：${Max_Count-MAGNETIC_FIELD_Count}"
                            }else{
                                message="磁场数据--采集完成\n";
                                text_MAGNETIC_FIELD.text =message;
                            }

                        }
                    }

                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        ndkSensor_ACCELEROMETER.unregister()
    }
//    fun writeFile(sensorName:String, vector:String, data:ByteArray) {
//            File("${sensorName}-${vector}-0.bin").writeBytes(byteArrayOf(data[0]));
//            File("${sensorName}-${vector}-1.bin").writeBytes(byteArrayOf(data[1]));
//            File("${sensorName}-${vector}-2.bin").writeBytes(byteArrayOf(data[2]));
//            File("${sensorName}-${vector}-3.bin").writeBytes(byteArrayOf(data[3]));
//    }

    fun writeFile(sensorName:String, vector:String, data:ByteArray) {
        var filePath="${path}${sensorName}-${vector}.bin";
        if (!File(path).exists()){
            File(path).mkdir();
        }
        if (!File(filePath).exists()){
            File(filePath).createNewFile();
        }
        if (!pathSet.contains(filePath)){
            if (File(filePath).exists()){
                File(filePath).delete();
                File(filePath).createNewFile();
            }
            pathSet.add(filePath);
        }
        File("${path}${sensorName}-${vector}.bin").appendBytes(data);

    }

    private fun requestPermission() {
//        val permission = Manifest.permission.READ_EXTERNAL_STORAGE;
//        val checkSelfPermission = ActivityCompat.checkSelfPermission(context,permission)
//        if (checkSelfPermission  == PackageManager.PERMISSION_GRANTED) {
//
//
//        }else{
//
//
//            val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)
//            requestPermissions(permissions,1)
//        }


    }





    /**
     * 浮点转换为字节
     * @param f
     * @return
     */
    fun float2byte(f: Float): ByteArray? {
        // 把float转换为byte[]
        val fbit = java.lang.Float.floatToIntBits(f)
        val b = ByteArray(4)
        for (i in 0..3) {
            b[i] = (fbit shr 24 - i * 8).toByte()
        }

        // 翻转数组
        val len = b.size
        // 建立一个与源数组元素类型相同的数组
        val dest = ByteArray(len)
        // 为了防止修改源数组，将源数组拷贝一份副本
        System.arraycopy(b, 0, dest, 0, len)
        var temp: Byte;
        // 将顺位第i个与倒数第i个交换
        for (i in 0 until (len / 2)) {
            temp = dest[i]
            dest[i] = dest[len - i - 1]
            dest[len - i - 1] = temp
        }

        return dest
    }
}