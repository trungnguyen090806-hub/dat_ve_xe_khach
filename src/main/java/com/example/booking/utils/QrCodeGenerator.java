package com.example.booking.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public final class QrCodeGenerator {

    private QrCodeGenerator() {
    }

    /**
     * Tao anh QR code dang Base64 PNG
     * @param text Noi dung ma hoa trong QR
     * @param width Chieu rong anh (px)
     * @param height Chieu cao anh (px)
     * @return Chuoi Base64 PNG co the gan vao src="data:image/png;base64,..."
     */
    public static String generateBase64Qr(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] pngData = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(pngData);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi sinh mã QR: " + e.getMessage(), e);
        }
    }
}
