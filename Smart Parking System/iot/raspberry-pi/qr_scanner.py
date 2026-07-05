import cv2
import logging
from pyzbar import pyzbar

logger = logging.getLogger("QRScanner")

class QRScanner:
    def __init__(self):
        logger.info("QR Scanner initialized.")

    def scan_from_frame(self, frame):
        """
        Scans a frame for QR codes.
        Returns the decoded text if a QR code is found, otherwise None.
        """
        if frame is None or frame.size == 0:
            return None

        try:
            # pyzbar decodes directly from BGR/RGB images
            decoded_objects = pyzbar.decode(frame)
            for obj in decoded_objects:
                # We only care about QR codes
                if obj.type == 'QRCODE':
                    qr_data = obj.data.decode('utf-8')
                    logger.info(f"QR Code detected: {qr_data}")
                    return qr_data
        except Exception as e:
            logger.error(f"Error scanning QR code: {e}")
        
        return None
