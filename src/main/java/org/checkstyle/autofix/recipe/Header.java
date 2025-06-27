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

import org.checkstyle.autofix.parser.CheckstyleViolation;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class Header extends Recipe {

    private List<CheckstyleViolation> violations;
    private String licenseText;

    public Header() {
        this.violations = new ArrayList<>();
        this.licenseText = "";
    }

    public Header(List<CheckstyleViolation> violations, String licenseText) {
        this.violations = violations;
        this.licenseText = licenseText;
    }

    @Override
    public String getDisplayName() {
        return "Add license header";
    }

    @Override
    public String getDescription() {
        return "Adds license headers to Java source files when missing. Does not override existing license headers.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J visit(Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                    if (cu.getComments().isEmpty() && isAtViolationLocation(cu.getSourcePath().toString())) {
                        cu = cu.withPrefix(Space.format(licenseText + "\n"));
                    }
                    return super.visit(cu, ctx);
                }
                return super.visit(tree, ctx);
            }
        };
    }
    private boolean isAtViolationLocation(String currentFileName) {

        return violations.stream().anyMatch(violation -> {
            return violation.getLine() == 1
                    && Objects.isNull(violation.getColumn())
                    && Path.of(violation.getFileName()).equals(Path.of(currentFileName));
        });
    }

}
