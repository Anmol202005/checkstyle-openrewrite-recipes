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

package org.checkstyle.autofix.recipe;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.checkstyle.autofix.parser.CheckstyleRecord;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/**
 * Fixes Checkstyle UpperEll violations by replacing lowercase 'l' suffix
 * in long literals with uppercase 'L'.
 */
public class UpperEllRecipe extends Recipe {

    private List<CheckstyleRecord> violations;

    public UpperEllRecipe() {
        this.violations = new ArrayList<>();
    }

    public UpperEllRecipe(List<CheckstyleRecord> violations) {
        this.violations = violations;
    }

    @Override
    public String getDisplayName() {
        return "UpperEll recipe";
    }

    @Override
    public String getDescription() {
        return "Replace lowercase 'l' suffix in long literals with uppercase 'L' "
                + "to improve readability.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UpperEllVisitor(violations);
    }

    private static final class UpperEllVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final List<CheckstyleRecord> violations;
        private String fullSource;
        private String currentFileName;

        UpperEllVisitor(List<CheckstyleRecord> violations) {
            this.violations = violations;
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            this.fullSource = cu.printAll();
            this.currentFileName = cu.getSourcePath().toString();
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
            J.Literal result = super.visitLiteral(literal, ctx);
            final String valueSource = result.getValueSource();

            if (valueSource != null && valueSource.endsWith("l")
                    && result.getType() == JavaType.Primitive.Long
                    && isAtViolationLocation(valueSource)) {

                final String numericPart = valueSource.substring(0, valueSource.length() - 1);
                result = result.withValueSource(numericPart + "L");
            }

            return result;
        }

        private boolean isAtViolationLocation(String literalText) {
            final int literalIndex = fullSource.indexOf(literalText);

            final String textBeforeLiteral = fullSource.substring(0, literalIndex);
            final int line = 1 + (int) textBeforeLiteral.chars()
                    .filter(character -> character == '\n').count();

            return violations.stream().anyMatch(violation -> {

                final Path violationPath = Path.of(violation.getFileName());
                final Path currentPath = Path.of(currentFileName);

                return violation.getLine() == line
                        && violationPath.equals(currentPath);
            });
        }
    }
}
