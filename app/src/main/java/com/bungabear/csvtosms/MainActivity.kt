/*
 * Copyright (c) 2018. Minjae Son
 */

package com.bungabear.csvtosms

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.telephony.SmsManager
import android.util.Log
import android.widget.ArrayAdapter
import com.opencsv.CSVReader
import kotlinx.android.synthetic.main.activity_main.*
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.Charset
import java.util.*

class MainActivity : AppCompatActivity() {
    private val asciiEncoder = Charset.forName("US-ASCII").newEncoder()
    private val context: Context = this
    private var adapter : ArrayAdapter<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1)
        btn_open.setOnClickListener {
            val mimeTypes = arrayOf("text/csv", "text/comma-separated-values")
            val intent = Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT)
                    .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            startActivityForResult(Intent.createChooser(intent, "Select a file"), 0)

        }
    }

    // When file selected,
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 0 && resultCode == RESULT_OK){
            val uri = data!!.data
            val csv = readCSV(context, uri)

            // Divide column name. CSV has first row for colum name
            val columName = csv[0]
            csv.removeAt(0)
            var tel = -1
            var message = -1

            // Find tel, message column's offset
            for(i in columName.indices){
                val name = columName[i]
                when(name){
                // Start of column name has "\uFEFF" letter. It is not visible, but effects when compare.
                    "\uFEFFtel" -> tel = i
                    "tel" -> tel = i
                    "\uFEFFmessage" -> message = i
                    "message" -> message = i
                }
                if( tel != -1 && message != -1)
                    break
            }

            adapter!!.clear()
            // SMS 전송
            if( tel != -1 && message != -1) {
                csv.map {
                    try{
                        sendSMS(it[tel], it[message])
                    }
                    catch (e : Exception){
                        Log.e("sendSMS", "tel $it.tel \nmessage $it.message",e)
                    }
                }
            }
        }
    }

    private fun readCSV(context : Context, uri : Uri) : Vector<Array<String>>{
        val data = Vector<Array<String>>()

        // Copy InputStream data to tmp file
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "tmp.csv")
        FileUtils.copyInputStreamToFile(inputStream, file)
        inputStream.close()

        val reader = CSVReader(file.reader())
        var s: Array<String>?

        do{
            s = reader.readNext()
            if(s == null){
                break
            }
            data.add(s)
            Log.d("reasCSV", s.joinToString())
        }while (true)

        file.delete()
        return data
    }

    private fun sendSMS(num : String, message : String){
        // Split message when message.length over a SMS length.
        if(isPureAscii(message)){
            if(message.length > 140) {
                sendSMS(num, message.substring(0, 140))
                sendSMS(num, message.substring(140))
                return
            }
        }
        else {
            if(message.length > 70) {
                sendSMS(num, message.substring(0, 70))
                sendSMS(num, message.substring(70))
                return
            }
        }
//      TODO Send process feedback
//        val sentIntent : PendingIntent = PendingIntent.getBroadcast(this, 0, Intent("SMS_SENT_ACTION"), 0)
//        val deliveredIntent : PendingIntent  = PendingIntent.getBroadcast(this, 0, Intent("SMS_DELIVERED_ACTION"), 0)
//
//        registerReceiver(object:BroadcastReceiver(){
//            override fun onReceive(context: Context?, intent: Intent?) {
//                Log.d("test", ""+resultCode)
//                when(resultCode){
//                    Activity.RESULT_OK->
//                        // 전송 성공
//                        Toast.makeText(context, "전송 완료", Toast.LENGTH_SHORT).show();
//                    SmsManager.RESULT_ERROR_GENERIC_FAILURE ->
//                        // 전송 실패
//                        Toast.makeText(context, "전송 실패", Toast.LENGTH_SHORT).show();
//                    SmsManager.RESULT_ERROR_NO_SERVICE ->                // 서비스 지역 아님
//                        Toast.makeText(context, "서비스 지역이 아닙니다", Toast.LENGTH_SHORT).show();
//                    SmsManager.RESULT_ERROR_RADIO_OFF->
//                        // 무선 꺼짐
//                        Toast.makeText(context, "무선(Radio)가 꺼져있습니다", Toast.LENGTH_SHORT).show();
//                    SmsManager.RESULT_ERROR_NULL_PDU->
//                        // PDU 실패
//                        Toast.makeText(context, "PDU Null", Toast.LENGTH_SHORT).show();
//                }
////                context.unregisterReceiver(this)
//            }
//        }, IntentFilter("SMS_SEND_ACTION"));
//
//        registerReceiver(object:BroadcastReceiver() {
//            override fun onReceive(context: Context?, intent: Intent?) {
//                Log.d("test", ""+resultCode)
//                when(resultCode){
//                // 도착 완료
//                    Activity.RESULT_OK -> Toast.makeText(context, "SMS 도착 완료", Toast.LENGTH_SHORT).show()
//                // 도착 안됨
//                    Activity.RESULT_CANCELED -> Toast.makeText(context, "SMS 도착 실패", Toast.LENGTH_SHORT).show()
//
//                }
////                context.unregisterReceiver(this)
//            }
//
//        }, IntentFilter("SMS_DELIVERED_ACTION"))
        val mSmsManager : SmsManager = SmsManager.getDefault()
        mSmsManager.sendTextMessage(num, null, message, null, null)
        val log = "tel : $num \nmessage : $message"
        Log.d("sendSMS", log)

        adapter!!.add(log)
        lv_sendlist.adapter = adapter
    }

    // Check string is only ascii
    private fun isPureAscii(v: String): Boolean {
        return asciiEncoder.canEncode(v)
    }

}