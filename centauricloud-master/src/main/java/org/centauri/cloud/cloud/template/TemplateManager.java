package org.centauri.cloud.cloud.template;

import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.centauri.cloud.cloud.Cloud;
import org.centauri.cloud.cloud.profiling.CentauriProfiler;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateManager {

	@Getter
	private Set<Template> templates = new HashSet<>();

	private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z0-9]*$");

	public void removeTemplate(String name) throws Exception {
		Template template = this.getTemplate(name);
		if (template == null) {
			Cloud.getLogger().warn("Cannot find template {}!", name);
			return;
		}

		FileUtils.deleteDirectory(template.getDir());
		this.templates.remove(template);
		Cloud.getLogger().info("Removed template {}!", name);
	}

	public void loadTemplate(String name) throws Exception {
		Matcher matcher = PATTERN.matcher(name);
		if (!matcher.matches())
			throw new UnsupportedOperationException("Wrong name");

		CentauriProfiler.Profile profile = Cloud.getInstance().getProfiler().start("TemplateManager_loadTemplate_" + name);

		File templateDir = new File(Cloud.getInstance().getTemplatesDir(), name + "/");
		Template template = new Template(name, templateDir, new File(templateDir, "/centauricloud.yml"));
		template.getDir().mkdir();

		//creates a template if not exists
		if (!template.getConfig().exists()) {
			Files.copy(this.getClass().getResourceAsStream("/centauricloud.yml"), template.getConfig().toPath());
			Cloud.getLogger().info("Created Template {}!", name);
		}

		template.loadConfig();
		template.loadSharedFiles();

		this.templates.add(template);

		Cloud.getInstance().getProfiler().stop(profile);
	}

	public Template getTemplate(String name) {
		return templates.stream().filter(template -> template.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}

	public void importAllTemplates() {
		Cloud.getLogger().info("Autoload all templates...");
		for (File templateDir : Cloud.getInstance().getTemplatesDir().listFiles()) {
			if (templateDir.isDirectory()) {
				try {
					if (PATTERN.matcher(templateDir.getName()).matches()) {
						this.loadTemplate(templateDir.getName());
						this.getTemplate(templateDir.getName()).compress();
					}
				} catch (Exception ex) {
					Cloud.getLogger().catching(ex);
				}
			}
		}
		Cloud.getLogger().info("Finished loading all templates!");
	}

}