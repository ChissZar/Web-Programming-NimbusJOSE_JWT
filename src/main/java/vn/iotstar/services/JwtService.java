package vn.iotstar.services;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import vn.iotstar.exception.ExpiredJwtException;
import vn.iotstar.exception.SignatureException;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service xử lý các thao tác liên quan đến JWT (tạo token, trích xuất claim, xác thực token).
 * Sử dụng thư viện Nimbus JOSE + JWT thay thế cho JJWT (io.jsonwebtoken).
 */
@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, JWTClaimsSet::getSubject);
    }

    public <T> T extractClaim(String token, Function<JWTClaimsSet, T> claimsResolver) {
        final JWTClaimsSet claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        try {
            Date now = new Date(System.currentTimeMillis());
            Date expiryDate = new Date(System.currentTimeMillis() + expiration);

            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(userDetails.getUsername())
                    .issueTime(now)
                    .expirationTime(expiryDate);

            if (extraClaims != null) {
                for (Map.Entry<String, Object> entry : extraClaims.entrySet()) {
                    builder.claim(entry.getKey(), entry.getValue());
                }
            }

            JWTClaimsSet claimsSet = builder.build();
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);

            JWSSigner signer = new MACSigner(getSigningKey());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo JWT bằng thư viện Nimbus JOSE + JWT", e);
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        return expiration != null && expiration.before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, JWTClaimsSet::getExpirationTime);
    }

    private JWTClaimsSet extractAllClaims(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(getSigningKey());
            if (!signedJWT.verify(verifier)) {
                throw new SignatureException("The JWT signature is invalid");
            }
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            if (claimsSet.getExpirationTime() != null && claimsSet.getExpirationTime().before(new Date())) {
                throw new ExpiredJwtException("The JWT token has expired");
            }
            return claimsSet;
        } catch (SignatureException | ExpiredJwtException e) {
            throw e;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid compact JWT string: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Could not verify token", e);
        }
    }

    private byte[] getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length >= 32) {
            return keyBytes;
        }
        byte[] padded = new byte[32];
        System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
        return padded;
    }
}
