package com.ca.commons.cbutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Theme {
	
	private static Theme instance;
	
	public static Theme getInstance() {
		if (instance == null) {
			instance = new Theme();
		}
		return instance;
	}
	
	private String dirHtmlDocs;
	private String dirIcons;
	private String dirImages;
	private String dirTemplates;
	
	private Theme() { 
        String localDir = System.getProperty("user.dir") + File.separator;
        String theme = findActiveTheme(localDir);
        init(localDir, theme);
	}
	
	private String findActiveTheme(String localDir) {
        Properties themeProp = new Properties();
        try {
            themeProp.load(new FileInputStream(localDir + "themes" + File.separator + "themes.properties"));
        } catch (IOException e) { }
        String theme = themeProp.getProperty("settings.theme", "classic-theme");
		return theme;
	}
	
	private void init(String localDir, String theme) {
        dirHtmlDocs = localDir + "themes" + File.separator + theme + File.separator + "htmldocs" + File.separator;
        dirIcons = localDir + "themes" + File.separator + theme + File.separator + "icons" + File.separator;
        dirImages = localDir + "themes" + File.separator + theme + File.separator + "images" + File.separator; 
        dirTemplates = localDir + "themes" + File.separator + theme + File.separator + "templates" + File.separator;
	}
	
	public String getDirHtmlDocs() {
		return dirHtmlDocs;
	}

	public String getDirIcons() {
		return dirIcons;
	}

	public String getDirImages() {
		return dirImages;
	}

	public String getDirTemplates() {
		return dirTemplates;
	}

}
