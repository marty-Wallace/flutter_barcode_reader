import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';

enum BarcodeType {
  UPC_A,
  UPC_E,
  EAN_13,
  EAN_8,
  RSS_14,
  CODE_39,
  CODE_93,
  CODE_128,
  ITF,
  CODABAR,
  QR_CODE,
  DATA_MATRIX,
  PDF_417,
}

enum BarcodeViewOrientation { DEFAULT, PORTRAIT, LANDSCAPE }

class BarcodeScanner {
  static const CameraAccessDenied = 'PERMISSION_NOT_GRANTED';
  static const UserCanceled = 'USER_CANCELED';
  static const MethodChannel _channel =
      const MethodChannel('com.apptreesoftware.barcode_scan');

  static Future<String> scan(
      {List<BarcodeType> types,
      BarcodeViewOrientation orientation,
      double viewWidthOffsetRatio,
      double viewHeightOffsetRatio}) async {

    if (!Platform.isAndroid) {

      return await _channel.invokeMethod('scan');
    }

    Map<String, dynamic> args = {
      "formats": _barcodeListToStringArg(types),
      "orientation": _fixEnumToString((orientation ?? BarcodeViewOrientation.DEFAULT).toString()),
    };

    if(viewWidthOffsetRatio != null && viewHeightOffsetRatio != null) {
      args.addAll({
        "view_width_offset_ratio": viewWidthOffsetRatio,
        "view_height_offset_ratio": viewHeightOffsetRatio,
      });
    }

    return await _channel.invokeMethod('scan', args);
  }

  static String _barcodeListToStringArg(List<BarcodeType> types) {
    return types
            ?.map((type) => _fixEnumToString(type.toString()))
            ?.toList()
            ?.join(' ') ??
        '';
  }

  static String _fixEnumToString(String toString) {
    // Remove the Enum name and . from the toString()
    return toString.substring(toString.indexOf('.') + 1);
  }
}
