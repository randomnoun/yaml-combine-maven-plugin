package com.randomnoun.maven.plugin.yamlCombine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.utils.io.FileUtils.FilterWrapper;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

public class SwaggerCombiner {

	public List<FilterWrapper> getFilterWrappers() {
		return filterWrappers;
	}

	public void setFilterWrappers(List<FilterWrapper> filterWrappers) {
		this.filterWrappers = filterWrappers;
	}

	File relativeDir;
	String[] files;
	boolean verbose;
	Log log;
	List<FilterWrapper> filterWrappers;

	private Map<String, Map<String, Object>> yamlFiles = new HashMap<String, Map<String, Object>>();

	@SuppressWarnings("unchecked")
	public void combine(Writer w) throws IOException {

		List<String> fileList = new ArrayList<String>(Arrays.asList(files));
		Collections.sort(fileList);
		
		Yaml yaml = new Yaml();
		@SuppressWarnings("rawtypes")
		Map mergedObj = null;
		for (String f : fileList) {
			InputStream inputStream = new FileInputStream(new File(relativeDir, f));
			Reader reader = new InputStreamReader(inputStream);
			if (filterWrappers != null) {
				for ( FilterWrapper wrapper : filterWrappers ) {
                    reader = wrapper.getReader( reader );
                }
			}
			
			@SuppressWarnings("rawtypes")
			Map obj = yaml.load(reader);
			if (mergedObj == null) {
				mergedObj = obj;
			} else {
				merge(mergedObj, obj, f, "");
			}
		}
		// going to use $xref instead of $ref since my ref syntax is a bit different
		// and to make it more obvious that this isn't a valid swagger file

		// this is definitely a constructive use of my weekend
		replaceRefs((Map<Object, Object>) mergedObj, relativeDir, "");

		final ObjectMapper mapper = new ObjectMapper(
			new YAMLFactory().configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
				.configure(YAMLGenerator.Feature.SPLIT_LINES, false)
				.configure(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS, true));
		configureMapper(mapper);
		mapper.writeValue(w, mergedObj);
		w.flush();
	}

