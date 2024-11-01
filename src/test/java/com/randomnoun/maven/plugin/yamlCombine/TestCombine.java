package com.randomnoun.maven.plugin.yamlCombine;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import junit.framework.Assert;
import junit.framework.TestCase;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

public class TestCombine extends  TestCase {

    private Charset inputFileSetCharset = UTF_8;

    public void setUp() { }
	
	// tests that $xrefs are resolved, and that $refs aren't
	public void test1() throws IOException {
		Log log = new SystemStreamLog();

		YamlCombiner sc = new YamlCombiner();
        sc.setRelativeDir(new File("src/test/resources/t1"));
        sc.setFiles(new String[] { "input.yaml" });
        sc.setLog(log);
        
		StringWriter w = new StringWriter();
        sc.combine(w, inputFileSetCharset);

        Path expectedPath = new File("src/test/resources/t1/expected-output.yaml").toPath();
        List<String> expectedLines = Files.readAllLines(expectedPath);
        
        List<String> lines = Arrays.asList(w.toString().split("\n"));
        
        Assert.assertEquals(expectedLines, lines);
	}
	
	// slightly different xref syntax ( # toggles /-escaping ) 
	// ( same output as t1 )
	public void test2() throws IOException {
		Log log = new SystemStreamLog();

		YamlCombiner sc = new YamlCombiner();
        sc.setRelativeDir(new File("src/test/resources/t2"));
        sc.setFiles(new String[] { "input.yaml" });
        sc.setLog(log);
        
		StringWriter w = new StringWriter();
        sc.combine(w, inputFileSetCharset);

        Path expectedPath = new File("src/test/resources/t2/expected-output.yaml").toPath();
        List<String> expectedLines = Files.readAllLines(expectedPath);
        
        List<String> lines = Arrays.asList(w.toString().split("\n"));

        Assert.assertEquals(expectedLines, lines);
	}
	
	// override keys in xref object
	public void test3() throws IOException {
		Log log = new SystemStreamLog();

		YamlCombiner sc = new YamlCombiner();
        sc.setRelativeDir(new File("src/test/resources/t3"));
        sc.setFiles(new String[] { "input.yaml" });
        // sc.setVerbose(true);
        sc.setLog(log);

        StringWriter w = new StringWriter();
        sc.combine(w, inputFileSetCharset);

        Path expectedPath = new File("src/test/resources/t3/expected-output.yaml").toPath();
        List<String> expectedLines = Files.readAllLines(expectedPath);
        
        List<String> lines = Arrays.asList(w.toString().split("\n"));
        
        Assert.assertEquals(expectedLines, lines);
	}
	
	// combine inputs from multiple input yamls
	public void test4() throws IOException {
		Log log = new SystemStreamLog();

		YamlCombiner sc = new YamlCombiner();
        sc.setRelativeDir(new File("src/test/resources/t4"));
        sc.setFiles(new String[] { "input.yaml", "input-2.yaml" });
        // sc.setVerbose(true);
        sc.setLog(log);

        StringWriter w = new StringWriter();
        sc.combine(w, inputFileSetCharset);
        System.out.println(w.toString());

        Path expectedPath = new File("src/test/resources/t4/expected-output.yaml").toPath();
        List<String> expectedLines = Files.readAllLines(expectedPath);
        
        List<String> lines = Arrays.asList(w.toString().split("\n"));

        Assert.assertEquals(expectedLines, lines);
	}

    // parse top level $xref to copy whole yaml
    public void test5() throws IOException {
        Log log = new SystemStreamLog();

        YamlCombiner sc = new YamlCombiner();
        sc.setRelativeDir(new File("src/test/resources/t5"));
        sc.setFiles(new String[] { "input.yaml" });
        sc.setLog(log);

        StringWriter w = new StringWriter();
        sc.combine(w, inputFileSetCharset);

        Path expectedPath = new File("src/test/resources/t5/expected-output.yaml").toPath();
        List<String> expectedLines = Files.readAllLines(expectedPath);

        List<String> lines = Arrays.asList(w.toString().split("\n"));

        Assert.assertEquals(expectedLines, lines);
    }

    public void test6() throws IOException {
        Log log = new SystemStreamLog();

        YamlCombiner sc = new YamlCombiner();
        sc.setRelativeDir(new File("src/test/resources/t6"));
        sc.setFiles(new String[] { "input.yaml", "input-2.yaml"  });
        sc.setLog(log);
        FileOutputStream fos = new FileOutputStream("target/t6.yaml");
        PrintWriter w = new PrintWriter(new OutputStreamWriter(fos, UTF_8));
        sc.combine(w, inputFileSetCharset);

        Path expectedPath = new File("src/test/resources/t6/expected-output.yaml").toPath();

        List<String> expectedLines = Files.readAllLines(expectedPath,UTF_8);

        Path actualPath = new File("target/t6.yaml").toPath();
        List<String> lines = Files.readAllLines(actualPath);
        for (int i = 0; i < expectedLines.size(); i++) {
            Assert.assertEquals(expectedLines.get(i),lines.get(i));
        }
        Assert.assertEquals(expectedLines, lines);
    }

    public void test7() throws IOException {
        Log log = new SystemStreamLog();

        YamlCombiner sc = new YamlCombiner();
        sc.setRelativeDir(new File("src/test/resources/t7"));
        sc.setFiles(new String[] { "input.yaml", "input-2.yaml"  });
        sc.setLog(log);
        FileOutputStream fos = new FileOutputStream("target/t7.yaml");
        PrintWriter w = new PrintWriter(new OutputStreamWriter(fos, ISO_8859_1));
        sc.combine(w, ISO_8859_1);

        Path expectedPath = new File("src/test/resources/t7/expected-output.yaml").toPath();

        List<String> expectedLines = Files.readAllLines(expectedPath,ISO_8859_1);

        Path actualPath = new File("target/t7.yaml").toPath();
        List<String> lines = Files.readAllLines(actualPath,ISO_8859_1);
        for (int i = 0; i < expectedLines.size(); i++) {
            Assert.assertEquals(expectedLines.get(i),lines.get(i));
        }
        Assert.assertEquals(expectedLines, lines);
    }

    public void test8() throws IOException {
        Log log = new SystemStreamLog();

        YamlCombiner sc = new YamlCombiner();
        sc.setRelativeDir(new File("src/test/resources/t8"));
        sc.setFiles(new String[] { "input.yaml", "input-2.yaml"  });
        sc.setLog(log);
        FileOutputStream fos = new FileOutputStream("target/t8.yaml");
        PrintWriter w = new PrintWriter(new OutputStreamWriter(fos, UTF_8));
        sc.combine(w, ISO_8859_1);

        Path expectedPath = new File("src/test/resources/t8/expected-output.yaml").toPath();

        List<String> expectedLines = Files.readAllLines(expectedPath,UTF_8);

        Path actualPath = new File("target/t8.yaml").toPath();
        List<String> lines = Files.readAllLines(actualPath,UTF_8);
        for (int i = 0; i < expectedLines.size(); i++) {
            Assert.assertEquals(expectedLines.get(i),lines.get(i));
        }
        Assert.assertEquals(expectedLines, lines);
    }
	
}
