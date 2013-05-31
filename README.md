AnnoStore
===========

This allows users to store annotations.

## Posting an annotation

- Post against the path /annostore
- The posted graph must contain exactly one resource of type http://www.w3.org/ns/oa#Annotation
- No particular permission is required to post an annotation but the 
current user will be added as a the value of a DC:creator property

The following posts a meaningless annotation (mea

    curl -i -X POST -H "Content-Type: text/turtle" \
     -H "Accept: application/rdf+xml" \
    --data \
    ' @prefix oa: <http://www.w3.org/ns/oa#> .
      @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
    [] a oa:Annotation ;
       rdfs:commend "an meaningless annotation" ;
       oa:hasTarget <http://example.org/foo> . ' http://localhost:8080/annostore

## Retrieving annotations

For retrieving annotations AnnoStore provides a service to dereference the
context of a resource

GET /Annostore with the IRI of the resource of which the context is requetsed 
as value of the iri query parameter.

Example:
    curl  -H "Accept: application/rdf+xml"  \
        http://localhost:8080/annostore?iri=http://example.org/foo .