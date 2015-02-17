package com.archibus.app.reservation.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.*;

/**
 * Utility class. Provides methods to transcode strings. Used by AppointmentHelper to convert
 * Appointment GUIDs.
 * 
 * @author Yorik Gerlo
 * @since 21.2
 */
public final class StringTranscoder {
    
    /**
     * Private default constructor: utility class is non-instantiable.
     */
    private StringTranscoder() {
        super();
    }
    
    /**
     * Encode a string from base 16 (hex) to base64.
     * 
     * @param uid the base16 encoded string
     * @return the base64 encoded string
     * @throws DecoderException when the transcode fails
     */
    public static String transcodeHexToBase64(final String uid) throws DecoderException {
        // The Appointment GUID must be base64 encoded for EWS. The IcalUid property is base16
        // (hex), so convert it to base64.
        final byte[] buffer = Hex.decodeHex(uid.toCharArray());
        return new String(Base64.encodeBase64(buffer));
    }
    
}
