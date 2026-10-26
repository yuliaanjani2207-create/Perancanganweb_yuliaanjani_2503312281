package model;

/** Entitas dosen (tabel dosen). */
public class Dosen {
    private int id;
    private String nidn;
    private String nama;
    private String prodi;
    private String email;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNidn() { return nidn; }
    public void setNidn(String nidn) { this.nidn = nidn; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public String getProdi() { return prodi; }
    public void setProdi(String prodi) { this.prodi = prodi; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
