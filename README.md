![CoConAT](https://raw.githubusercontent.com/mgoellnitz/coconat/master/template/coconat-small.png)

[![Latest Release](https://img.shields.io/github/release/mgoellnitz/coconat.java.svg)](https://github.com/mgoellnitz/coconat.java/releases/latest)
[![Build Status](https://travis-ci.org/mgoellnitz/coconat.java.svg?branch=master)](https://travis-ci.org/mgoellnitz/coconat.java)
[![Build Status](https://img.shields.io/gitlab/pipeline/coconat/coconat.java.svg)](https://gitlab.com/coconat/coconat.java/pipelines)
[![Coverage Status](https://coveralls.io/repos/github/mgoellnitz/coconat.java/badge.svg?branch=master)](https://coveralls.io/github/mgoellnitz/coconat.java?branch=master)
[![Coverage Status](http://codecov.io/github/mgoellnitz/coconat.java/coverage.svg?branch=master)](https://codecov.io/gh/mgoellnitz/coconat.java)

# CoConAT Java flavour

This is the Java flavour of the [CoConAT Content Access Tool](http://mgoellnitz.github.io/coconat/).
It is a small library to access the contents of a CoreMedia content repository 
through direct access of  a Replication Live Server database in a structured 
way.


## Ancestry

It is derived from the CoreMedia Adaptor CoMA of the [tangram project](https://github.com/mgoellnitz/tangram)
and will hopefully replace parts of it later. This ancestry also is the reason 
why you will find many method implemented which are not anticipated in the 
relating interfaces.


## Feedback

Please use the [issue reporting facility at github](https://github.com/mgoellnitz/coconat.java/issues) 
to get in touch.


## Usage example

The code contains a small ReadContent.java program which illustrates the usages 
and is meant for connection to a menusite MySQL database created with a stock 
CoreMedia 7 content management server which can be created from the project 
workspace or from the gradle demo for CoreMedia [server assembly](https://github.com/mgoellnitz/cm-cms-webapp)
and the [accompanying tools](https://github.com/mgoellnitz/cm-cms-tools).


## Test

It also comes with a small database using a transcribed [tangram example
application](https://github.com/mgoellnitz/tangram-examples) document model and
some content items which are used during the unit test. This renders the test an
integration test but removed the need to mock too many items. To ease this a 
small hsqldb was used which is not supported by recent CoreMedia versions. So 
the document model was translated to an old 5.0 server. Both versions of the 
document type model are included in this code repository.


## Building

The code includes the usual gradle based build. So a complete run will be

```
gradle clean build jacocoTestReport publishToMavenLocal
```


## Availability

Stable releases are available through the jcenter repository while snapshots are 
available at oss.jfrog.org.

```
https://oss.jfrog.org/artifactory/oss-snapshot-local/
```
