# swagger-combine-maven-plugin

This project provides a source pre-processor which can be used to combine one or more YAML files before swagger code generation.

## Overview

This plugin exists mainly because `$ref` references are only allowed in one or two places in the [swagger spec](https://swagger.io/docs/specification/using-ref/), and you may want to import objects in other locations.

[The maven documentation for the plugin is here](https://randomnoun.github.io/swagger-combine-maven-plugin/)

And there's a [blog article here](http://www.randomnoun.com/wp/2021/06/29/swagger-combine/).

## Syntax

This plugin recognises a new `$xref` reference which is backwards-compatible with `$ref`s, but can be located anywhere in the source swagger file. 
The pre-processor will replace `$xrefs` with the content of those references. 

`$xrefs` use the same JSON-Pointer syntax as swagger ( which performs URL-escaping and ~1 substitution ), so you could have references that look like

    paths:
      /thing:
        $xref: 'paths.yaml#/paths/~1thing
    
or

    paths:
      /thing:
        $xref: 'paths.yaml#/paths/%2Fthing
    
To make references more readable, you can add '#' characters inside the reference. Each '#' character toggles how '/' characters are interpretted, either '/' as JSON-Pointer separators,
or '/' as characters within a YAML key.

e.g.

    paths:
      /thing:
        $xref: 'paths.yaml#/paths/#/thing

## Modifying imported objects

Unlike `$ref`s, `$xref`s let you modify the imported object. 

An object defined via an `$xref` can provide additional keys which are merged in with the `$xref`ed object.

e.g.

    paths:
      /thing:
        get:
          $xref: 'paths.yaml#/paths/#/thing#/get'
          parameters:
            old-parameter:
              description: some old parameter whose description has changed
            new-parameter: 
              description: a new parameter in v2 of the API that isn't in v1
              type: string

## Combining entire input files

As an alternative to using `$xref` references, you can also merge entire YAML files together by supplying multiple YAML files as inputs to the plugin.

## Handling of $ref

Files can continue to use `$ref` references, and these references will survive the swagger-combine-maven-plugin process.

## Examples

Here's you how might use this plugin in your pom.xml file:

    <project>
      <build>
        <plugins>
    
          <plugin>
            <groupId>com.randomnoun.maven.plugins</groupId>
            <artifactId>swagger-combine-maven-plugin</artifactId>
            <version>1.0.3</version>
            <executions>
              <execution>
                <id>swagger-combine</id>
                <phase>generate-sources</phase>
                <goals>
                    <goal>swagger-combine</goal>
                </goals>
                <configuration>
                  <fileset>
                    <includes>
                      <!-- can supply multiple files here or filespecs; e.g. *.yaml -->
                      <include>my-swagger-file-with-xrefs-in-it.yaml</include>
                    </includes>
                    <directory>${project.basedir}/src/main/swagger</directory>
                  </fileset>
                  <outputDirectory>${project.basedir}/target/swagger-combine</outputDirectory>
                  <!-- use this file as the input to the codegen goal -->
                  <finalName>my-swagger-file-with-resolved-xrefs.yaml</finalName>
                </configuration>
              </execution>
            </executions>
          </plugin>
                
        <plugins>
      <build>
    <project>
            

## Is there a blog article about this project ?

Why yes there is: http://www.randomnoun.com/wp/2021/06/29/swagger-combine/

## Alternatives

Did a bit of googling and found:

* https://swagger.io/docs/specification/using-ref/ - swagger $ref documentation
* https://datatracker.ietf.org/doc/html/rfc6901 - draft JSON pointer spec
* https://davidgarcia.dev/posts/how-to-split-open-api-spec-into-multiple-files/ ( openAPI $ref examples, entire yaml docs only )
* https://stackoverflow.com/questions/54586137/how-do-i-combine-multiple-openapi-3-specification-files-together ( using swagger codegen to generate combined yaml file )
* https://github.com/swagger-api/swagger-codegen/issues/3614 - file $ref issue
* https://github.com/deveshpujari/swagger-multi-file-yaml ( another plugin to merge yaml , full files only, looks broken )
* https://github.com/ngeor/kamino/tree/trunk/java/yak4j-swagger-maven-plugin ( another one , [has been deleted](https://github.com/ngeor/kamino/search?q=yak4j-swagger-maven-plugin&type=commits) )
* https://github.com/squark-io/swagger-combine ( another one, only merges top-level swagger objects )
* https://kislyuk.github.io/yq/ - python jq-like processor for yaml, python 2 only though

but these didn't really work for me.

## Licensing

swagger-combine-maven-plugin is licensed under the BSD 2-clause license.


