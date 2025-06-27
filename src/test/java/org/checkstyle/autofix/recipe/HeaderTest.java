package org.checkstyle.autofix.recipe;

import org.checkstyle.autofix.parser.CheckstyleReportsParser;
import org.checkstyle.autofix.parser.CheckstyleViolation;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class HeaderTest extends AbstractRecipeTest {

    @Override
    protected Recipe getRecipe() throws IOException, XMLStreamException {
        final String reportPath = "src/test/resources/org/checkstyle/autofix/recipe/header"
                + "/report.xml";
        final String licensePath = "src/test/resources/org/checkstyle/autofix/recipe/header"
               + "/header.txt";
        final List<CheckstyleViolation> violations =
                CheckstyleReportsParser.parse(Path.of(reportPath));
        final String licenseText = Files.readString(Path.of(licensePath));
        return new Header(violations, licenseText);
    }

    @Test
    void hexOctalLiteralTest() throws IOException, XMLStreamException {
        testRecipe("header", "HeaderBlankLines");
    }
}
