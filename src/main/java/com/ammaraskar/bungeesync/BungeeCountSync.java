package com.ammaraskar.bungeesync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import org.yaml.snakeyaml.Yaml;

public class BungeeCountSync extends Plugin implements Listener {

    private String server;
    private ServerCountProvider countProvider;

    @Override
    public void onEnable() {
        this.getProxy().getPluginManager().registerListener(this, this);

        try {
            this.loadConfig();
        } catch (IOException | SQLException | ClassNotFoundException | RuntimeException ex) {
            getLogger().log(Level.WARNING, "Failed to load configuration.", ex);
            throw new RuntimeException("Failed to start BungeeCountSync", ex);
        }
    }

    public String getServerName() {
        return this.server;
    }

    public ServerCountProvider getServerCountProvider() {
        return this.countProvider;
    }

    private void loadConfig() throws IOException, SQLException, ClassNotFoundException {
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdir();
        }

        File exampleConfig = new File(this.getDataFolder(), "example_config.yml");
        if (!exampleConfig.exists()) {
            exampleConfig.createNewFile();
            try (InputStream in = this.getResourceAsStream("example_config.yml"); OutputStream out = new FileOutputStream(exampleConfig)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }

        File file = new File(this.getDataFolder() + File.separator + "config.yml");
        Yaml yaml = new Yaml();
        Map<?, ?> rawYaml = (Map<?, ?>) yaml.load(new FileInputStream(file));

        this.server = (String) rawYaml.get("server");

        String countMethod = "mysql";
        if (rawYaml.containsKey("countMethod")) {
            countMethod = (String) rawYaml.get("countMethod");
        }

        if (countMethod.equalsIgnoreCase("mysql")) {
            this.countProvider = new SQLCountProvider(this, rawYaml);
        }
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        ServerPing response = event.getResponse();
        int players = this.countProvider.getTotalCount(this.getProxy().getOnlineCount());
        response.setPlayers(new ServerPing.Players(response.getPlayers().getMax(), players, new ServerPing.PlayerInfo[]{}));
    }
}
