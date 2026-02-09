package org.acme;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

import io.nayuki.qrcodegen.QrCode;
import io.quarkiverse.mcp.server.ImageContent;
import io.quarkiverse.mcp.server.MetaField;
import io.quarkiverse.mcp.server.MetaKey;
import io.quarkiverse.mcp.server.MetaField.Type;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.TextResourceContents;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A minimal Python MCP server that generates customizable QR codes with an
 * interactive view UI rewritten to Quarkus MCP.
 * 
 * Original Python source:
 * https://github.com/modelcontextprotocol/ext-apps/blob/main/examples/qr-server/server.py
 */
public class QrServer {

    private static final String MCP_APP_RESOURCE_MIME_TYPE = "text/html;profile=mcp-app";

    private static final String VIEW_URI = "ui://qr-server/view.html";

    @Tool(description = "Generate a QR code from text.")
    @MetaField(name = "ui", value = """
            {"resourceUri": "ui://qr-server/view.html"}
            """, type = Type.JSON)
    ImageContent generateQr(
            @ToolArg(description = "The text/URL to encode", defaultValue = "https://modelcontextprotocol.io") String text,
            @ToolArg(description = "Size of each box in pixels", defaultValue = "10") int boxSize,
            @ToolArg(description = "Border size in boxes", defaultValue = "4") int border,
            @ToolArg(description = "Error correction level", defaultValue = "MEDIUM") QrCode.Ecc errorCorrection,
            @ToolArg(description = "Foreground color (hex code like #FF0000)", defaultValue = "#000000") String foregroundColor,
            @ToolArg(description = "Background color (hex code like #F0F0F0)", defaultValue = "#FFFFFF") String backgroundColor) {

        QrCode qr = QrCode.encodeText(text, errorCorrection);
        return new ImageContent(
                toBase64Image(qr, boxSize, border, hexColorToInt(backgroundColor), hexColorToInt(foregroundColor)),
                "image/png");
    }

    // The spec is clear that _meta.ui must be checked from both resources/list and resources/read
    // But some implementations do not care, so we append the csp meta to each resource response
    // https://github.com/modelcontextprotocol/ext-apps/blob/main/specification/draft/apps.mdx#metadata-location
    private static final Map<MetaKey, Object> RESOURCE_META = Map.of(MetaKey.of("ui"),
            new JsonObject()
                    .put("csp", new JsonObject()
                            .put("resourceDomains", new JsonArray().add("https://unpkg.com"))));

    @Resource(uri = VIEW_URI, description = "View HTML resource.", mimeType = MCP_APP_RESOURCE_MIME_TYPE)
    @MetaField(name = "ui", value = """
            {"csp": {"resourceDomains": ["https://unpkg.com"]}}
            """, type = Type.JSON)
    TextResourceContents view() {
        return new TextResourceContents(VIEW_URI,
                readResourceFile("qr-mcp-app.html"),
                MCP_APP_RESOURCE_MIME_TYPE,
                RESOURCE_META);
    }

    /**
     * Original source:
     * https://github.com/myfear/ejq_substack_articles/blob/main/qr-code-demo/src/main/java/org/acme/qr/QRCodeService.java#L64-L97
     */
    static String toBase64Image(QrCode qr, int scale, int border, int lightColor, int darkColor) {
        return java.util.Base64.getEncoder().encodeToString(toImage(qr, scale, border, lightColor, darkColor));
    }

    static byte[] toImage(QrCode qr, int scale, int border, int lightColor, int darkColor) {
        Objects.requireNonNull(qr);
        if (scale <= 0 || border < 0)
            throw new IllegalArgumentException("Value out of range");
        if (border > Integer.MAX_VALUE / 2 || qr.size + border * 2L > Integer.MAX_VALUE / scale)
            throw new IllegalArgumentException("Scale or border too large");

        BufferedImage image = new BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale,
                BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                boolean color = qr.getModule(x / scale - border, y / scale - border);
                image.setRGB(x, y, color ? darkColor : lightColor);
            }
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode image", e);
        }
    }

    private static String readResourceFile(String fileName) {
        try (InputStream is = QrServer.class.getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) {
                throw new IllegalArgumentException("File not found: " + fileName);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static int hexColorToInt(String colorHex) {
        if (colorHex == null) {
            throw new IllegalArgumentException("colorHex must not be null");
        }
        colorHex = colorHex.strip();
        if (colorHex.startsWith("#")) {
            try {
                return Integer.parseInt(colorHex.substring(1), 16);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Not a hex color: " + colorHex);
            }
        } else {
            throw new IllegalArgumentException("Invalid hex color: " + colorHex);
        }
    }

}
