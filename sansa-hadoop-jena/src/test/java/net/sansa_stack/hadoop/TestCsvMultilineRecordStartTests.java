package net.sansa_stack.hadoop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aksw.commons.model.csvw.domain.impl.DialectMutableImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import net.sansa_stack.hadoop.core.pattern.CustomMatcher;
import net.sansa_stack.hadoop.core.pattern.CustomPattern;
import net.sansa_stack.hadoop.core.pattern.CustomPatternCsv;

@RunWith(Parameterized.class)
public class TestCsvMultilineRecordStartTests {

    @Parameterized.Parameters(name = "{index}: test {0}")
    public static Iterable<?> data() {
        // int MAX_COLUMN_LENGTH = 300000;

        // CustomPattern excelPattern = CustomPatternCsv.create(CustomPatternCsv.Config.createExcel(MAX_COLUMN_LENGTH));
        CustomPattern excelPattern = CustomPatternCsv.create(DialectMutableImpl.create()
                // setQuoteChar("\"").setQuoteEscapeChar("\"").setDelimiter(",").setLineTerminatorList(Arrays.asList("\n", "\r\n", "\n\r", "\r\n\r"))
                , 1000, Integer.MAX_VALUE);

        // expected: 4, 8, 35
        CsvTestCase excel1 = CsvTestCase.create("csv-excel-1", excelPattern, String.join("\n",
                        "a,b",
                        "x,y",
                        "\"\"\"this\nis\nmultiline\"\"\", d",
                        "e,f"),
                4, 8, 35);

        // expected 4, 8, 18
        CsvTestCase excel2 = CsvTestCase.create("csv-excel-2", excelPattern, String.join("\n",
                        "a,b",
                        "x,y",
                        "\"\"\"\"\"\", d",
                        "e,f"),
                4, 8, 18);

        // 17
        CsvTestCase excel3 = CsvTestCase.create("csv-excel-3", excelPattern, String.join("\n",
                        "a,b",
                        "x,y",
                        "\"\"\"\"\", d",
                        "e,f"),
                17);

        // Single line - there is no character after a multiline so no match position is returned
        CsvTestCase excel4 = CsvTestCase.create("csv-excel-4", excelPattern, String.join("\n",
                "a,b")
                // No match positions
                );

        // Single line - there is no character after a multiline so no match position is returned
        CsvTestCase excel5 = CsvTestCase.create("csv-excel-5", excelPattern, String.join("\n",
                "a,\"\",\"\",b",
                "c,\"\",\"\"\"\",d",
                "e,\"\",\"\",f"),
                // No match positions
                10, 22);


        // escape with backslash
        // CustomPattern customPatternA = CustomPatternCsv2.create(CustomPatternCsvOld.Config.create('"', '\\', MAX_COLUMN_LENGTH));
        CustomPattern customPatternA = CustomPatternCsv.create(DialectMutableImpl.create().setQuoteEscapeChar("\\"), 1000, Integer.MAX_VALUE);

        CsvTestCase customA1 = CsvTestCase.create("csv-a1", customPatternA, String.join("\n",
                        "a,b",
                        "x,y",
                        "d,\"this\nis\nmult\\\",\niline\"",
                        "e,f"), // pos 34 is the 'e'
                4, 8, 34);


        List<CsvTestCase> result = Arrays.asList(
                excel1,
                excel2,
                excel3,
                excel4,
                excel5,
                customA1);

        return result;
    }


    protected CsvTestCase testCase;

    public TestCsvMultilineRecordStartTests(CsvTestCase testCase) {
        this.testCase = testCase;
    }

    @Test
    public void test() {
        String input = testCase.getInput();
        CustomPattern pattern = testCase.getPattern();
        List<Integer> expectedMatchPositions = testCase.getExpectedMatchPositions();

        CustomMatcher m = pattern.matcher(input);
        List<Integer> actualMatchPositions = new ArrayList<>();
        while (m.find()) {
            int actualMatch = m.start();
            // if (actualMatch == 0) continue;
            actualMatchPositions.add(actualMatch);
            // System.out.println("newline at pos: " + actualMatch + " --- group: " + m.group());
        }

        Assert.assertEquals(expectedMatchPositions, actualMatchPositions);
    }

    public static class CsvTestCase {
        protected String name;
        protected CustomPattern pattern;
        protected String input;
        protected List<Integer> expectedMatchPositions;

        public CsvTestCase(String name, CustomPattern pattern, String input, List<Integer> expectedMatchPositions) {
            this.name = name;
            this.pattern = pattern;
            this.input = input;
            this.expectedMatchPositions = expectedMatchPositions;
        }

