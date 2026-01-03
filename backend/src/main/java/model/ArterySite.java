package model;

public enum ArterySite {
    AbdAorta("AbdAorta"),
    AntTibial("AntTibial"),
    AorticRoot("AorticRoot"),
    Brachial("Brachial"),
    Carotid("Carotid"),
    CommonIliac("CommonIliac"),
    Digital("Digital"),
    Femoral("Femoral"),
    IliacBif("IliacBif"),
    Radial("Radial"),
    SupMidCerebral("SupMidCerebral"),
    SupTemporal("SupTemporal"),
    ThorAorta("ThorAorta");

    private final String token;
    ArterySite(String token) { this.token = token; }

    public String token() { return token; }

    public String pressureFileName() {
        return "PWs_" + token + "_P.csv";
    }
}