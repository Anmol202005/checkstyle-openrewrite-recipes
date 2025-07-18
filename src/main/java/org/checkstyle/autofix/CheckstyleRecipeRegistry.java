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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkstyle.autofix.parser.CheckConfiguration;
import org.checkstyle.autofix.parser.CheckstyleViolation;
import org.checkstyle.autofix.recipe.Header;
import org.checkstyle.autofix.recipe.UpperEll;
import org.openrewrite.Recipe;

public final class CheckstyleRecipeRegistry {

    private static final Map<String, Function<List<CheckstyleViolation>, Recipe>> RECIPE_MAP =
            new HashMap<>();
    private static final Map<String, BiFunction<List<CheckstyleViolation>, CheckConfiguration,
                Recipe>> RECIPE_MAP_WITH_CONFIG =
            new HashMap<>();

    static {
        RECIPE_MAP.put("UpperEllCheck", UpperEll::new);
        RECIPE_MAP_WITH_CONFIG.put("header", Header::new);
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
     * @param config the checkstyle configuration
     * @return a list of generated Recipe objects
     */
    public static List<Recipe> getRecipes(List<CheckstyleViolation> violations,
                                          CheckConfiguration config) {
        return violations.stream()
                .collect(Collectors.groupingBy(CheckstyleViolation::getSource))
                .entrySet()
                .stream()
                .map(entry -> createRecipe(entry, config))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Recipe createRecipe(Map.Entry<String, List<CheckstyleViolation>> entry,
                                       CheckConfiguration config) {
        final String simpleCheckName =
                entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1);
        final List<CheckstyleViolation> violations = entry.getValue();

        Recipe recipe = null;
        final BiFunction<List<CheckstyleViolation>, CheckConfiguration, Recipe> configFactory =
                RECIPE_MAP_WITH_CONFIG.get(simpleCheckName);
        if (configFactory != null) {
            recipe = configFactory.apply(violations,
                    extractCheckConfiguration(config, simpleCheckName));
        }
        else {
            final Function<List<CheckstyleViolation>, Recipe> factory =
                    RECIPE_MAP.get(simpleCheckName);
            if (factory != null) {
                recipe = factory.apply(violations);
            }
        }

        return recipe;
    }

    private static CheckConfiguration extractCheckConfiguration(CheckConfiguration config,
                                                           String checkName) {
        return Stream.concat(
                        config.getChildren().stream()
                                .filter(child -> isCheckNameMatch(child.getName(), checkName)),
                        config.getChildren().stream()
                                .filter(child -> "TreeWalker".equals(child.getName()))
                                .flatMap(treeWalker -> treeWalker.getChildren().stream())
                                .filter(child -> isCheckNameMatch(child.getName(), checkName))
                )
                .findFirst()
                .orElseThrow(() -> {
                    return new IllegalArgumentException(checkName + " configuration not "
                            + "found");
                });
    }

    private static boolean isCheckNameMatch(String configName, String checkName) {
        String normalizedCheckName = checkName;
        final int checkLength = 5;
        if (checkName.toLowerCase().endsWith("check")) {
            normalizedCheckName = checkName.substring(0, checkName.length() - checkLength);
        }
        return normalizedCheckName.equalsIgnoreCase(configName);
    }

}
