package com.security.jwt.service;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Calendar;
import java.util.Date;

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
    private static final String CERTIFICATE_DN = "CN=JWT-EC256, OU=JWT, O=Dev, L=Seoul, ST=Seoul, C=KR";
    private static final int VALIDITY_YEARS = 10;

    /**
     * EC256 (P-256) 키쌍 생성
     */
    public static KeyPair generateEC256KeyPair() throws Exception {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime256v1");
        keyGen.initialize(ecSpec);
        return keyGen.generateKeyPair();
    }

    /**
     * 자체 서명 인증서 생성
     */
    public static X509Certificate generateSelfSignedCertificate(KeyPair keyPair) throws Exception {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        X500Name issuerName = new X500Name(CERTIFICATE_DN);
        X500Name subjectName = issuerName;

        BigInteger serialNumber = new BigInteger(64, new SecureRandom());

        Date notBefore = new Date();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, VALIDITY_YEARS);
        Date notAfter = cal.getTime();

        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
            issuerName,
            serialNumber,
            notBefore,
            notAfter,
            subjectName,
            SubjectPublicKeyInfo.getInstance(
                ASN1Sequence.getInstance(keyPair.getPublic().getEncoded())
            )
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
            .setProvider("BC")
            .build((java.security.interfaces.ECPrivateKey) keyPair.getPrivate());

        X509CertificateHolder certHolder = builder.build(signer);
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
    }

    /**
     * 인증서 배열 생성
     */
    public static X509Certificate[] createCertificateChain(X509Certificate certificate) {
        return new X509Certificate[]{certificate};
    }

    public static String getCertificateDN() {
        return CERTIFICATE_DN;
    }

    public static int getValidityYears() {
        return VALIDITY_YEARS;
    }
}
