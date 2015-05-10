![CoConAT](http://coconat.divshot.io/coconat-small.png)

# CoConAT Java flavour

This is the Java flavour of the CoConAT Content Access Tool. It is a small library 
to access the contents of a CoreMedia content repository through direct access of 
a Replication Live Server database in a structured way.

## Ancestry

It is derived from the CoreMedia Adaptor CoMA of the [tangram project](https://github.com/mgoellnitz/tangram) 
and will hopefully replace parts of it later. This ancestry also is the reason why 
you will find many method implemented which are not anticipated in the relating interfaces.

## Usage example

The code contains a small ReadContent.java program which illustrates the usages and 
is meant for connection to a menusite MySQL database created with a stock CoreMedia 7 
content management server which can be created from the project workspace or from 
the gradle demo for CoreMedia [server assembly](https://github.com/mgoellnitz/cm-cms-webapp) 
and the [accompanying tools](https://github.com/mgoellnitz/cm-cms-tools).

## Test

It also comes with a small database using a transcribed [tangram example 
application](https://github.com/mgoellnitz/tangram-examples) document model and 
some content items which are used during the unit test. This renders the test an 
integration test but removed the need to mock too many items. To ease this a small 
hsqldb was used which is not supported by recent CoreMedia versions. So the document 
model was translated to an old 5.0 server. Both versions of the document type model 
are included in this code repository.
