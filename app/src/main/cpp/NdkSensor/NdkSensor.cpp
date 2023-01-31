#include "NdkSensor.h"
#include <android/log.h>

const int kLooperId = 3;
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, "NdkSensor", __VA_ARGS__)

/** data can be passed from ASensorManager_createEventQueue() */
static int onSensorChanged(int fd, int events, void* data){
    auto *mSensorManager = (NdkSensorManager *)data;
    ASensorEvent event;
    while (ASensorEventQueue_getEvents(mSensorManager->_sensorEventQueue, &event, 1) > 0){
        mSensorManager->_listener->onSensorChanged(&event);
    }
    return 1;
}

NdkSensorManager::NdkSensorManager(){
    _sensorManager = ASensorManager_getInstance();
    _ndkSensorLooper = ALooper_forThread();
}
//注册传感器
void NdkSensorManager::registerSensor(const vector<int> &sensorIDs, int32_t usec) {
    //创建事件队列
    _sensorEventQueue = ASensorManager_createEventQueue(_sensorManager, _ndkSensorLooper, kLooperId,
                                                        onSensorChanged, this);
    for(auto type: sensorIDs){
        //获得传感器管理器
        auto *sensor = const_cast<ASensor *>(ASensorManager_getDefaultSensor(_sensorManager, type));
        if(sensor != nullptr){
            //使用默认频率启用传感器，并指定事件队列
            auto status = ASensorEventQueue_enableSensor(_sensorEventQueue, sensor);
            assert(status >= 0);
            //设置采样频率
            status = ASensorEventQueue_setEventRate(_sensorEventQueue, sensor, usec);
            assert(status >= 0);
            //在数组的后面添加传感器
            enable_sensor.emplace_back(type, sensor);
            LOGI("Register: %d", type);
        } else {
            LOGI("Sensor id : %d == null", type);
        }
    }

}

void NdkSensorManager::unregister() {
    for(auto sensor : enable_sensor){
        if(sensor.second != nullptr && _sensorEventQueue != nullptr){
            ASensorEventQueue_disableSensor(_sensorEventQueue, sensor.second);
            sensor.second = NULL;
        }
    }
    enable_sensor.clear();
}