	@SuppressWarnings({ "unchecked" })
	private void merge(Map<Object, Object> mergedObj, Map<Object, Object> obj, String f, String prefix)
			throws IllegalArgumentException {
		Set<Object> cloneSet = new LinkedHashSet<Object>(obj.keySet());
		for (Object k : cloneSet) {
			Object v = obj.get(k);
			Object mv = mergedObj.get(k);

			if (mv == null) {
				// simple merge
				mergedObj.put(k, v);
			} else if (v == null) {
				// nothing to merge
			} else if (mv instanceof Map && v instanceof Map) {
				// the only things we can merge are maps
				merge((Map<Object, Object>) mv, (Map<Object, Object>) v, f, prefix + String.valueOf(k) + "/");
			} else if (mv.getClass().equals(v.getClass())) {
				// replace if the types are the same
				mergedObj.put(k, v);
			} else {
				throw new IllegalArgumentException("Could not merge " + f + "#" + prefix + String.valueOf(k) + 
					" (" + v.getClass().getName() + ") into merged object " + mv.getClass().getName());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Object replaceRefs(Map<Object, Object> obj, File relativeDir, String spacePrefix)
			throws IllegalArgumentException, IOException {
		// process $xref before other keys
		Object result = null;
		if (obj.containsKey("$xref")) {
			Object xref = obj.get("$xref");
			if (verbose) {
				getLog().info(spacePrefix + "$xref to " + xref);
			}
			result = getXref(relativeDir, (String) xref);
			if (result instanceof Map) {
				// shallow clone, but replaceRefs will perform shallow clones at deeper levels
				result = new HashMap<Object, Object>((Map<Object, Object>) result);
				result = replaceRefs((Map<Object, Object>) result, relativeDir, spacePrefix + "  ");
			}

			// override values in xref object
			for (Object k : obj.keySet()) {
				Object v = obj.get(k);
				if (k.equals("$xref")) {
					// ignore
				} else {
					if (verbose) {
						getLog().info(spacePrefix + String.valueOf(k));
					}
					if (result instanceof Map) {
						Map<Object, Object> r = (Map<Object, Object>) result;
						if (r.get(k) == null) {
							// add new property to xref'ed Map
							r.put(k, v);
						} else if (r.get(k) instanceof Map && v instanceof Map) {
							// the xref'ed object is a Map containing a Map
							// don't think this is ever going to happen. but maybe it will
							Map<Object, Object> rv = (Map<Object, Object>) r.get(k);
							merge(rv, (Map<Object, Object>) v, "", String.valueOf(k) + "/");
						} else {
							// replace existing key/value pair
							r.put(k, v);
						}
					} else {
						throw new IllegalArgumentException(
							"Could not override " + String.valueOf(k) + " (" + v.getClass().getName() +
							") from xref '" + xref + "' " + result.getClass().getName());
					}
				}
			}

		} else {
			// descend into obj
			// clone to prevent ConcurrentModificationException
			Set<Object> cloneSet = new LinkedHashSet<Object>(obj.keySet());
			for (Object k : cloneSet) {
				Object v = obj.get(k);
				if (k.equals("$xref")) {
					throw new IllegalStateException("$xref found in Map that didn't contain $xref");
				} else if (verbose) {
					getLog().info(spacePrefix + String.valueOf(k));
				}
				if (v instanceof Map) {
					// shallow clone, but replaceRefs will perform shallow clones at deeper levels
					Map<Object, Object> clone = new HashMap<Object, Object>((Map<Object, Object>) v);
					Object newObject = replaceRefs(clone, relativeDir, spacePrefix + "  ");
					obj.put(k, newObject);
				}
			}
			result = obj;
		}

		return result;
	}

	private Object getXref(File relativeDir, String ref) throws IllegalArgumentException, IOException {
		// myproject-v1-object.yaml#/definitions/InvalidResponse       existing
		// myproject-v1-swagger-api.yaml#/paths/~1authenticate         the JSON-Pointer way
		// myproject-v1-swagger-api.yaml#/paths/%2Fauthenticate        the url escape way
		// myproject-v1-swagger-api.yaml#/paths/#/authenticate         the randomnoun way.

		// can't have '#' as the start of a key as that's a comment. but you probably can. somehow.
		int pos = ref.indexOf('#');
		if (pos == -1) {
			// local refs still start with a '#'
			// $ref: '#/paths/~1blogs~1{blog_id}~1new~0posts'
			throw new IllegalArgumentException("Unparseable $xref '" + ref + "'");
		} else {
			String f = ref.substring(0, pos);
			String p = ref.substring(pos + 1);
			// any remaining '#' chars switch between ~1-style escaping and no escaping
			// see ResolverCache or the json-pointer escaping rules in swagger
			final StringBuilder result = new StringBuilder();
			p.chars().forEachOrdered(new IntConsumer() {
				boolean asJsonPath = true;

				@Override
				public void accept(int ch) {
					if (ch == '#') {
						asJsonPath = !asJsonPath;
					} else if (asJsonPath) {
						result.append((char) ch);
					} else {
						// escape this jsonpath-ly
						if (ch == '/') {
							result.append("~1");
						} else if (ch == '~') {
							result.append("~0");
						} else {
							result.append((char) ch);
						}
					}
				}
			});
			ref = f + "#" + result.toString();

			// as per ResolverCache
			// although ResolverCache parses an entire json doc every time there's a ref,
			// even if we've already parsed it.
			final String[] refParts = ref.split("#/");
			final String file = refParts[0];
			final String definitionPath = refParts.length == 2 ? refParts[1] : null;
			Map<String, Object> contents = yamlFiles.get(file);
			if (contents == null) {
				Yaml yaml = new Yaml();
				File inputFile = new File(relativeDir, file);
				InputStream inputStream = new FileInputStream(inputFile);
				try {
					contents = yaml.load(inputStream);
				} catch (Exception e) {
					throw new IOException("Invalid YAML file '" + inputFile.getCanonicalPath() + "'", e);
				}
				inputStream.close();
				yamlFiles.put(file, contents);
			}
			if (verbose) {
				getLog().info("definitionPath '" + definitionPath + "' in '" + file + "'");
			}
			if (definitionPath == null) {
				return contents;
			} else {
				String[] jsonPathElements = definitionPath.split("/");
				Object node = contents;
				for (String jsonPathElement : jsonPathElements) {
					try {
						node = ((Map<?, ?>) node).get(unescapePointer(jsonPathElement));
						if (node == null) {
							throw new IllegalArgumentException(
									"Could not find " + definitionPath + " in contents of " + file);
						}
					} catch (ClassCastException cce) {
						throw new IllegalArgumentException("Could not descend into " + jsonPathElement + " of "
								+ definitionPath + " in contents of " + file);
					}
					// getLog().info("jsonPathElement is " + node);
				}
				return node;
			}
		}
	}

	// ResolverCache.unescapePointer()
	private String unescapePointer(String jsonPathElement) {
		// URL decode the fragment
		try {
			jsonPathElement = URLDecoder.decode(jsonPathElement, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			//
		}
		// Unescape the JSON Pointer segment using the algorithm described in RFC 6901,
		// section 4:
		// https://tools.ietf.org/html/rfc6901#section-4
		// First transform any occurrence of the sequence '~1' to '/'
		jsonPathElement = jsonPathElement.replaceAll("~1", "/");
		// Then transforming any occurrence of the sequence '~0' to '~'.
		return jsonPathElement.replaceAll("~0", "~");
	}

	// from io.swagger.codegen.languages.SwaggerYamlGenerator
	// but hopefully not requires as we're only dealing with structured lists/maps,
	// not Swagger objects
	private void configureMapper(ObjectMapper mapper) {
		/*
		 * Module deserializerModule = new DeserializationModule(true, true);
		 * mapper.registerModule(deserializerModule);
		 * mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		 * mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		 * mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		 * mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		 * mapper.addMixIn(Response.class, ResponseSchemaMixin.class);
		 * ReferenceSerializationConfigurer.serializeAsComputedRef(mapper);
		 */
	}

	public File getRelativeDir() {
		return relativeDir;
	}

	public void setRelativeDir(File relativeDir) {
		this.relativeDir = relativeDir;
	}

	public String[] getFiles() {
		return files;
	}

	public void setFiles(String[] files) {
		this.files = files;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public Log getLog() {
		return log;
	}

	public void setLog(Log log) {
		this.log = log;
	}


}