        public static CsvTestCase create(String name, CustomPattern pattern, String input, Integer ... expectedMatchPositions) {
            return new CsvTestCase(name, pattern, input, Arrays.asList(expectedMatchPositions));
        }

        public String getName() {
            return name;
        }

        public CustomPattern getPattern() {
            return pattern;
        }

        public String getInput() {
            return input;
        }

        public List<Integer> getExpectedMatchPositions() {
            return expectedMatchPositions;
        }

        @Override
        public String toString() {
            return name;
        }
    }


/*

    public static CustomPattern createPattern() {
        CustomPattern result;
        // A csv record could look like
        // foo,"""foo->
        // bar"
        // baz,bay
        int cand = 3;
        switch (cand) {
            case 0:
                return CustomPatternJava.compile("(?<=\n(?!((?<![^\"]\"[^\"]).){0,50000}\"(\r?\n|,|$))).",
                        Pattern.DOTALL);

            // when going back from the quote char before the cell delimiters [,\n$]
            //
            case 1: {
                // Match an effective quote: A quote that is preceeded by an even number of quotes
                String eQuote = "[^\"](\"\"){0,10}\"";

                // Match up to n characters not preceeded by an effective quote
                String noLeadingEQuotedChar = "((?<!${eQuote}(?!\")).){0,50000}";

                // Match a character following a newline but only if the following there is
                // no effective quote that is NOT preceeded by an effective quote
                String matchCharAfterNewline = "(?<=\n(?!${noLeadingEQuotedChar}${eQuote}(\r?\n|,|$))).";
//              String matchCharAfterNewline = "(?<=\n(?!${noLeadingEQuotedChar}${eQuote}(\r?\n|,|$))).";

                String complete = matchCharAfterNewline
                        .replace("${noLeadingEQuotedChar}", noLeadingEQuotedChar)
                        .replace("${eQuote}", eQuote);

                // System.out.println("complete:" + complete);

                return CustomPatternJava.compile(
                        // There must not be an unescaped quote char before line limiter
                        // without a prior unescaped quote char
                        //"(?<=(\n|^)(?!((?<![^\"](\"\"){0,10}\"(?!\")).){0,50000}[^\"](\"\"){0,10}\"(\r?\n|,|$))).",
                        complete,
                        Pattern.DOTALL);
*/
/*
                return Pattern.compile(
                        "(?<=\n(?!((?<!(?<![^\"](\"\"){0,10}\")).){0," + maxCharsPerColumn + "}\"(\r?\n|,|$))).",
                        Pattern.DOTALL);
*//*

            }
            case 2: {
                // Match the first quote in a sequence of quotes:
                // A quote that is
                // - not preceded by a quote
                // - is followed by an even number of quotes followed by a non-quote char or end-of-line
                String equoteFirst = "((?<!\")\"(?=(\"\"){0,10}([^\"]|$)))";
                String equoteLast = "((?<=(^|[^\"])(\"\"){0,10})\"(?!\"))";

                // A character not preceded by an effective quote
                String unequotedChar = "((?<!${equoteLast}).)";

                // Match a character following a newline but only if the following there is
                // no effective quote that is NOT preceeded by an effective quote
                String matchCharAfterNewline = "(?<=\n(?!(?<!${equoteLast}).{0,50000}${equoteFirst}(\r?\n|,|$))).";
//              String matchCharAfterNewline = "(?<=\n(?!${noLeadingEQuotedChar}${eQuote}(\r?\n|,|$))).";

                String complete = matchCharAfterNewline
                        .replace("${unequotedChar}", unequotedChar)
                        .replace("${equoteFirst}", equoteFirst)
                        .replace("${equoteLast}", equoteLast)
                        ;

                // System.out.println("complete:" + complete);

                return CustomPatternJava.compile(
                        // There must not be an unescaped quote char before line limiter
                        // without a prior unescaped quote char
                        //"(?<=(\n|^)(?!((?<![^\"](\"\"){0,10}\"(?!\")).){0,50000}[^\"](\"\"){0,10}\"(\r?\n|,|$))).",
                        complete,
                        Pattern.DOTALL);
            }
            case 3: return CustomPatternCsv.create(CustomPatternCsv.Config.createExcel());
            default:
                return null;
        }
    }
    @Test
    public void test1() {

        */
/*
        Pattern testPattern = Pattern.compile("\n");
        Matcher testMatcher = testPattern.matcher("abcd");
        System.out.println(testMatcher.group());
         *//*

        int i = 0;
        for (String input : inputs) {
            System.out.println("input #" + i++);
            CustomMatcher m = pattern.matcher(input);
            while (m.find()) {
                System.out.println("newline at pos: " + m.start() + " --- group: " + m.group());
            }
        }
    }
*/
}
