package com.randomnoun.maven.plugin.swaggerCombine;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestCombine extends  TestCase {

	public void setUp() { }
	
	// tests that $xrefs are resolved, and that $refs aren't
	public void test1() throws IOException {
		Log log = new SystemStreamLog();

		SwaggerCombiner sc = new SwaggerCombiner();
        sc.setRelativeDir(new File("src/test/resources/t1"));
        sc.setFiles(new String[] { "input.yaml" });
        sc.setLog(log);
        
		StringWriter w = new StringWriter();
        sc.combine(w);

        Path expectedPath = new File("src/test/resources/t1/expected-output.yaml").toPath();
        List<String> expectedLines = Files.readAllLines(expectedPath);
        
        List<String> lines = Arrays.asList(w.toString().split("\n"));

        Assert.assertEquals(expectedLines, lines);
	}
	
	// slightly different xref syntax ( # toggles /-escaping ) 
	// ( same output as t1 )
	public void test2() throws IOException {
		Log log = new SystemStreamLog();

		SwaggerCombiner sc = new SwaggerCombiner();
        sc.setRelativeDir(new File("src/test/resources/t2"));
        sc.setFiles(new String[] { "input.yaml" });
        sc.setLog(log);
        
		StringWriter w = new StringWriter();
        sc.combine(w);

        Path expectedPath = new File("src/test/resources/t2/expected-output.yaml").toPath();
        List<String> expectedLines = Files.readAllLines(expectedPath);
        
        List<String> lines = Arrays.asList(w.toString().split("\n"));

        Assert.assertEquals(expectedLines, lines);
	}
	
	// override keys in xref object
	public void test3() throws IOException {
		Log log = new SystemStreamLog();

		SwaggerCombiner sc = new SwaggerCombiner();
        sc.setRelativeDir(new File("src/test/resources/t3"));
        sc.setFiles(new String[] { "input.yaml" });
        // sc.setVerbose(true);
        sc.setLog(log);

        StringWriter w = new StringWriter();
        sc.combine(w);

        Path expectedPath = new File("src/test/resources/t3/expected-output.yaml").toPath();
        List<String> expectedLines = Files.readAllLines(expectedPath);
        
        List<String> lines = Arrays.asList(w.toString().split("\n"));
        
        Assert.assertEquals(expectedLines, lines);
	}
	
	// combine inputs from multiple input yamls
	public void test4() throws IOException {
		Log log = new SystemStreamLog();

		SwaggerCombiner sc = new SwaggerCombiner();
        sc.setRelativeDir(new File("src/test/resources/t4"));
        sc.setFiles(new String[] { "input.yaml", "input-2.yaml" });
        // sc.setVerbose(true);
        sc.setLog(log);

        StringWriter w = new StringWriter();
        sc.combine(w);


        Path expectedPath = new File("src/test/resources/t4/expected-output.yaml").toPath();
        List<String> expectedLines = Files.readAllLines(expectedPath);
        
        List<String> lines = Arrays.asList(w.toString().split("\n"));

        Assert.assertEquals(expectedLines, lines);
	}
	
	
	
}
