package org.acme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.nayuki.qrcodegen.QrCode;

public class QrServerTest {

    @Test
    public void testHexColorToInt() {
        assertEquals(16734003, QrServer.hexColorToInt("#FF5733"));
        assertEquals(0xFF5734, QrServer.hexColorToInt("#FF5734"));
        assertEquals(0, QrServer.hexColorToInt("#000000"));
        assertThrows(IllegalArgumentException.class, () -> QrServer.hexColorToInt("foo"));
    }

    @Test
    public void testToImage() throws IOException {
        QrCode qr = QrCode.encodeText("fooos", QrCode.Ecc.MEDIUM);
        byte[] img = QrServer.toImage(qr, 10, 4, 0xFFFFFF, 0);
        assertTrue(img.length > 0);
        //Files.write(img, new File("target/test.png"));
    }

}
