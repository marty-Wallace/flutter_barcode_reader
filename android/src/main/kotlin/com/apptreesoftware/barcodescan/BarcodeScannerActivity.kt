package com.apptreesoftware.barcodescan

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import me.dm7.barcodescanner.core.IViewFinder
import me.dm7.barcodescanner.core.R
import me.dm7.barcodescanner.core.ViewFinderView
import me.dm7.barcodescanner.zxing.ZXingScannerView


class BarcodeScannerActivity : Activity(), ZXingScannerView.ResultHandler {

    private lateinit var scannerView: me.dm7.barcodescanner.zxing.ZXingScannerView

    companion object {
        const val REQUEST_TAKE_PHOTO_CAMERA_PERMISSION = 100
        const val TOGGLE_FLASH = 200

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = ""
        val formats = intent.getStringExtra("formats")
        val orientation =  intent.getStringExtra("orientation")
        if(orientation != "PORTRAIT") {
            this.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        val widthOffset = intent.getDoubleExtra("width_offset_ratio", .15)
        val heightOffset = intent.getDoubleExtra("height_offset_ratio", .15)
        val types = BarcodeFormatConverter.stringsToEnum(formats)

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val heightPx = metrics.heightPixels
        val widthPx = metrics.widthPixels

        scannerView = object : ZXingScannerView(this) {
            override fun createViewFinderView(context: Context): IViewFinder {
                return CustomViewFinderView(context, widthPx, heightPx, widthOffset, heightOffset).apply {

                    setBorderColor(ContextCompat.getColor(context, R.color.viewfinder_border))
                    setLaserColor(ContextCompat.getColor(context, R.color.viewfinder_laser))
                    setLaserEnabled(true)
                    setBorderStrokeWidth(resources.getInteger(R.integer.viewfinder_border_width))
                    setBorderLineLength(resources.getInteger(R.integer.viewfinder_border_length))
                    setMaskColor(ContextCompat.getColor(context, R.color.viewfinder_mask))
                    setBorderCornerRounded(false)
                    setBorderCornerRadius(0)
                    setSquareViewFinder(false)
                    setViewFinderOffset(0)
                }
            }
        }

        scannerView.formats.removeAll(ZXingScannerView.ALL_FORMATS)
        scannerView.setFormats(types)
        scannerView.setAutoFocus(true)
        scannerView.setBorderLineLength(20)
        scannerView.setSquareViewFinder(false)
        setContentView(scannerView)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (scannerView.flash) {
            val item = menu.add(0,
                    TOGGLE_FLASH, 0, "Flash Off")
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        } else {
            val item = menu.add(0,
                    TOGGLE_FLASH, 0, "Flash On")
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == TOGGLE_FLASH) {
            scannerView.flash = !scannerView.flash
            this.invalidateOptionsMenu()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        scannerView.setResultHandler(this)
        // start camera immediately if permission is already given
        if (!requestCameraAccessIfNecessary()) {
            scannerView.startCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        scannerView.stopCamera()
    }

    override fun handleResult(result: Result?) {
        val intent = Intent()
        intent.putExtra("SCAN_RESULT", result.toString())
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun finishWithError(errorCode: String) {
        val intent = Intent()
        intent.putExtra("ERROR_CODE", errorCode)
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
    }

    private fun requestCameraAccessIfNecessary(): Boolean {
        val array = arrayOf(Manifest.permission.CAMERA)
        if (ContextCompat
                .checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, array,
                    REQUEST_TAKE_PHOTO_CAMERA_PERMISSION)
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,grantResults: IntArray) {
        when (requestCode) {
            REQUEST_TAKE_PHOTO_CAMERA_PERMISSION -> {
                if (PermissionUtil.verifyPermissions(grantResults)) {
                    scannerView.startCamera()
                } else {
                    finishWithError("PERMISSION_NOT_GRANTED")
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

}

object BarcodeFormatConverter {

    fun stringsToEnum(formats : String) : MutableList<BarcodeFormat> {
        if (formats == "") {
            return ZXingScannerView.ALL_FORMATS
        }
        return formats.split(' ').map {
            when (it) {
                "UPC_A" -> BarcodeFormat.UPC_A
                "UPC_E" -> BarcodeFormat.UPC_E
                "EAN_13" -> BarcodeFormat.EAN_13
                "EAN_8" -> BarcodeFormat.EAN_8
                "RSS_14" -> BarcodeFormat.RSS_14
                "CODE_39" -> BarcodeFormat.CODE_39
                "CODE_93" -> BarcodeFormat.CODE_93
                "CODE_128" -> BarcodeFormat.CODE_128
                "ITF" -> BarcodeFormat.ITF
                "CODABAR" -> BarcodeFormat.CODABAR
                "QR_CODE" -> BarcodeFormat.QR_CODE
                "DATA_MATRIX" -> BarcodeFormat.DATA_MATRIX
                "PDF_417" -> BarcodeFormat.PDF_417
                else -> {}
            }
        }.filterIsInstance<BarcodeFormat>().toList().toMutableList()
    }
}

object PermissionUtil {

    /**
     * Check that all given permissions have been granted by verifying that each entry in the
     * given array is of the value [PackageManager.PERMISSION_GRANTED].

     * @see Activity.onRequestPermissionsResult
     */
    fun verifyPermissions(grantResults: IntArray): Boolean {
        return grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }
}

class CustomViewFinderView(context: Context, private val widthPx : Int, private val heightPx : Int,
                           private val widthOffsetRatio : Double, private val heightOffsetRatio : Double) : ViewFinderView(context) {

    constructor(context: Context) : this(context, 0, 0, 0.15, 0.15)

    override fun updateFramingRect() {

        super.updateFramingRect()
        if(widthPx != 0 && heightPx != 0) {
            val widthOffset = (widthPx * widthOffsetRatio).toInt()
            val heightOffset = (heightPx * heightOffsetRatio).toInt()
            val originalRect = super.getFramingRect()
            originalRect.set(widthOffset, heightOffset, widthPx -  widthOffset, heightPx - heightOffset)
        }
    }
}
