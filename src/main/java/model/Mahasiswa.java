package model;

/** Entitas mahasiswa (tabel mahasiswa). */
public class Mahasiswa {
    private int id;
    private String nim;
    private String nama;
    private String prodi;
    private String email;
    private String telepon;
    private String status = "AKTIF";

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNim() { return nim; }
    public void setNim(String nim) { this.nim = nim; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public String getProdi() { return prodi; }
    public void setProdi(String prodi) { this.prodi = prodi; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelepon() { return telepon; }
    public void setTelepon(String telepon) { this.telepon = telepon; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
