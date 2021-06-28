# swagger-combine-maven-plugin

This project provides a source pre-processor which can be used to combine one or more YAML files before swagger code generation.

## Overview

This plugin exists mainly because `$ref` references are only allowed in one or two places in the swagger spec, and you may want to import objects in other locations.

## Syntax

This plugin recognises a new `$xref` reference which is backwards-compatible with `$ref`s, but can be located anywhere in the source swagger file. 
The pre-processor will replace `$xrefs` with the content of those references. 

`$xrefs` use the same JSON-Pointer syntax as swagger ( which performs URL-escaping and ~1 substitution ), so you could have references that look like

    paths:
      /thing:
        $ref: 'paths.yaml#/paths/~1thing
    
or

    paths:
      /thing:
        $ref: 'paths.yaml#/paths/%2Fthing
    
To make references more readable, you can add '#' characters inside the reference which toggles how '/' characters are interpretted ( either '/' as JSON-Pointer separators,
or '/' as characters within a YAML key ).

e.g.

    paths:
      /thing:
        $ref: 'paths.yaml#/paths/#/thing

## Modifying imported objects

Unlike `$ref`s, `$xref`s let you modify the imported object; an object defined via an $xref can provide additional keys which are merged in with the `$xref`ed object.

e.g.

    paths:
      /thing:
        get:
          $xref: 'paths.yaml#/paths/#/thing#/get'
          parameters:
            old-parameter:
              description: some old parameter whose definition has changed
            new-parameter: 
              description: the new parameter in v2 of the API that isn't in v1

## Combining entire input files

As an alternative or in addition to using `$xref` references, you can also merge entire YAML files together by supplying them as inputs to the plugin.
The YAML files will be merged together.

## Handling of $ref

Files can continue to use `$ref` references, and these references will survive the swagger-combine-maven-plugin process.


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

log4j-one is licensed under the BSD 2-clause license.


