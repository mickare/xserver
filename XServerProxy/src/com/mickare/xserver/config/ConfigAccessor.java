package com.mickare.xserver.config;

/*

 * Copyright (C) 2012

 *

 * Permission is hereby granted, free of charge, to any person obtaining a copy

 * of this software and associated documentation files (the "Software"), to deal

 * in the Software without restriction, including without limitation the rights

 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell

 * copies of the Software, and to permit persons to whom the Software is

 * furnished to do so, subject to the following conditions:

 *

 * The above copyright notice and this permission notice shall be included in all

 * copies or substantial portions of the Software.

 *

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS

 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,

 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE

 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER

 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,

 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE

 * SOFTWARE.

 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import net.md_5.bungee.api.plugin.Plugin;

public class ConfigAccessor {

        private final Plugin plugin;

        private final String fileName;
        private File configFile;

        private final Yaml yaml;

        @SuppressWarnings("rawtypes")
        private Map config;

        public ConfigAccessor(Plugin plugin, String fileName) {

                if (plugin == null)

                        throw new IllegalArgumentException("plugin cannot be null");

                this.plugin = plugin;

                this.fileName = fileName;

                DumperOptions options = new DumperOptions();
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                yaml = new Yaml(options);

                reloadConfig();
        }

        @SuppressWarnings("rawtypes")
        public void reloadConfig() {

                if (configFile == null) {
                        File dataFolder = plugin.getDataFolder();
                        if (dataFolder == null) {
                                throw new IllegalStateException();
                        }
                        configFile = new File(dataFolder, fileName);
                }

                InputStream defConfigStream = null;

                try {
                        if (configFile.exists()) {
                                defConfigStream = new FileInputStream(configFile);
                        } else {
                                // Look for defaults in the jar
                                defConfigStream = plugin.getResourceAsStream(fileName);
                        }

                        if (defConfigStream != null) {
                                config = (Map) yaml.load(defConfigStream);
                                if (config == null) {
                                        config = new HashMap();
                                }

                        }

                } catch (IOException e) {
                        // Never happens... i think...
                        e.printStackTrace();
                } finally {
                        if (defConfigStream != null) {
                                try {
                                        defConfigStream.close();
                                } catch (IOException e) {
                                        e.printStackTrace();
                                }
                        }
                }

        }

    private <T> T get(String path, T def)
    {
        return get( path, def, config );
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> T get(String path, T def, Map submap)
    {
        int index = path.indexOf( '.' );
        if ( index == -1 )
        {
            Object val = submap.get( path );
            if ( val == null && def != null )
            {
                val = def;
                submap.put( path, def );
            }
            return (T) val;
        } else
        {
            String first = path.substring( 0, index );
            String second = path.substring( index + 1, path.length() );
            Map sub = (Map) submap.get( first );
            if ( sub == null )
            {
                sub = new LinkedHashMap();
                submap.put( first, sub );
            }
            return get( second, def, sub );
        }
    }
        
        public int getInt(String path) {
                return get( path, null );
        }

        public String getString(String path) {
                return get( path, null );
        }

        public boolean getBoolean(String path)
    {
        return get( path, null );
    }
        
        public void saveDefaultConfig() {

                if (!configFile.exists()) {
                        InputStream inputStream = null;
                        OutputStream outputStream = null;
                        try {
                                
                                configFile.getParentFile().mkdirs();
                                configFile.createNewFile();
                                
                                inputStream = plugin.getResourceAsStream("config.yml");
                                outputStream = new FileOutputStream(configFile);

                                int read = 0;
                                byte[] bytes = new byte[1024];

                                while ((read = inputStream.read(bytes)) != -1) {
                                        outputStream.write(bytes, 0, read);
                                }

                        } catch (IOException e) {
                                e.printStackTrace();
                        } finally {
                                if (inputStream != null) {
                                        try {
                                                inputStream.close();
                                        } catch (IOException e) {
                                                e.printStackTrace();
                                        }
                                }
                                if (outputStream != null) {
                                        try {
                                                // outputStream.flush();
                                                outputStream.close();
                                        } catch (IOException e) {
                                                e.printStackTrace();
                                        }

                                }
                        }
                }

        }

}