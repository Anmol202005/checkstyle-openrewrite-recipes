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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.checkstyle.autofix.parser.CheckConfiguration;
import org.checkstyle.autofix.parser.CheckstyleViolation;
import org.checkstyle.autofix.parser.ConfigurationLoader;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

public class CheckstyleRecipeRegistryTest {

    @Test
    void testGetRecipesReturnsCorrectRecipe() {

        final List<CheckstyleViolation> violations = List.of(
                new CheckstyleViolation(5, 10, "error",
                        "com.puppycrawl.tools.checkstyle.checks.UpperEllCheck",
                        "Use uppercase 'L' for long literals.", "Example1.java"),

                new CheckstyleViolation(15, 20, "error",
                        "com.puppycrawl.tools.checkstyle.checks.UpperEllCheck",
                        "Use uppercase 'L' for long literals.", "Example2.java"),

                new CheckstyleViolation(8, 12, "error",
                        "header",
                        "Line does not match expected header line", "Example3.java")
        );
        final CheckConfiguration config = ConfigurationLoader.loadConfiguration(
                "src/test/resources/org/checkstyle/autofix/reciperegistry/config.xml",
                "src/test/resources/org/checkstyle/autofix/reciperegistry/check.properties");

        final List<Recipe> recipes = CheckstyleRecipeRegistry.getRecipes(violations, config);

        assertEquals(2, recipes.size(), "Should return one recipe");
        assertEquals("Header recipe", recipes.get(0).getDisplayName());
    }

}
