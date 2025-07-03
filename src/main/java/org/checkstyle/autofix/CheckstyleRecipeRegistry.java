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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.checkstyle.autofix.parser.CheckstyleViolation;
import org.checkstyle.autofix.recipe.Header;
import org.checkstyle.autofix.recipe.UpperEll;
import org.openrewrite.Recipe;

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;

public final class CheckstyleRecipeRegistry {

    private static final Map<String, Function<List<CheckstyleViolation>, Recipe>> RECIPE_MAP =
            new HashMap<>();

    private static final String HEADER_LITERAL = "header";

    static {
        RECIPE_MAP.put("UpperEllCheck", UpperEll::new);
    }

    private CheckstyleRecipeRegistry() {
        // utility class
    }

    /**
     * Returns a list of Recipe objects based on the given list of Checkstyle violations.
     * The method groups violations by their check name, finds the matching recipe factory
     * using the simple name of the check, and applies the factory to generate Recipe instances.
     *
     * @param violations the list of Checkstyle violations
     * @param config the Checkstyle configuration
     * @return a list of generated Recipe objects
     */
    public static List<Recipe> getRecipes(List<CheckstyleViolation> violations,
                                          Configuration config) {

        final Map<String, List<CheckstyleViolation>> violationsByCheck = violations.stream()
                .collect(Collectors.groupingBy(CheckstyleViolation::getSource));

        final List<Recipe> recipes = new ArrayList<>();

        for (Map.Entry<String, List<CheckstyleViolation>> entry : violationsByCheck.entrySet()) {
            final String checkName = entry.getKey();
            final String simpleCheckName = checkName
                    .substring(checkName.lastIndexOf('.') + 1);
            final List<CheckstyleViolation> checkViolations = entry.getValue();

            final Function<List<CheckstyleViolation>, Recipe> recipeFactory =
                    RECIPE_MAP.get(simpleCheckName);
            if (recipeFactory != null) {
                recipes.add(recipeFactory.apply(checkViolations));
            }

            else if (HEADER_LITERAL.equals(simpleCheckName)) {
                final String headerContent = extractHeaderContent(config);
                recipes.add(new Header(checkViolations, headerContent));
            }
        }

        return recipes;
    }

    private static String readHeaderFileContent(String headerFilePath) {
        try {
            return Files.readString(Path.of(headerFilePath));
        }
        catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read: " + headerFilePath, exception);
        }
    }

    private static String extractHeaderContent(Configuration config) {
        return Arrays.stream(config.getChildren())
                .filter(child -> "Header".equals(child.getName()))
                .findFirst()
                .map(CheckstyleRecipeRegistry::getHeaderFromChild)
                .orElseThrow(() -> new IllegalArgumentException("Header configuration not found"));
    }

    private static String getHeaderFromChild(Configuration child) {
        final String result;
        final String[] propertyNames = child.getPropertyNames();
        final boolean hasHeaderProperty = Arrays.asList(propertyNames).contains(HEADER_LITERAL);
        try {
            if (hasHeaderProperty) {
                result = child.getProperty(HEADER_LITERAL);
            }
            else {
                final String headerFile = child.getProperty("headerFile");
                result = readHeaderFileContent(headerFile);
            }
        }
        catch (CheckstyleException exception) {
            throw new IllegalArgumentException("Failed to extract header from config", exception);
        }
        return result;
    }

}
