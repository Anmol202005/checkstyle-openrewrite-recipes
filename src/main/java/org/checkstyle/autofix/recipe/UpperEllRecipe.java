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
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Function;

import org.checkstyle.autofix.parser.CheckstyleRecord;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.RecipeRunException;
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
        private String currentFileName;

        UpperEllVisitor(List<CheckstyleRecord> violations) {
            this.violations = violations;
        }

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            this.currentFileName = cu.getSourcePath().toString();
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
            J.Literal result = super.visitLiteral(literal, ctx);
            final String valueSource = result.getValueSource();

            if (valueSource != null && valueSource.endsWith("l")
                    && result.getType() == JavaType.Primitive.Long
                    && isAtViolationLocation(result)) {

                final String numericPart = valueSource.substring(0, valueSource.length() - 1);
                result = result.withValueSource(numericPart + "L");
            }

            return result;
        }

        private boolean isAtViolationLocation(J.Literal literal) {
            final J.CompilationUnit cursor = Objects.requireNonNull(getCursor()
                    .firstEnclosing(J.CompilationUnit.class));

            final int line = computeLinePosition(cursor, literal, getCursor());
            final int column = computeColumnPosition(cursor, literal, getCursor());

            return violations.stream().anyMatch(violation -> {
                return violation.getLine() == line
                        && violation.getColumn() == column
                        && Path.of(violation.getFileName()).equals(Path.of(currentFileName));
            });
        }

        private int computePosition(
                J tree,
                J targetElement,
                Cursor cursor,
                Function<String, Integer> positionCalculator
        ) {
            final TreeVisitor<?, PrintOutputCapture<TreeVisitor<?, ?>>> printer =
                    tree.printer(cursor);

            final PrintOutputCapture<TreeVisitor<?, ?>> capture =
                    new PrintOutputCapture<>(printer) {
                        @Override
                        public PrintOutputCapture<TreeVisitor<?, ?>> append(String text) {
                            if (targetElement.isScope(getContext().getCursor().getValue())) {
                                throw new CancellationException();
                            }
                            return super.append(text);
                        }
                    };

            final int result;
            try {
                printer.visit(tree, capture, cursor.getParentOrThrow());
                throw new IllegalStateException("Target element not found in tree");
            }
            catch (CancellationException ignored) {
                result = positionCalculator.apply(capture.getOut());
            }
            catch (RecipeRunException exception) {
                if (exception.getCause() instanceof CancellationException) {
                    result = positionCalculator.apply(capture.getOut());
                }
                else {
                    throw exception;
                }
            }
            return result;
        }

        private int computeLinePosition(J tree, J targetElement, Cursor cursor) {
            return computePosition(tree, targetElement, cursor,
                    out -> 1 + (int) out.chars().filter(chr -> chr == '\n').count());
        }

        private int computeColumnPosition(J tree, J targetElement, Cursor cursor) {
            return computePosition(tree, targetElement, cursor, this::calculateColumnOffset);
        }

        private int calculateColumnOffset(String out) {
            final int lineBreakIndex = out.lastIndexOf('\n');
            final int result;
            if (lineBreakIndex == -1) {
                result = out.length();
            }
            else {
                result = out.length() - lineBreakIndex;
            }
            return result;
        }

    }
}
