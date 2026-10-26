package model;

/** Metadata token QR yang pernah diterbitkan (tabel token_qr).
 *  Yang disimpan hanya HASH dari token (SHA-256), bukan token aslinya,
 *  sehingga bocornya database tidak membocorkan cara memalsukan QR. */
public class TokenQr {
    private int id;
    private int sesiId;
    private String tokenHash;
    private String createdAt;
    private String expiresAt;
    private boolean used;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSesiId() { return sesiId; }
    public void setSesiId(int sesiId) { this.sesiId = sesiId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}
