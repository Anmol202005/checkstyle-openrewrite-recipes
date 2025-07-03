///////////////////////////////////////////////////////////////////////////////////////////////
// checkstyle-openrewrite-recipes: Automatically fix Checkstyle violations with OpenRewrite.
// Copyright (C) 2025 The Checkstyle OpenRewrite Recipes Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
///////////////////////////////////////////////////////////////////////////////////////////////

package org.checkstyle.autofix;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.checkstyle.autofix.parser.CheckstyleReportParser;
import org.checkstyle.autofix.parser.CheckstyleViolation;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;

/**
 * Main recipe that automatically fixes all supported Checkstyle violations.
 */
public class CheckstyleAutoFix extends Recipe {

    @Option(displayName = "Violation report path",
            description = "Path to the checkstyle violation report XML file.",
            example = "target/checkstyle/checkstyle-report.xml")
    private String violationReportPath;

    @Option(displayName = "Checkstyle config path",
            description = "Path to the file containing Checkstyle configuration.",
            example = "config/checkstyle.xml")
    private String checkstyleConfigurationPath;

    @Option(displayName = "Checkstyle properties file path",
            description = "Path to the file containing the Checkstyle Properties.",
            example = "config/checkstyle.properties")
    private String propertiesPath;

    private Configuration cachedConfig;

    public CheckstyleAutoFix() {
        // Constructor is now clean
    }

    @Override
    public String getDisplayName() {
        return "Checkstyle autoFix";
    }

    @Override
    public String getDescription() {
        return "Automatically fixes Checkstyle violations.";
    }

    public String getViolationReportPath() {
        return violationReportPath;
    }

    public String getCheckstyleConfigurationPath() {
        return checkstyleConfigurationPath;
    }

    public String getPropertiesPath() {
        return propertiesPath;
    }

    @Override
    public List<Recipe> getRecipeList() {
        try {
            final Configuration config = loadCheckstyleConfiguration();

            final List<CheckstyleViolation> violations = CheckstyleReportParser
                    .parse(Path.of(getViolationReportPath()));

            return CheckstyleRecipeRegistry.getRecipes(violations, config);

        }
        catch (CheckstyleException | IOException exception) {
            throw new IllegalArgumentException("Failed to load Checkstyle"
                    + " configuration or parse violations", exception);
        }
    }

    private Configuration loadCheckstyleConfiguration() throws CheckstyleException, IOException {
        final Configuration config;

        if (cachedConfig == null) {
            final Properties props = new Properties();
            final String propFile = getPropertiesPath();

            try (FileInputStream input = new FileInputStream(propFile)) {
                props.load(input);
            }
            catch (FileNotFoundException exception) {
                throw new IllegalArgumentException("Failed to read: " + propFile, exception);
            }

            final String configPath = getCheckstyleConfigurationPath();

            if (isRemoteUrl(configPath)) {
                config = ConfigurationLoader.loadConfiguration(
                        configPath,
                        new PropertiesExpander(props)
                );
            }
            else {
                final File configFile = new File(configPath);
                config = ConfigurationLoader.loadConfiguration(
                        configFile.toURI().toString(),
                        new PropertiesExpander(props)
                );
            }

            cachedConfig = config;
        }

        return cachedConfig;
    }

    private boolean isRemoteUrl(String path) {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }

}
