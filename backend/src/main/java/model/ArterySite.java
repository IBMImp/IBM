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

    public static ArterySite fromToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Artery site token must be provided");
        }
        for (ArterySite site : values()) {
            if (site.token.equalsIgnoreCase(token) || site.name().equalsIgnoreCase(token)) {
                return site;
            }
        }
        throw new IllegalArgumentException("Unsupported artery site: " + token);
    }

    @Override
    public String toString() {
        return token;
    }
}
