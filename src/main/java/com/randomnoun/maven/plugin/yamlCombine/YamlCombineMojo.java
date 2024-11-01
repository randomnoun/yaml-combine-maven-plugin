package com.randomnoun.maven.plugin.yamlCombine;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.DefaultMavenFileFilter;
import org.apache.maven.shared.filtering.FilterWrapper;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.utils.io.DirectoryScanner;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLoggerManager;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Maven goal which combines a bunch of yaml files into a big yaml file.
 * 
 * @blog http://www.randomnoun.com/wp/2021/06/29/swagger-combine/
 */
@Mojo (name = "yaml-combine", defaultPhase = LifecyclePhase.GENERATE_SOURCES)

public class YamlCombineMojo
    extends AbstractMojo
{
	// FileSet handling from 
	// https://github.com/apache/maven-jar-plugin/blob/maven-jar-plugin-3.2.0/src/main/java/org/apache/maven/plugins/jar/AbstractJarMojo.java

    /**
     * The input files to combine. Files that are included via <code>$xref</code> references do not need to be included in this fileset.
     */
    @Parameter( property="fileset")
    private FileSet fileset;

	@Parameter( defaultValue = "UTF-8", required = false )
	private String fileSetEncoding;
    
    /**
     * Directory containing the generated JAR.
     */
    @Parameter( defaultValue = "${project.build.directory}", required = true )
    private File outputDirectory;



	@Parameter( defaultValue = "UTF-8", required = false )
	private String outputFileEncoding;

    /**
     * Name of the generated JAR.
     */
    @Parameter( defaultValue = "${project.build.finalName}", readonly = true )
    private String finalName;

    /**
     * True to filter files before processing, in the maven copy-resources sense ( i.e. perform variable substitution ).
     */
    // Because that needed a new verb obviously.
    @Parameter( property="filtering", defaultValue = "false", required = true )
    private boolean filtering;

    /**
     * True to increase logging
     */
    @Parameter( property="verbose", defaultValue = "false", required = true )
    private boolean verbose;
    
    
    
    /**
     * The {@link {MavenProject}.
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    /**
     * The {@link MavenSession}.
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    private MavenSession session;

    
    /** @component */
    private BuildContext buildContext;
    
    
    /**
     * @parameter property="settings"
     * @required
     * @since 1.0
     * @readonly
     */
    private Settings settings;


    /**
     *
     */
    @org.apache.maven.plugins.annotations.Component
    private MavenProjectHelper projectHelper;

    
    // private method in FileSetManager

	private DirectoryScanner scan(FileSet fileSet) {
		File basedir = new File(fileSet.getDirectory());
		if (!basedir.exists() || !basedir.isDirectory()) {
			return null;
		}

		DirectoryScanner scanner = new DirectoryScanner();

		String[] includesArray = fileSet.getIncludesArray();
		String[] excludesArray = fileSet.getExcludesArray();

		if (includesArray.length > 0) {
			scanner.setIncludes(includesArray);
		}

		if (excludesArray.length > 0) {
			scanner.setExcludes(excludesArray);
		}

		if (fileSet.isUseDefaultExcludes()) {
			scanner.addDefaultExcludes();
		}

		scanner.setBasedir(basedir);
		scanner.setFollowSymlinks(fileSet.isFollowSymlinks());

		scanner.scan();

		return scanner;
	}
	
	

    
    /**
     * Generates the JAR.
     * 
     * @return The instance of File for the created archive file.
     * @throws MojoExecutionException in case of an error.
     */
	public File createCombinedYaml() throws MojoExecutionException {
		if (outputDirectory == null) {
			throw new IllegalArgumentException("Missing outputDirectory");
		}
		if (finalName == null) {
			throw new IllegalArgumentException("Missing finalName");
		}
		File destFile = new File(outputDirectory, finalName);
		Charset outputFileCharset = Charset.forName(outputFileEncoding);
		Charset inputFileSetCharset = Charset.forName(fileSetEncoding);

		// filesets aren't order-preserving.
		// which is kind of annoying. could sort the Files just to get some
		// deterministic order I suppose

		if (destFile.exists() && destFile.isDirectory()) {
			throw new MojoExecutionException(destFile + " is a directory.");
		}
		if (destFile.exists() && !destFile.canWrite()) {
			throw new MojoExecutionException(destFile + " is not writable.");
		}

		getLog().info("Creating combined yaml file " + destFile.getAbsolutePath());

		if (!destFile.getParentFile().exists()) {
			// create the parent directory...
			if (!destFile.getParentFile().mkdirs()) {
				// Failure, unable to create specified directory for some unknown reason.
				throw new MojoExecutionException("Unable to create directory or parent directory of " + destFile);
			}
		}
		if (destFile.exists()) {
			// delete existing
			if (!destFile.delete()) {
				throw new MojoExecutionException("Unable to delete existing file " + destFile);
			}
		}
		

		DirectoryScanner scanner = scan(fileset);
		String[] files = scanner.getIncludedFiles(); // also performs exclusion. So way to go, maven.

		// let's just have a single output file then.
		FileOutputStream fos = null;
		try {
			YamlCombiner sc = new YamlCombiner();
			sc.setRelativeDir(new File(fileset.getDirectory()));
			sc.setFiles(files);
			sc.setLog(getLog());
			sc.setVerbose(verbose);
			
			fos = new FileOutputStream(destFile);
			if (filtering) {
				MavenResourcesExecution mre = new MavenResourcesExecution();
		        mre.setMavenProject( project );
		        mre.setFileFilters( null );
		        mre.setEscapeWindowsPaths( true );
		        mre.setMavenSession( session );
		        mre.setInjectProjectBuildFilters( true );
		        
		        // there's probably a better way of getting a plexus Logger instance
		        // but for the life of me I can't work it out 
		        // after about 15 levels of indirection they end up not-properly wrapping slf4j anyway.
		        // ( see https://maven.apache.org/ref/3.6.0/maven-embedder/apidocs/org/apache/maven/cli/logging/Slf4jLoggerManager.html )
		        ConsoleLoggerManager clm = new ConsoleLoggerManager();
		        Logger logger = clm.getLoggerForComponent("YamlCombineMojo");
		        
		        DefaultMavenFileFilter dmff = new DefaultMavenFileFilter(buildContext);
		        // dmff.enableLogging(logger); // filter will NPE if this isn't set
		        getLog().info("logger is " + logger);
				try {
					List<FilterWrapper> filterWrappers = dmff.getDefaultFilterWrappers( mre );
					sc.setFilterWrappers(filterWrappers);
				} catch (MavenFilteringException e) {
					throw new IllegalStateException("Coult not get default filter wrappers", e);
				}
			}
			PrintWriter w = new PrintWriter(new OutputStreamWriter(fos, outputFileCharset));
			sc.combine(w,inputFileSetCharset);
		} catch (IOException ioe) {
			// trouble at the mill
			throw new MojoExecutionException("Could not create combined yaml file", ioe); 
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch(IOException ioe) {
					throw new IllegalStateException("IOException closing file", ioe);
				}
			}
		}
		return destFile;

	}
   
    
    public void execute()
        throws MojoExecutionException
    {
    	createCombinedYaml();
    }
    

    
    // getters / setters
    
    /**
     * {@inheritDoc}
     */
    protected Boolean getFiltering() {
    	return filtering;
    }
   

    /**
     * {@inheritDoc}
     */
    protected Boolean getVerbose() {
     	return verbose;
    }
    
    /**
     * @return the project
     */
    public MavenProject getProject() {
        return project;
    }

    /**
     * @param project
     * the project to set
     */
    public void setProject(final MavenProject project) {
        this.project = project;
    }

    /**
     * @return the settings
     */
    public Settings getSettings() {
        return settings;
    }

    /**
     * @param settings
     * the settings to set
     */
    public void setSettings(final Settings settings) {
        this.settings = settings;
    }
    
}
