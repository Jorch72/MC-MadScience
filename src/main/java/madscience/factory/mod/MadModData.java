package madscience.factory.mod;

import madscience.factory.MadTileEntityFactoryProductData;

import com.google.gson.annotations.Expose;

public class MadModData
{
    // Identification.
    @Expose
    private String id;

    @Expose
    private String name;

    @Expose
    private String channelName;

    // Metadata.
    @Expose
    private String description;

    @Expose
    private String homeURL;

    @Expose
    private String logoPath;

    @Expose
    private String credits;

    @Expose
    private String[] authors;

    @Expose
    private String fingerprint;

    @Expose
    private String minecraftVersion;

    @Expose
    private String dependencies;

    // Proxy Classes Namespace.
    @Expose
    private String proxyClient;

    @Expose
    private String proxyServer;

    // Full version string for internal reference by mod.
    @Expose
    private String versionMajor;

    @Expose
    private String versionMinor;

    @Expose
    private String versionRevision;

    @Expose
    private String versionBuild;

    // Update checker.
    @Expose
    private String updateURL;

    // Machines
    @Expose
    private MadTileEntityFactoryProductData[] unregisteredMachines;

    public MadModData(String id,
            String name,
            String channelName,
            String description,
            String homeURL,
            String logoPath,
            String credits,
            String[] authors,
            String fingerprint,
            String mcVersionSupported,
            String forgeDependencies,
            String proxyClient,
            String proxyServer,
            String versionMajor,
            String versionMinor,
            String versionRevision,
            String versionBuild,
            String updateURL,
            MadTileEntityFactoryProductData[] madMachines)
    {
        super();

        this.id = id;
        this.name = name;
        this.channelName = channelName;

        this.description = description;
        this.homeURL = homeURL;
        this.logoPath = logoPath;
        this.credits = credits;
        this.authors = authors;
        this.fingerprint = fingerprint;

        this.minecraftVersion = mcVersionSupported;
        this.dependencies = forgeDependencies;

        this.proxyClient = proxyClient;
        this.proxyServer = proxyServer;

        this.versionMajor = versionMajor;
        this.versionMinor = versionMinor;
        this.versionRevision = versionRevision;
        this.versionBuild = versionBuild;

        this.updateURL = updateURL;

        this.unregisteredMachines = madMachines;
    }

    public String getID()
    {
        return id;
    }

    public String getModName()
    {
        return name;
    }

    public String getChannelName()
    {
        return channelName;
    }

    public String getDescription()
    {
        return description;
    }

    public String getHomeURL()
    {
        return homeURL;
    }

    public String getLogoPath()
    {
        return logoPath;
    }

    public String getCredits()
    {
        return credits;
    }

    public String[] getAuthors()
    {
        return authors;
    }

    public String getFingerprint()
    {
        return fingerprint;
    }

    public String getMCAcceptedVersions()
    {
        return minecraftVersion;
    }

    public String getForgeDependencies()
    {
        return dependencies;
    }

    public String getProxyClientNamespace()
    {
        return proxyClient;
    }

    public String getProxySeverNamespace()
    {
        return proxyServer;
    }

    public String getVersionMajor()
    {
        return versionMajor;
    }

    public String getVersionMinor()
    {
        return versionMinor;
    }

    public String getVersionRevision()
    {
        return versionRevision;
    }

    public String getVersionBuild()
    {
        return versionBuild;
    }

    public String getUpdateURL()
    {
        return updateURL;
    }

    public MadTileEntityFactoryProductData[] getUnregisteredMachines()
    {
        return unregisteredMachines;
    }
}
