import arc.Events;
import arc.struct.ObjectMap;
import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.world.Tile;
import mindustry.world.blocks.logic.LogicBlock;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Core extends Plugin {
    public static DiscordBot discordBot = null;

    private static final ObjectMap<String, Tutorial> tutorials = new ObjectMap<>();

    @Override
    public void init() {
        Events.on(EventType.ServerLoadEvent.class, event -> {
            DiscordBot.loadBot();
        });

        Sql.RegisterEvents();//has to be first in order for other modules to be able to use SQL during ServerLoadEvent
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("scconfig", "[name] [value...]", "Configure server settings.", arg -> {
            if (arg.length == 0) {
                Log.info("All config values:");
                for (PluginConfig c : PluginConfig.all) {
                    Log.info("&lk| @: @", c.name(), "&lc&fi" + c.get());
                    Log.info("&lk| | &lw" + c.description);
                    Log.info("&lk|");
                }
                Log.info("use the command with the value set to \"default\" in order to use the default value.");
                return;
            }

            try {
                PluginConfig c = PluginConfig.valueOf(arg[0]);
                if (arg.length == 1) {
                    Log.info("'@' is currently @.", c.name(), c.get());
                } else {
                    if (arg[1].equals("default")) {
                        c.set(c.defaultValue);
                    } else if (c.isBool()) {
                        c.set(arg[1].equals("on") || arg[1].equals("true"));
                    } else if (c.isNum()) {
                        try {
                            c.set(Integer.parseInt(arg[1]));
                        } catch (NumberFormatException e) {
                            Log.err("Not a valid number: @", arg[1]);
                            return;
                        }
                    } else if (c.isString()) {
                        if (c.isB64()) {
                            try {
                                c.set(JLib.longToB64(Long.parseLong(arg[1])));
                            } catch (Exception e) {
                                Log.err("Not a valid number: @", arg[1]);
                                return;
                            }
                        } else {
                            c.set(arg[1].replace("\\n", "\n"));
                        }
                    }

                    Log.info("@ set to @.", c.name(), c.get());
                    arc.Core.settings.forceSave();
                }
            } catch (IllegalArgumentException e) {
                Log.err("Unknown ServerPluginConfig: '@'. Run the command with no arguments to get a list of valid ServerPluginConfigs.", arg[0]);
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        DiscordBot.registerClientCommands(handler);
        handler.<Player>register("test", "", (args, p) -> {
            Tile t = Vars.world.tile(0);
            Call.constructFinish(t, Blocks.worldProcessor, null, (byte) 0, Team.sharded, null);
            if (t.build instanceof LogicBlock.LogicBuild lb) {
                lb.updateCode("");
            }
        });
    }

    private static void loadTutorials() {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet("https://api.github.com/repositories/502487703/contents/tutorials");
            try {
                HttpResponse response = client.execute(get);
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                JSONArray ja = new JSONArray(responseBody);

                for (int i = 0; i < ja.length(); i++) {
                    JSONObject jo = ja.getJSONObject(i);
                    addTutorial(jo.getString("download_url"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addTutorial(String link) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(link);
            try {
                HttpResponse response = client.execute(get);
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                JSONObject jo = new JSONObject(responseBody);
                tutorials.put(jo.getString("name"), new Tutorial(jo.getString("description"), jo.getString("author"), jo.getBoolean("debug"), jo.getString("code")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record Tutorial(String desc, String author, boolean debug, String code) {}
}
