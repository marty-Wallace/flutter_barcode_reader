package com.apptreesoftware.barcodescan

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar

class BarcodeScanPlugin(private val registrar: Registrar): MethodCallHandler,
        PluginRegistry.ActivityResultListener {
    private var result : Result? = null
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar): Unit {
            val channel = MethodChannel(registrar.messenger(), "com.apptreesoftware.barcode_scan")
            if (registrar.activity() != null) {
            val plugin = BarcodeScanPlugin(registrar)
            channel.setMethodCallHandler(plugin)
            registrar.addActivityResultListener(plugin)
        }
    }}

    override fun onMethodCall(call: MethodCall, result: Result): Unit {
        if (call.method == "scan") {
            this.result = result

            val formats = call.argument<String>("formats").toUpperCase()

            val orientation = if(call.hasArgument("orientation")) {
                call.argument<String>("orientation").toUpperCase()
            }else {
                "LANDSCAPE"
            }

            val widthOffsetRatio = if(call.hasArgument("view_width_offset_ratio")) {
                call.argument<Double>("view_width_offset_ratio")
            }else {
                0.15
            }
            val heightOffsetRatio = if(call.hasArgument("view_height_offset_ratio")) {
                call.argument<Double>("view_height_offset_ratio")
            }else {
                0.15
            }
            showBarcodeView(formats, orientation, widthOffsetRatio, heightOffsetRatio)
        } else {
            result.notImplemented()
        }
    }

    private fun showBarcodeView(formats: String, orientation: String, widthOffsetRatio: Double, heightOffsetRatio: Double) {
        val intent = Intent(registrar.activity(), BarcodeScannerActivity::class.java)
        intent.putExtra("formats", formats)
        intent.putExtra("orientation", orientation)
        intent.putExtra("height_offset_ratio", heightOffsetRatio)
        intent.putExtra("width_offset_ratio", widthOffsetRatio)

        registrar.activity().startActivityForResult(intent, 100)
    }

    override fun onActivityResult(code: Int, resultCode: Int, data: Intent?): Boolean {
        if (code == 100) {
            if (resultCode == Activity.RESULT_OK) {
                val barcode = data?.getStringExtra("SCAN_RESULT")
                barcode?.let { this.result?.success(barcode) }
            } else {
                val errorCode = data?.getStringExtra("ERROR_CODE")
                this.result?.error(errorCode, null, null)
            }
            return true
        }
        return false
    }
}
