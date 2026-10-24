package pe.edu.cibertec.smartfix.incident.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JwtUtil {

    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();
    private static final Pattern SUB = Pattern.compile("\"sub\":\"(\\d+)\"");
    private static final Pattern ROL = Pattern.compile("\"rol\":\"([A-Z_]+)\"");
    private static final Pattern EXP = Pattern.compile("\"exp\":(\\d+)");

    public record Claims(Long id, String rol) {}

    private JwtUtil() {
    }

    public static String generar(String secret, Long id, String rol, String nombre, String correo, long horas) {
        long ahora = System.currentTimeMillis() / 1000;
        String header = ENC.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String json = "{\"sub\":\"" + id + "\",\"rol\":\"" + rol + "\",\"nombre\":\"" + escapar(nombre)
                + "\",\"correo\":\"" + escapar(correo) + "\",\"iat\":" + ahora + ",\"exp\":" + (ahora + horas * 3600) + "}";
        String payload = ENC.encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + "." + firmar(secret, header + "." + payload);
    }

    public static Claims validar(String secret, String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length != 3) {
                return null;
            }
            String esperada = firmar(secret, partes[0] + "." + partes[1]);
            if (!MessageDigest.isEqual(esperada.getBytes(StandardCharsets.UTF_8), partes[2].getBytes(StandardCharsets.UTF_8))) {
                return null;
            }
            String json = new String(DEC.decode(partes[1]), StandardCharsets.UTF_8);
            Matcher exp = EXP.matcher(json);
            Matcher sub = SUB.matcher(json);
            Matcher rol = ROL.matcher(json);
            if (!exp.find() || !sub.find() || !rol.find()) {
                return null;
            }
            if (Long.parseLong(exp.group(1)) < System.currentTimeMillis() / 1000) {
                return null;
            }
            return new Claims(Long.valueOf(sub.group(1)), rol.group(1));
        } catch (Exception e) {
            return null;
        }
    }

    private static String firmar(String secret, String datos) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return ENC.encodeToString(mac.doFinal(datos.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el token", e);
        }
    }

    private static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
