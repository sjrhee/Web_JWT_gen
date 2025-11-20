package com.security.jwt.service;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Calendar;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

/**
 * 인증서 생성 서비스
 */
public class CertificateService {
    private static final Logger logger = LogManager.getLogger(CertificateService.class);
    private static final String CERTIFICATE_DN = "CN=JWT-EC256, OU=JWT, O=Dev, L=Seoul, ST=Seoul, C=KR";
    private static final int VALIDITY_YEARS = 10;

    /**
     * EC256 (P-256) 키쌍 생성
     */
    public static KeyPair generateEC256KeyPair() throws Exception {
        logger.info("=== generateEC256KeyPair START ===");
        try {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
                logger.info("BouncyCastle 보안 제공자 등록");
            }

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime256v1");
            keyGen.initialize(ecSpec);
            logger.info("EC 키 생성기 초기화 완료 (Curve: prime256v1)");

            KeyPair keyPair = keyGen.generateKeyPair();
            logger.info("EC256 키쌍 생성 완료");
            logger.info("=== generateEC256KeyPair END ===");
            return keyPair;
        } catch (Exception e) {
            logger.error("EC256 키쌍 생성 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 자체 서명 인증서 생성
     */
    public static X509Certificate generateSelfSignedCertificate(KeyPair keyPair) throws Exception {
        logger.info("=== generateSelfSignedCertificate START ===");
        try {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
                logger.info("BouncyCastle 보안 제공자 등록");
            }

            X500Name issuerName = new X500Name(CERTIFICATE_DN);
            X500Name subjectName = issuerName;
            logger.info("인증서 DN 설정: {}", CERTIFICATE_DN);

            BigInteger serialNumber = new BigInteger(64, new SecureRandom());
            logger.info("시리얼 번호 생성: {}", serialNumber.toString(16));

            Date notBefore = new Date();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, VALIDITY_YEARS);
            Date notAfter = cal.getTime();
            logger.info("인증서 유효 기간: {} ~ {}", notBefore, notAfter);

            X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                    issuerName,
                    serialNumber,
                    notBefore,
                    notAfter,
                    subjectName,
                    SubjectPublicKeyInfo.getInstance(
                            ASN1Sequence.getInstance(keyPair.getPublic().getEncoded())));
            logger.info("X509v3 인증서 빌더 생성 완료");

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                    .setProvider("BC")
                    .build((java.security.interfaces.ECPrivateKey) keyPair.getPrivate());
            logger.info("SHA256withECDSA 서명자 생성 완료");

            X509CertificateHolder certHolder = builder.build(signer);
            logger.info("인증서 서명 완료");

            X509Certificate certificate = new JcaX509CertificateConverter().setProvider("BC")
                    .getCertificate(certHolder);
            logger.info("최종 X509 인증서 변환 완료");
            logger.info("=== generateSelfSignedCertificate END ===");
            return certificate;
        } catch (Exception e) {
            logger.error("자체 서명 인증서 생성 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 인증서 배열 생성
     */
    public static X509Certificate[] createCertificateChain(X509Certificate certificate) {
        return new X509Certificate[] { certificate };
    }

    public static String getCertificateDN() {
        return CERTIFICATE_DN;
    }

    public static int getValidityYears() {
        return VALIDITY_YEARS;
    }
}
