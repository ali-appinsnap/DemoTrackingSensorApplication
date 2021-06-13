package com.example.demotrackingsensorapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.lifecycle.Observer
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * Created by Muhammad Ali on 05-May-20.
 * Email muhammad.ali9385@gmail.com
 */


val REQUEST_CODE_SEND_SMS = 2324



@BindingAdapter("setDrawable")
fun setImageUri(view: ImageView, imageUri: String?) {
    if (imageUri == null) {
        view.setImageURI(null)
    } else {
        view.setImageURI(Uri.parse(imageUri))
    }
}

@BindingAdapter("setDrawable")
fun setImageUri(view: ImageView, imageUri: Uri?) {
    view.setImageURI(imageUri)
}

@BindingAdapter("setDrawable")
fun setImageDrawable(view: ImageView, drawable: Drawable?) {
    view.setImageDrawable(drawable)
}

@BindingAdapter("setDrawable")
fun setImageResource(imageView: ImageView, resource: Int) {
    imageView.setImageResource(resource)
}


/**
 * extension methods for logs.
 */

val Any.APP_TAG: String
    get() = "logGenix::" + this::class.simpleName

fun logV(tag: String, msg: String) {
    if (BuildConfig.DEBUG) Log.v(tag, msg)
}

// do something for a debug build
fun logD(tag: String, msg: String) {
    if (BuildConfig.DEBUG) Log.d(tag, msg)
}

fun logE(tag: String, msg: String) {
    if (BuildConfig.DEBUG) Log.e(tag, msg)
}


/**
 * extension methods for Toasts.
 */
fun showShort(context: Context, msg: String) {
    if (BuildConfig.DEBUG) Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

fun showReleaseShort(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

// do something for a debug build
fun showLong(context: Context, msg: String) {
    if (BuildConfig.DEBUG) Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}

// do something for a debug build
fun showReleaseLong(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}


/**
 * extension methods for Views.
 */
fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}


fun getImeiOrDeviceId(context: Context): String {
    /* val tm =
         ContextCompat.getSystemService<Any>(context) as TelephonyManager?
     return tm!!.deviceId*/
    return ""
}


fun getOtp(message: String): String {
    val pattern: Pattern = Pattern.compile("(\\d{6})")

//   \d is for a digit
//   {} is the number of digits here 6.


//   \d is for a digit
//   {} is the number of digits here 6.
    val matcher: Matcher = pattern.matcher(message)
    var value = ""
    if (matcher.find()) {
        value = matcher.group(0) // 6 digit number
    }
    return value
}


/*

fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap? {
    var width = image.width
    var height = image.height
    val bitmapRatio = width.toFloat() / height.toFloat()
    if (bitmapRatio > 1) {
        width = maxSize
        height = (width / bitmapRatio).toInt()
    } else {
        height = maxSize
        width = (height * bitmapRatio).toInt()
    }
    return Bitmap.createScaledBitmap(image, width, height, true)
}*/
