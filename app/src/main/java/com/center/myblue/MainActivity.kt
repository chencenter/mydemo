package com.center.myblue

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    var mBluetoothDevice = null
//    var devices: Set<BluetoothDevice>? = null
    var devices = mutableSetOf<BluetoothDevice>()
    var localDevices = mutableSetOf<BluetoothDevice>()
//    var arrayAdapter: ArrayAdapter<String>? = null
    var list = mutableListOf<String>()
    var progressDialog: ProgressDialog? = null
    private val SEARCH_CODE = 0x123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.e("center", "onCreate: GPS是否可用：" + isGpsEnable(this))
    }

    fun openblus(view : View){
        if (!mBluetoothAdapter.isEnabled){
            mBluetoothAdapter.enable()
        }else{
            Toast.makeText(this,"蓝牙设备已经打开",Toast.LENGTH_SHORT).show()
        }
    }

    fun closeblus(view: View){
        if(mBluetoothAdapter.isEnabled){
            mBluetoothAdapter.disable()
        }else{
            Toast.makeText(this,"蓝牙设备已经关闭",Toast.LENGTH_SHORT).show()
        }
    }

    fun getBlusData(view: View){
        if (mBluetoothAdapter.isEnabled){
            localDevices.clear()
            localDevices = mBluetoothAdapter.bondedDevices//得到本地蓝牙集合
            Log.e("center","devices:"+devices)
            list.clear()
            localDevices.forEach {
                show_tv.text = "名字:"+it.name+"地址:"+it.address
                Log.e("center","名字:"+it.name+"地址:"+it.address)
                list.add("名字:"+it.name+"地址:"+it.address)
            }
            setAdapter()
        }else{
            Toast.makeText(this,"蓝牙设备已经关闭",Toast.LENGTH_SHORT).show()
        }
    }

    fun searchBlus(view: View){
//        //可被搜索
//        if (mBluetoothAdapter.isEnabled){
//            var intent:Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)//请求搜索
//            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,120)//可被搜索120秒
//            startActivity(intent)//开始搜索
//        }
        // 判断是否打开蓝牙
        if (!mBluetoothAdapter.isEnabled){
            //弹出对话框提示用户是后打开
            var intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent,SEARCH_CODE)
        }else{
            // 不做提示，强行打开
            mBluetoothAdapter.enable()
        }
        startDiscovery()
    }

    fun isGpsEnable(context: Context): Boolean{
        var locationManager:LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gps:Boolean = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        var network:Boolean = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (gps || network){
            return true
        }
        return false
    }


    /**
     * 注册异步搜索蓝牙设备的广播
     */
    fun startDiscovery(){
        //找到广播设备
        var filter:IntentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        //注册广播
        registerReceiver(receiver,filter)
        //搜索玩广播
        var filter1:IntentFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        //注册广播
        registerReceiver(receiver,filter1)
        startScanBluth()
    }

    /**
     * 广播接收器
     */
    var receiver  = object:BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            //搜到的广播类型
            var action = intent!!.action
            //发现设备的广播
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                //从intent中获取设备
                var device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                //没否配对
                if (device.bondState != BluetoothDevice.BOND_BONDED){
                    if (!devices!!.contains(device)){
                        devices.add(device)
                    }
                    show_tv.text = "附近设备:"+devices.size+"个\\u3000\\u3000本机蓝牙地址："+getBluetoothAddress()
                    list.clear()
                    devices.forEach {
                        show_tv.text = "名字:"+it.name+"地址:"+it.address
                        Log.e("center","名字:"+it.name+"地址:"+it.address)
                        list.add("名字:"+it.name+"地址:"+it.address)
                    }
                    setAdapter()
                }
            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                progressDialog!!.dismiss()
            }
        }
    }
    /**
     * 搜索蓝牙的方法
     */
    fun startScanBluth(){
        //判断是否搜索，如果搜索，就取消搜索
        if (mBluetoothAdapter.isDiscovering){
            mBluetoothAdapter.cancelDiscovery()
        }
        //开始搜索
        mBluetoothAdapter.startDiscovery()
        if (progressDialog == null){
            progressDialog = ProgressDialog(this)
        }
        progressDialog!!.setMessage("正在搜索，请稍后!")
        progressDialog!!.show()
    }

    /**
     * 获取本机蓝牙地址
     */
    fun getBluetoothAddress():String?{
        try {
            var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            var field = bluetoothAdapter.javaClass.getDeclaredField("mService")
            field.isAccessible = true
            var bluetoothManagerService = field.get(bluetoothAdapter)
            if (bluetoothManagerService == null){
                return null
            }
            var method = bluetoothManagerService.javaClass.getMethod("getAddress")
            var address = method.invoke(bluetoothManagerService)
            if (address != null){
                return address.toString()
            }else{
                return null
            }
        }catch (e:Exception){}
        return null
    }

    fun setAdapter(){
        var arrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, list)
        listview.adapter = arrayAdapter
        arrayAdapter!!.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (receiver != null){
            unregisterReceiver(receiver)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SEARCH_CODE ){
            startDiscovery()
        }
    }
}
